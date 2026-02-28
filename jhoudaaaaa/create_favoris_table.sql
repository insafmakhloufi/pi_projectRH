-- Table pour stocker les offres favorites des utilisateurs
CREATE TABLE IF NOT EXISTS favoris_offre (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    offre_id INT NOT NULL,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Contrainte unique pour éviter les doublons
    UNIQUE KEY unique_favoris (user_id, offre_id),
    
    -- Index pour performance
    INDEX idx_user_id (user_id),
    INDEX idx_offre_id (offre_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
