package com.hotel.survey.model;

public class Question {
    private final int id;
    private final String category;    // e.g. "IL DELFINO"
    private final String subcategory; // e.g. "Restaurant" — null if none
    private final String text;        // displayed question text
    private int rating;               // 0 = skipped / not applicable

    public Question(int id, String category, String subcategory, String text) {
        this.id = id;
        this.category = category;
        this.subcategory = subcategory;
        this.text = text;
        this.rating = 0;
    }

    public int getId()           { return id; }
    public String getCategory()  { return category; }
    public String getSubcategory() { return subcategory; }
    public String getText()      { return text; }
    public int getRating()       { return rating; }
    public void setRating(int r) { this.rating = r; }
    public boolean isSkipped()   { return rating == 0; }
}
