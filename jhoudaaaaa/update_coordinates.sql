-- ============================================
-- MISE À JOUR DES COORDONNÉES GPS POUR LES ANCIENNES LOCALISATIONS
-- ============================================
-- Ce script ajoute les coordonnées GPS aux villes tunisiennes manquantes
-- Exécutez-le dans phpMyAdmin ou MySQL Workbench

-- Bizerte (ID 5)
UPDATE localisation 
SET latitude = 37.2746, longitude = 9.8739 
WHERE id_localisation = 5 AND (latitude IS NULL OR longitude IS NULL);

-- Gabès (ID 6)
UPDATE localisation 
SET latitude = 33.8815, longitude = 10.0982 
WHERE id_localisation = 6 AND (latitude IS NULL OR longitude IS NULL);

-- Ariana (ID 7) - banlieue nord de Tunis
UPDATE localisation 
SET latitude = 36.8625, longitude = 10.1956 
WHERE id_localisation = 7 AND (latitude IS NULL OR longitude IS NULL);

-- Monastir (ID 8)
UPDATE localisation 
SET latitude = 35.7775, longitude = 10.8262 
WHERE id_localisation = 8 AND (latitude IS NULL OR longitude IS NULL);

-- Nabeul (ID 9)
UPDATE localisation 
SET latitude = 36.4561, longitude = 10.7376 
WHERE id_localisation = 9 AND (latitude IS NULL OR longitude IS NULL);

-- Mahdia (ID 10)
UPDATE localisation 
SET latitude = 35.5047, longitude = 11.0622 
WHERE id_localisation = 10 AND (latitude IS NULL OR longitude IS NULL);

-- Remote (ID 11) - Télétravail (position fictive au centre de la Tunisie)
UPDATE localisation 
SET latitude = 36.8065, longitude = 10.1815 
WHERE id_localisation = 11 AND (latitude IS NULL OR longitude IS NULL);

-- France (ID 12) - Position fictive à Paris
UPDATE localisation 
SET latitude = 48.8566, longitude = 2.3522 
WHERE id_localisation = 12 AND (latitude IS NULL OR longitude IS NULL);

-- ============================================
-- VÉRIFICATION
-- ============================================
SELECT 
    id_localisation,
    ville,
    pays,
    latitude,
    longitude,
    CASE 
        WHEN latitude IS NOT NULL AND longitude IS NOT NULL THEN '✅ Avec coordonnées'
        ELSE '❌ Sans coordonnées'
    END as statut
FROM localisation
ORDER BY id_localisation;

-- ============================================
-- COMPTEUR FINAL
-- ============================================
SELECT 
    COUNT(*) as total_localisations,
    SUM(CASE WHEN latitude IS NOT NULL AND longitude IS NOT NULL THEN 1 ELSE 0 END) as avec_gps,
    SUM(CASE WHEN latitude IS NULL OR longitude IS NULL THEN 1 ELSE 0 END) as sans_gps
FROM localisation;
Abonnement Entreprise Premium	Entreprise paye pour mise en avant de ses offres	Abonnement (id, idEntreprise, type, dateDebut, dateFin, montant)