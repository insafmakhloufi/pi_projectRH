import json
import os
import re
from collections import defaultdict

import mysql.connector
import numpy as np
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from skl2onnx import to_onnx
from skl2onnx.common.data_types import FloatTensorType


# Features basées uniquement sur le contenu (plus de 'declared_complexity')
FEATURES = [
    "nb_chapters",
    "pdf_count",
    "text_count",
    "total_text_len",
    "avg_text_len",
    "estimated_text_minutes",
    "estimated_total_minutes",
]

# Paramètres de la logique "B" (Temps d'apprentissage estimé)
MINUTES_PER_PDF = 20
CHARS_PER_MINUTE = 900

# Seuils S1
THRESHOLD_EASY_MAX = 30  # < 30 min -> Facile
THRESHOLD_MEDIUM_MAX = 90 # 30..90 min -> Moyen, > 90 min -> Complexe


def parse_jdbc_mysql_url(jdbc_url: str):
    m = re.match(r"^jdbc:mysql://([^:/]+)(?::(\d+))?/(\w+)", jdbc_url.strip())
    if not m:
        raise ValueError(f"Unsupported JDBC url: {jdbc_url}")
    host = m.group(1)
    port = int(m.group(2) or 3306)
    database = m.group(3)
    return host, port, database


def get_db_config():
    jdbc_url = os.getenv("DB_URL", "jdbc:mysql://localhost:3306/careerlink")
    user = os.getenv("DB_USER", "root")
    password = os.getenv("DB_PASSWORD", "")
    host, port, database = parse_jdbc_mysql_url(jdbc_url)
    return {"host": host, "port": port, "database": database, "user": user, "password": password}


def label_from_estimated_time(total_minutes: float):
    if total_minutes < THRESHOLD_EASY_MAX:
        return "Facile"
    if total_minutes <= THRESHOLD_MEDIUM_MAX:
        return "Moyen"
    return "Complexe"


def load_dataset(conn):
    cur = conn.cursor(dictionary=True)

    cur.execute("SELECT id, `complexité` AS complexite, `duré` AS duree, nb_chapitres FROM Cour")
    courses = cur.fetchall()

    cur.execute("SELECT cour_id, contenu FROM Chapitre")
    chapters = cur.fetchall()

    chapters_by_course = defaultdict(list)
    for ch in chapters:
        chapters_by_course[int(ch["cour_id"])].append(ch.get("contenu") or "")

    X = []
    y = []

    for c in courses:
        course_id = int(c["id"])
        
        contenus = chapters_by_course.get(course_id, [])
        nb_chapters = int(c.get("nb_chapitres") or len(contenus) or 0)

        pdf_count = 0
        text_count = 0
        total_text_len = 0
        for contenu in contenus:
            v = (contenu or "").strip().lower()
            if v.startswith("pdf:") or v.startswith("pdfs:"):
                pdf_count += 1
            else:
                l = len((contenu or "").strip())
                if l > 0:
                    text_count += 1
                    total_text_len += l

        avg_text_len = (total_text_len / text_count) if text_count > 0 else 0.0
        
        # Logique B : Estimation du temps
        estimated_text_minutes = total_text_len / CHARS_PER_MINUTE
        estimated_pdf_minutes = pdf_count * MINUTES_PER_PDF
        estimated_total_minutes = estimated_text_minutes + estimated_pdf_minutes

        feats = {
            "nb_chapters": float(nb_chapters),
            "pdf_count": float(pdf_count),
            "text_count": float(text_count),
            "total_text_len": float(total_text_len),
            "avg_text_len": float(avg_text_len),
            "estimated_text_minutes": float(estimated_text_minutes),
            "estimated_total_minutes": float(estimated_total_minutes),
        }

        X.append([float(feats[f]) for f in FEATURES])
        y.append(label_from_estimated_time(estimated_total_minutes))

    return np.asarray(X, dtype=np.float32), np.asarray(y)


