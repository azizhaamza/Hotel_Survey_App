package com.hotel.survey.model;

import java.util.List;

/**
 * Payload envoyé à api/submit.php.
 * Remplace les anciens champs q1_room_rating…q5_ov_rating
 * par une liste dynamique compatible avec n'importe quel nombre de questions.
 */
public class SurveyResult {
    public String device_id;
    public String ip_chambre;
    public String num_chambre;
    public String nom_client;
    public String prenom_client;
    public String langue;

    /** Liste des réponses, une par question affichée. */
    public List<QuestionResult> reponses;
}
