package com.hotel.survey.api;

import com.hotel.survey.model.GuestInfo;
import com.hotel.survey.model.SurveyQuestion;
import com.hotel.survey.model.SurveyResult;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    /**
     * Identifie la chambre par son IP LAN.
     * Retourne GuestInfo (found, nom, prenom, num_chambre, langue, survey).
     */
    @GET("api/guest.php")
    Call<GuestInfo> getGuest(@Query("ip") String ip);

    /**
     * Récupère toutes les questions avec catégories dans les 4 langues.
     * Le PHP doit faire : SELECT q.*, c.CATEGORIE_FR, c.CATEGORIE_EN, c.CATEGORIE_AR, c.CATEGORIE_ALL
     *                     FROM survey_questions q JOIN survey_categories c ON q.category_id = c.id
     *                     ORDER BY q.id
     */
    @GET("api/questions.php")
    Call<List<SurveyQuestion>> getQuestions();

    /**
     * Soumet les réponses du client.
     */
    @POST("api/submit.php")
    Call<SubmitResponse> submitSurvey(@Body SurveyResult result);

    /**
     * Enregistre l'action du client sur le bouton Plus tard / Jamais.
     * action = "never"  → met survey = -1 dans t_chambre (ne plus proposer)
     * action = "later"  → ne fait rien côté serveur (survey reste 0)
     * action = "done"   → met survey =  1 (déjà envoyé par submit, mais utile en fallback)
     */
    @GET("api/survey_action.php")
    Call<SubmitResponse> surveyAction(
            @Query("ip")     String ip,
            @Query("action") String action
    );
}