def main():
    cfg = get_db_config()
    conn = mysql.connector.connect(**cfg)
    try:
        X, y = load_dataset(conn)
    finally:
        conn.close()

    print(f"Dataset: {X.shape[0]} courses")
    unique_classes = np.unique(y)
    print(f"Classes found: {unique_classes.tolist()}")

    if X.shape[0] < 1:
        print("ERROR: No courses found. Cannot train.")
        return

    # Handle single-class case: create a dummy model that always predicts that class
    if len(unique_classes) == 1:
        print(f"WARNING: Only one class found ('{unique_classes[0]}'). Creating constant classifier.")
        out_dir = os.getenv("OUT_DIR", os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "ml"))
        out_dir = os.path.abspath(out_dir)
        os.makedirs(out_dir, exist_ok=True)

        # Export a simple schema indicating constant prediction
        schema = {
            "input_name": "input",
            "features": FEATURES,
            "classes": [str(unique_classes[0])],
            "logic": "Time-based (B) - Constant",
            "constant_prediction": str(unique_classes[0]),
            "params": {
                "min_per_pdf": MINUTES_PER_PDF,
                "chars_per_min": CHARS_PER_MINUTE,
                "thresholds": {"easy_max": THRESHOLD_EASY_MAX, "medium_max": THRESHOLD_MEDIUM_MAX}
            }
        }
        schema_path = os.path.join(out_dir, "feature_schema.json")
        with open(schema_path, "w", encoding="utf-8") as f:
            json.dump(schema, f, ensure_ascii=False, indent=2)

        # For ONNX, we create a minimal model (LinearRegression) that outputs the class index
        # This is a workaround - Java will detect it's constant from the schema
        from sklearn.dummy import DummyClassifier
        model = DummyClassifier(strategy="most_frequent")
        model.fit(X, y)
        initial_type = [("input", FloatTensorType([None, X.shape[1]]))]
        onnx_model = to_onnx(model, initial_types=initial_type, target_opset=15)

        onnx_path = os.path.join(out_dir, "complexity_model.onnx")
        with open(onnx_path, "wb") as f:
            f.write(onnx_model.SerializeToString())

        print("Exported Constant Classifier (single class dataset):")
        print(f" - ONNX: {onnx_path}")
        print(f" - Schema: {schema_path}")
        print(f" - Will always predict: {unique_classes[0]}")
        return

    model = Pipeline([
        ("scaler", StandardScaler()),
        ("clf", LogisticRegression(
            max_iter=2000,
            class_weight="balanced",
        )),
    ])
    model.fit(X, y)

    initial_type = [("input", FloatTensorType([None, X.shape[1]]))]
    onnx_model = to_onnx(model, initial_types=initial_type, target_opset=15)

    out_dir = os.getenv("OUT_DIR", os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "ml"))
    out_dir = os.path.abspath(out_dir)
    os.makedirs(out_dir, exist_ok=True)

    onnx_path = os.path.join(out_dir, "complexity_model.onnx")
    with open(onnx_path, "wb") as f:
        f.write(onnx_model.SerializeToString())

    classes = None
    try:
        classes = model.named_steps["clf"].classes_.tolist()
    except Exception:
        classes = []

    schema = {
        "input_name": "input",
        "features": FEATURES,
        "classes": [str(c) for c in classes],
        "logic": "Time-based (B)",
        "params": {
            "min_per_pdf": MINUTES_PER_PDF,
            "chars_per_min": CHARS_PER_MINUTE,
            "thresholds": {"easy_max": THRESHOLD_EASY_MAX, "medium_max": THRESHOLD_MEDIUM_MAX}
        }
    }
    schema_path = os.path.join(out_dir, "feature_schema.json")
    with open(schema_path, "w", encoding="utf-8") as f:
        json.dump(schema, f, ensure_ascii=False, indent=2)

    print("Exported Real Complexity Model (Time-based B/S1):")
    print(f" - ONNX: {onnx_path}")
    print(f" - Schema: {schema_path}")


if __name__ == "__main__":
    main()
