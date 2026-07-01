-- ============================================================
-- Suivi du client ayant cliqué "Jamais" par chambre
-- Permet de détecter un changement de client (check-out/check-in)
-- et de remettre survey=0 automatiquement pour le nouveau client.
-- ============================================================

USE mythconverg;

CREATE TABLE IF NOT EXISTS survey_guest_tracking (
  ip_chambre       VARCHAR(45)  NOT NULL,
  never_client_nom VARCHAR(100) NOT NULL DEFAULT '',
  never_client_prenom VARCHAR(100) NOT NULL DEFAULT '',
  set_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (ip_chambre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Fonctionnement :
--   1. Client clique "Jamais" → survey_action.php stocke son nom ici
--   2. Nouveau client check-in → PMS met à jour nom_client dans t_chambre
--   3. Au prochain lancement de l'app, guest.php détecte la différence
--      → reset survey=0 dans t_chambre + supprime la ligne de tracking
--   4. Le nouveau client voit le sondage normalement
-- ============================================================
