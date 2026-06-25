package com.hotel.survey.model;

import java.io.Serializable;

public class SurveyQuestion implements Serializable {
    public int id;
    public int category_id;
    public String nom_categorie;
    public String question;

    public SurveyQuestion() {}
}