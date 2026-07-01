package com.hotel.survey.model;

/**
 * Résultat d'une question individuelle, inclus dans SurveyResult.reponses.
 */
public class QuestionResult {
    public int question_id;
    public int category_id;
    public int rating;    // 1-5, ou 0 si skipped
    public int skipped;   // 1 = ignorée, 0 = notée

    public QuestionResult(int question_id, int category_id, int rating, int skipped) {
        this.question_id = question_id;
        this.category_id = category_id;
        this.rating      = rating;
        this.skipped     = skipped;
    }
}
