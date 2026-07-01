-- Ajouter la colonne later_until à survey_guest_tracking
-- (si la table existe déjà sans cette colonne)
-- Exécuter dans phpMyAdmin ou MySQL console

ALTER TABLE survey_guest_tracking
  ADD COLUMN later_until DATETIME NULL DEFAULT NULL
  AFTER set_at;
