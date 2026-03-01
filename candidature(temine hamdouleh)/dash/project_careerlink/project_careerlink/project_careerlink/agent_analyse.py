#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import json
import os
import sys

import mysql.connector
import requests

API_URL = "https://openrouter.ai/api/v1/chat/completions"
MODEL = os.getenv("OPENROUTER_MODEL", "openai/o3")

OFFRE = {
    "id_offre": 1,
    "titre": "Développeur Java Spring Boot Full Stack",
    "entreprise": "TechCorp Algérie",
    "localisation": "Alger, Algérie",
    "type_contrat": "CDI",
    "annees_experience_requis": 2,
    "niveau_etudes_requis": "Bac+3 minimum (Licence ou Master en Informatique)",
    "competences_requises": ["Java", "Spring Boot", "SQL", "MySQL", "Docker", "REST API", "Git"],
    "competences_souhaitees": ["AWS", "Kubernetes", "Microservices", "Jenkins", "React"],
    "certifications_souhaitees": ["AWS Certified Developer"],
    "soft_skills_requis": ["Communication", "Travail en équipe", "Autonomie", "Leadership"],
    "description": (
        "Nous recherchons un développeur Java Spring Boot expérimenté pour rejoindre "
        "notre équipe produit. Vous serez responsable du développement et de la maintenance "
        "d'applications web full stack, de la conception d'APIs REST performantes, et de la "
        "participation aux décisions d'architecture technique. Vous travaillerez en équipe "
        "Agile/Scrum avec des déploiements sur AWS. Une expérience en architecture "
        "microservices est un plus apprécié."
    ),
}

SYSTEM_PROMPT = """
Tu es un expert RH et recruteur technique senior.
Analyse la candidature par rapport à l'offre d'emploi fournie.
Retourne UNIQUEMENT un objet JSON valide, sans texte avant ni après,
sans balises markdown, sans commentaires.

Structure JSON OBLIGATOIRE :
{
  "score_global": <int 0-100>,
  "compatibilite": <"Élevée" | "Moyenne" | "Faible">,
  "recommandation": <"Entretien fortement conseillé" | "Entretien possible" | "Non recommandé">,
  "classement": <"Top 3" | "Top 10" | "Standard">,
  "classement_detail": <string>,

  "competences_techniques": {
    "score": <int 0-100>,
    "taux_couverture": <int 0-100>,
    "commentaire": <string>,
    "items": [
      { "nom": <string>, "present": <bool>, "niveau": <"Confirmé"|"Intermédiaire"|"Débutant"|"Absent"> }
    ]
  },

  "experience": {
    "score": <int 0-100>,
    "annees_candidat": <int>,
    "annees_requis": <int>,
    "type_experience": <string>,
    "commentaire": <string>
  },

  "diplome_certifications": {
    "score": <int 0-100>,
    "diplome": <string>,
    "niveau_requis": <string>,
    "diplome_valide": <bool>,
    "commentaire": <string>,
    "certifications": [
      { "nom": <string>, "presente": <bool> }
    ]
  },

  "soft_skills": {
    "score": <int 0-100>,
    "items": [<string>],
    "commentaire": <string>
  },

  "points_forts": [
    { "icone": <"technique"|"experience"|"soft"|"diplome">, "mot_cle": <string>, "texte": <string> }
  ],

  "points_ameliorer": [
    { "icone": <"warning"|"error">, "texte": <string> }
  ],

  "analyse_globale": <string>
}
""".strip()


def db_config() -> dict:
    return {
        "host": os.getenv("DB_HOST", "localhost"),
        "port": int(os.getenv("DB_PORT", "3306")),
        "database": os.getenv("DB_NAME", "ta_base"),
        "user": os.getenv("DB_USER", "root"),
        "password": os.getenv("DB_PASSWORD", ""),
        "charset": "utf8mb4",
    }


def get_candidat(id_candidat: int) -> dict:
    conn = mysql.connector.connect(**db_config())
    cursor = conn.cursor(dictionary=True)
    cursor.execute(
        """
        SELECT IDCandidat, prenom, nom, email, indicatif, tel,
               adresse, ville, date_naissance, highest_degree,
               institution, lettre_motivation, skills_text,
               Pieces_jointes, video_path, decision_rh, user_id
        FROM candidature
        WHERE IDCandidat = %s
        """,
        (id_candidat,),
    )
    row = cursor.fetchone()
    cursor.close()
    conn.close()
    if not row:
        raise ValueError(f"Aucun candidat IDCandidat={id_candidat}")
    if row.get("date_naissance"):
        row["date_naissance"] = str(row["date_naissance"])
    return row


def build_user_prompt(candidat: dict, offre: dict) -> str:
    return f"""
=== CANDIDATURE ===
Nom complet   : {candidat.get('prenom','')} {candidat.get('nom','')}
Email         : {candidat.get('email','')}
Ville         : {candidat.get('ville','')}
Diplôme       : {candidat.get('highest_degree','Non précisé')}
Institution   : {candidat.get('institution','Non précisée')}
Compétences   : {candidat.get('skills_text','Non précisées')}
Lettre motiv. : {candidat.get('lettre_motivation','Non fournie')}

=== OFFRE D'EMPLOI ===
Titre         : {offre['titre']}
Entreprise    : {offre['entreprise']}
Contrat       : {offre['type_contrat']}
Expérience    : {offre['annees_experience_requis']} ans minimum
Études requis : {offre['niveau_etudes_requis']}
Compétences   : {', '.join(offre['competences_requises'])}
Optionnelles  : {', '.join(offre['competences_souhaitees'])}
Certifications: {', '.join(offre['certifications_souhaitees'])}
Soft skills   : {', '.join(offre['soft_skills_requis'])}
Description   : {offre['description']}

Analyse et retourne le JSON.
""".strip()


def call_agent(candidat: dict, offre: dict) -> dict:
    api_key = os.getenv("OPENROUTER_API_KEY", "").strip()
    if not api_key:
        raise RuntimeError("OPENROUTER_API_KEY manquante (variable d'environnement)")

    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json",
    }

    payload = {
        "model": MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": build_user_prompt(candidat, offre)},
        ],
        "reasoning": {"enabled": True},
    }

    print("[Agent] Appel OpenRouter...", file=sys.stderr)
    resp = requests.post(API_URL, headers=headers, data=json.dumps(payload, ensure_ascii=False), timeout=180)
    resp.raise_for_status()

    content = (resp.json()["choices"][0]["message"].get("content") or "").strip()

    if content.startswith("```"):
        lines = content.splitlines()
        content = "\n".join(lines[1:-1] if lines and lines[-1].strip() == "```" else lines[1:])

    print("[Agent] Parsing JSON...", file=sys.stderr)
    try:
        return json.loads(content)
    except json.JSONDecodeError as e:
        return {"erreur": str(e), "contenu_brut": content}


def main() -> None:
    if len(sys.argv) < 2:
        print(json.dumps({"erreur": "Usage: agent_analyse.py <IDCandidat>"}, ensure_ascii=False))
        sys.exit(1)

    id_candidat = int(sys.argv[1])

    print(f"[Agent] Lecture candidat {id_candidat}...", file=sys.stderr)
    candidat = get_candidat(id_candidat)

    result = call_agent(candidat, OFFRE)

    result["_nom"] = candidat.get("nom", "")
    result["_prenom"] = candidat.get("prenom", "")
    result["_offre_titre"] = OFFRE["titre"]

    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
