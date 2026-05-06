package com.hotel.survey.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SurveyResult {

    @SerializedName("timestamp")
    private final String timestamp;

    @SerializedName("device_id")
    private final String deviceId;

    @SerializedName("responses")
    private final List<QuestionResponse> responses;

    public SurveyResult(String timestamp, String deviceId, List<QuestionResponse> responses) {
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.responses = responses;
    }
}
