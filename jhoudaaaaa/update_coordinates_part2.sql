-- ============================================
-- MISE À JOUR COMPLÉMENTAIRE DES COORDONNÉES GPS
-- ============================================
-- Ce script ajoute les coordonnées pour les villes manquantes

-- Tunis (ID 1)
UPDATE localisation 
SET latitude = 36.8065, longitude = 10.1815 
WHERE id_localisation = 1 AND (latitude IS NULL OR longitude IS NULL);

-- Sfax (ID 2)
UPDATE localisation 
SET latitude = 34.7398, longitude = 10.7600 
WHERE id_localisation = 2 AND (latitude IS NULL OR longitude IS NULL);

-- Sousse (ID 3)
UPDATE localisation 
SET latitude = 35.8254, longitude = 10.6369 
WHERE id_localisation = 3 AND (latitude IS NULL OR longitude IS NULL);

-- Kairouan (ID 4)
UPDATE localisation 
SET latitude = 35.6781, longitude = 10.0963 
WHERE id_localisation = 4 AND (latitude IS NULL OR longitude IS NULL);

-- Vérification après mise à jour
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
