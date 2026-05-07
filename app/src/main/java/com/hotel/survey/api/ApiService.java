package com.hotel.survey.api;

import com.hotel.survey.model.GuestInfo;
import com.hotel.survey.model.SurveyResult;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @GET("api/guest.php")
    Call<GuestInfo> getGuest(@Query("ip") String ip);

    @POST("api/submit.php")
    Call<SubmitResponse> submitSurvey(@Body SurveyResult result);
}
