-- ============================================
-- AJOUT COLONNE DATE_PUBLICATION POUR BADGE "NOUVEAU"
-- ============================================

-- Ajouter la colonne date_publication à la table offreemlpoi
ALTER TABLE offreemlpoi 
ADD COLUMN IF NOT EXISTS date_publication DATE;

-- Mettre à jour les offres existantes avec une date par défaut (date d'aujourd'hui - random)
-- Pour les tests, on met des dates variées
UPDATE offreemlpoi 
SET date_publication = CURDATE() - INTERVAL (id % 10) DAY
WHERE date_publication IS NULL;

-- Vérification
SELECT 
    id, 
    titre, 
    date_publication,
    CASE 
        WHEN date_publication >= CURDATE() - INTERVAL 7 DAY THEN '🏷️ NOUVEAU'
        ELSE 'Ancien'
    END as badge
FROM offreemlpoi
ORDER BY date_publication DESC;
