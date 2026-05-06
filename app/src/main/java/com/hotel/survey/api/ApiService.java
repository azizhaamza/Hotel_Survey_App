package com.hotel.survey.api;

import com.hotel.survey.model.SurveyResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("survey/submit")
    Call<Void> submitSurvey(@Body SurveyResult result);
}
