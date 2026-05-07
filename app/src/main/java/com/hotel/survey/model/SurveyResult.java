package com.hotel.survey.model;

public class SurveyResult {
    public String ip_chambre;
    public String num_chambre;
    public String nom_client;
    public String prenom_client;
    public String device_id;

    // Q1 — YOUR ROOM
    public int q1_room_rating;
    public int q1_room_skipped;

    // Q2 — FRONT DESK
    public int q2_fd_rating;
    public int q2_fd_skipped;

    // Q3 — BREAKFAST
    public int q3_bk_rating;
    public int q3_bk_skipped;

    // Q4 — SPA CENTER
    public int q4_spa_rating;
    public int q4_spa_skipped;

    // Q5 — OVERALL STAY
    public int q5_ov_rating;
    public int q5_ov_skipped;
}
