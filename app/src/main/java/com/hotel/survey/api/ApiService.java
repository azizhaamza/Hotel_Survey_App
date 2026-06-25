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

    @GET("api/guest.php")
    Call<GuestInfo> getGuest(@Query("ip") String ip);

    // Nouvelle méthode pour récupérer la liste de toutes les questions
    @GET("api/questions.php")
    Call<List<SurveyQuestion>> getQuestions();

    @POST("api/submit.php")
    Call<SubmitResponse> submitSurvey(@Body SurveyResult result);
}
