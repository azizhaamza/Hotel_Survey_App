-- ============================================================
-- Migration : survey_responses statique -> dynamique
-- Compatible MariaDB / MySQL  (PHP 5.6 server)
-- ============================================================
-- AVANT  : 1 table "survey_responses" avec colonnes q1..q8 figees
-- APRES  : 2 tables separees
--            survey_results   -> 1 ligne par sondage (entete)
--            survey_responses -> N lignes par sondage (1 par question)
-- ============================================================

USE mythconverg;

-- 0. Sauvegarde de l'ancienne table (donnees conservees)
RENAME TABLE survey_responses TO survey_responses_backup;

-- 1. Table entete (une ligne par sondage)
CREATE TABLE IF NOT EXISTS survey_results (
  id             INT(11)      NOT NULL AUTO_INCREMENT,
  ip_chambre     VARCHAR(45)  NOT NULL DEFAULT '',
  num_chambre    VARCHAR(20)  NOT NULL DEFAULT '',
  nom_client     VARCHAR(100) NOT NULL DEFAULT '',
  prenom_client  VARCHAR(100) NOT NULL DEFAULT '',
  device_id      VARCHAR(64)  NOT NULL DEFAULT '',
  langue         VARCHAR(10)  NOT NULL DEFAULT 'FR',
  submitted_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_ip      (ip_chambre),
  KEY idx_chambre (num_chambre),
  KEY idx_date    (submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Table reponses dynamiques (N lignes par sondage)
CREATE TABLE IF NOT EXISTS survey_responses (
  id           INT(11)    NOT NULL AUTO_INCREMENT,
  result_id    INT(11)    NOT NULL,
  question_id  INT(11)    NOT NULL,
  category_id  INT(11)    NOT NULL DEFAULT 0,
  rating       TINYINT(4) NOT NULL DEFAULT 0,
  skipped      TINYINT(4) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_result   (result_id),
  KEY idx_question (question_id),
  CONSTRAINT fk_response_result
    FOREIGN KEY (result_id) REFERENCES survey_results(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- MIGRATION DES ANCIENNES DONNEES (optionnel)
-- Decommente et adapte les question_id selon tes vrais IDs
-- dans la table survey_questions
-- ============================================================

-- INSERT INTO survey_results
--     (ip_chambre, num_chambre, nom_client, prenom_client, device_id, submitted_at)
-- SELECT ip_chambre, num_chambre, nom_client, prenom_client, device_id, submitted_at
-- FROM survey_responses_backup
-- ORDER BY id;

-- INSERT INTO survey_responses (result_id, question_id, category_id, rating, skipped)
-- SELECT r.id, 1, 1, b.q1_room_rating, b.q1_room_skipped
-- FROM survey_results r
-- JOIN survey_responses_backup b
--   ON r.device_id=b.device_id AND r.submitted_at=b.submitted_at
-- UNION ALL
-- SELECT r.id, 2, 2, b.q2_fd_rating, b.q2_fd_skipped
-- FROM survey_results r JOIN survey_responses_backup b ON r.device_id=b.device_id AND r.submitted_at=b.submitted_at
-- UNION ALL
-- SELECT r.id, 3, 3, b.q3_bk_rating, b.q3_bk_skipped
-- FROM survey_results r JOIN survey_responses_backup b ON r.device_id=b.device_id AND r.submitted_at=b.submitted_at
-- UNION ALL
-- SELECT r.id, 4, 4, b.q4_spa_rating, b.q4_spa_skipped
-- FROM survey_results r JOIN survey_responses_backup b ON r.device_id=b.device_id AND r.submitted_at=b.submitted_at
-- UNION ALL
-- SELECT r.id, 5, 5, b.q5_ov_rating, b.q5_ov_skipped
-- FROM survey_results r JOIN survey_responses_backup b ON r.device_id=b.device_id AND r.submitted_at=b.submitted_at;

-- ============================================================
-- VERIFICATION
-- ============================================================

-- Compte les reponses par sondage :
-- SELECT sr.id, sr.nom_client, sr.num_chambre, sr.submitted_at,
--        COUNT(resp.id) AS nb_reponses
-- FROM survey_results sr
-- LEFT JOIN survey_responses resp ON resp.result_id = sr.id
-- GROUP BY sr.id ORDER BY sr.submitted_at DESC;

-- Lire toutes les reponses d'un sondage (ex: result_id = 1) :
-- SELECT q.Quest_FR, resp.rating, resp.skipped
-- FROM survey_responses resp
-- JOIN survey_questions q ON q.id = resp.question_id
-- WHERE resp.result_id = 1
-- ORDER BY q.id;

-- ============================================================
-- RESULTAT :
--   9 questions  ->  9 lignes dans survey_responses / result_id
--  10 questions  -> 10 lignes dans survey_responses / result_id
--  Pas de modification de structure si tu changes le nombre
--  de questions dans survey_questions.
-- ============================================================
