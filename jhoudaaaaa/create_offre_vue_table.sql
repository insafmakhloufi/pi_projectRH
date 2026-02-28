-- ============================================
-- CRÉATION DE LA TABLE POUR LE SUIVI DES VUES DES OFFRES
-- ============================================
-- Cette table stocke chaque vue d'une offre pour les statistiques

CREATE TABLE IF NOT EXISTS offre_vue (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_offre INT NOT NULL,
    date_vue DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_offre) REFERENCES offreemlpoi(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- VÉRIFICATION
-- ============================================
SELECT 'Table offre_vue créée avec succès' as message;
SHOW TABLES LIKE 'offre_vue';
