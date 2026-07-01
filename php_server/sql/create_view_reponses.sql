-- ============================================================
-- Vue : v_survey_reponses
-- Affiche chaque réponse sur une ligne séparée (déplie le JSON)
-- Compatible MariaDB 10.6+
-- ============================================================

CREATE OR REPLACE VIEW v_survey_reponses AS
SELECT
    sr.id                  AS survey_id,
    sr.submitted_at,
    sr.num_chambre,
    sr.nom_client,
    sr.prenom_client,
    sr.langue,
    sr.ip_chambre,
    r.question_id,
    r.category_id,
    r.rating,
    r.skipped
FROM survey_results sr
JOIN JSON_TABLE(
    sr.reponses,
    '$[*]' COLUMNS (
        question_id INT    PATH '$.question_id',
        category_id INT    PATH '$.category_id',
        rating      INT    PATH '$.rating',
        skipped     INT    PATH '$.skipped'
    )
) AS r ON 1=1
ORDER BY sr.submitted_at DESC, r.question_id ASC;


-- ============================================================
-- Requêtes utiles
-- ============================================================

-- Voir toutes les réponses d'un client :
-- SELECT * FROM v_survey_reponses WHERE num_chambre = '204';

-- Note moyenne par question :
-- SELECT question_id, AVG(rating) AS note_moyenne, COUNT(*) AS nb_reponses
-- FROM v_survey_reponses
-- WHERE skipped = 0
-- GROUP BY question_id
-- ORDER BY question_id;

-- Note moyenne par chambre :
-- SELECT num_chambre, AVG(rating) AS note_moyenne
-- FROM v_survey_reponses
-- WHERE skipped = 0
-- GROUP BY num_chambre
-- ORDER BY note_moyenne DESC;
