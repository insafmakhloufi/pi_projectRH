-- Vérifier combien d'offres ont des coordonnées GPS valides
SELECT 
    COUNT(*) as total_offres,
    SUM(CASE WHEN l.latitude IS NOT NULL AND l.longitude IS NOT NULL 
             AND l.latitude != 0 AND l.longitude != 0 
             THEN 1 ELSE 0 END) as avec_coordonnees,
    SUM(CASE WHEN l.latitude IS NULL OR l.longitude IS NULL 
             OR l.latitude = 0 OR l.longitude = 0 
             THEN 1 ELSE 0 END) as sans_coordonnees
FROM offreemlpoi o
LEFT JOIN localisation l ON o.id_localisation = l.id_localisation;

-- Détails des offres sans coordonnées
SELECT o.id, o.titre, o.status, l.ville, l.latitude, l.longitude
FROM offreemlpoi o
LEFT JOIN localisation l ON o.id_localisation = l.id_localisation
WHERE l.latitude IS NULL OR l.longitude IS NULL 
   OR l.latitude = 0 OR l.longitude = 0;
