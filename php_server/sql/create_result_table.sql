-- ============================================================
-- Table unique pour les résultats du sondage (structure JSON)
-- Base de données : mythconverg  — MariaDB 10.6+
-- ============================================================

USE mythconverg;

CREATE TABLE IF NOT EXISTS `survey_results` (
    `id`            INT(11)      NOT NULL AUTO_INCREMENT,
    `ip_chambre`    VARCHAR(25)  NOT NULL DEFAULT '',
    `num_chambre`   VARCHAR(128) NOT NULL DEFAULT '',
    `nom_client`    VARCHAR(255) NOT NULL DEFAULT '',
    `prenom_client` VARCHAR(255) NOT NULL DEFAULT '',
    `device_id`     VARCHAR(255) NOT NULL DEFAULT '',
    `langue`        VARCHAR(10)  NOT NULL DEFAULT 'FR',
    `submitted_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `reponses`      JSON         NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_ip`      (`ip_chambre`),
    KEY `idx_chambre` (`num_chambre`),
    KEY `idx_date`    (`submitted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Exemple : note moyenne question_id=1
-- SELECT AVG(r.rating) FROM survey_results sr,
--   JSON_TABLE(sr.reponses,'$[*]' COLUMNS(
--     question_id INT PATH '$.question_id',
--     rating INT PATH '$.rating', skipped INT PATH '$.skipped')) AS r
-- WHERE r.question_id=1 AND r.skipped=0;
