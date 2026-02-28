# ML - Prédiction de complexité (scikit-learn -> ONNX -> Java)

## 1) Entraîner le modèle (Python)

Le script d'entraînement est dans `ml/train_complexity_model.py`.
Il utilise désormais la **Logique B (Temps d'apprentissage estimé)** :
- **20 min/PDF**
- **900 chars/min pour le texte**
- **Seuils S1** : `< 30 min` (Facile), `30-90 min` (Moyen), `> 90 min` (Complexe)
- **Indépendant** de la complexité déclarée par l'utilisateur.

Dans le dossier `ml/`:

```bash
py -m pip install -r requirements.txt
```

### Connexion DB

Le script utilise ces variables d'environnement (avec des valeurs par défaut identiques au projet Java):

- `DB_URL` (default: `jdbc:mysql://localhost:3306/careerlink`)
- `DB_USER` (default: `root`)
- `DB_PASSWORD` (default: empty)

Exemple PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/careerlink"
$env:DB_USER="root"
$env:DB_PASSWORD=""
```

### Lancer l'entraînement

Depuis le dossier `ml/`:

```bash
py train_complexity_model.py
```

Cela exporte automatiquement dans:

- `src/main/resources/ml/complexity_model.onnx`
- `src/main/resources/ml/feature_schema.json`

## 2) Utilisation dans l'application Java

L'application charge automatiquement le modèle au runtime via `Services.Formation.MlComplexityPredictor`.

- Si le modèle existe: `PredictiveCourseAnalyzer` utilise le ML pour `getComplexityPrediction`.
- Si le modèle n'existe pas (ou erreur): fallback automatique sur l'heuristique.

## 3) Note sur les labels

Dans cette version, le label (`Facile/Moyen/Complexe`) est dérivé de `Cour.complexite` selon la règle:

- `< 2.5` -> Facile
- `== 2.5` -> Moyen
- `> 2.5` -> Complexe
