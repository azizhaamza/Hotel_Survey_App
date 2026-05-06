package com.hotel.survey.model;

import com.google.gson.annotations.SerializedName;

public class QuestionResponse {

    @SerializedName("question_id")
    private final int questionId;

    @SerializedName("category")
    private final String category;

    @SerializedName("subcategory")
    private final String subcategory;

    @SerializedName("question")
    private final String question;

    @SerializedName("rating")
    private final int rating;

    @SerializedName("skipped")
    private final boolean skipped;

    public QuestionResponse(int questionId, String category, String subcategory,
                            String question, int rating, boolean skipped) {
        this.questionId = questionId;
        this.category = category;
        this.subcategory = subcategory;
        this.question = question;
        this.rating = rating;
        this.skipped = skipped;
    }
}
