"""
Create a minimal ONNX model manually (no skl2onnx) that works with ONNX Runtime Java.
Uses only basic operators: MatMul, Add, Softmax - no TreeEnsembleClassifier.
"""

import os
import json
import numpy as np
import onnx
from onnx import helper, TensorProto, numpy_helper

# Features and classes
FEATURES = [
    "nb_chapters",
    "pdf_count",
    "text_count",
    "total_text_len",
    "avg_text_len",
    "estimated_text_minutes",
    "estimated_total_minutes",
]

CLASSES = ["Facile", "Moyen", "Complexe"]

# Model parameters (simple linear classifier)
# Weights: 3 classes x 7 features
# These weights encode a simple heuristic:
# - Class 0 (Facile): low values -> high score
# - Class 1 (Moyen): medium values -> high score  
# - Class 2 (Complexe): high values -> high score

# Weights tuned to make PDFs drive complexity strongly.
# (classes x features) where feature order is FEATURES.
weights = np.array([
    # Facile: prefer small values
    [-0.20, -1.20, -0.10, -0.00010, -0.00060, -0.05, -0.06],
    # Moyen: mild preference for medium values (kept near 0)
    [0.00, 0.10, 0.00, 0.00000, 0.00000, 0.01, 0.01],
    # Complexe: prefer large values (PDF count is the main driver)
    [0.20, 1.20, 0.10, 0.00010, 0.00060, 0.05, 0.06],
], dtype=np.float32)

# Biases: keep neutral to avoid systematic "Facile".
biases = np.array([0.0, 0.1, 0.0], dtype=np.float32)

# Create ONNX model
input_name = "input"

# Input: batch x 7 features
input_tensor = helper.make_tensor_value_info(input_name, TensorProto.FLOAT, [None, 7])

# Output: probabilities (batch x 3)
output_prob = helper.make_tensor_value_info("probabilities", TensorProto.FLOAT, [None, 3])

# Output: predicted class (batch,)
output_class = helper.make_tensor_value_info("predicted_class", TensorProto.INT64, [None])

# Weights initializer
weights_init = numpy_helper.from_array(weights, name="weights")
biases_init = numpy_helper.from_array(biases, name="biases")

# MatMul: input @ weights.T -> (batch, 3)
matmul_node = helper.make_node("MatMul", [input_name, "weights"], ["matmul_output"])

# Add bias
add_node = helper.make_node("Add", ["matmul_output", "biases"], ["logits"])

# Softmax for probabilities
softmax_node = helper.make_node("Softmax", ["logits"], ["probabilities"], axis=1)

# ArgMax for predicted class
argmax_node = helper.make_node("ArgMax", ["probabilities"], ["predicted_class"], axis=1, keepdims=0)

# Create graph
graph = helper.make_graph(
    [matmul_node, add_node, softmax_node, argmax_node],
    "ComplexityClassifier",
    [input_tensor],
    [output_prob, output_class],
    [weights_init, biases_init]
)

# Create model
model = helper.make_model(graph, opset_imports=[helper.make_opsetid("", 13)])
model.ir_version = 7

# Validate
onnx.checker.check_model(model)

# Output directory
out_dir = os.getenv("OUT_DIR", os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "ml"))
out_dir = os.path.abspath(out_dir)
os.makedirs(out_dir, exist_ok=True)

# Save ONNX model
onnx_path = os.path.join(out_dir, "complexity_model.onnx")
onnx.save(model, onnx_path)

# Save schema
schema = {
    "input_name": input_name,
    "features": FEATURES,
    "classes": CLASSES,
    "logic": "Linear classifier (manual ONNX)",
    "params": {
        "min_per_pdf": 20,
        "chars_per_min": 900,
        "thresholds": {"easy_max": 30, "medium_max": 90}
    }
}
schema_path = os.path.join(out_dir, "feature_schema.json")
with open(schema_path, "w", encoding="utf-8") as f:
    json.dump(schema, f, ensure_ascii=False, indent=2)

print("Created minimal ONNX model (no TreeEnsembleClassifier):")
print(f" - ONNX: {onnx_path}")
print(f" - Schema: {schema_path}")
print(f" - Operators: MatMul, Add, Softmax, ArgMax (all safe for Java ONNX Runtime)")
