package com.hotel.survey.model;

import java.io.Serializable;

/**
 * Question retournée par api/questions.php
 * Le PHP doit faire un JOIN survey_questions → survey_categories
 * et retourner tous les champs de langue.
 */
public class SurveyQuestion implements Serializable {

    public int    id;
    public int    category_id;

    // Texte de la question dans les 4 langues
    public String Quest_FR;
    public String Quest_EN;
    public String Quest_AR;
    public String Quest_ALL;   // allemand

    // Libellé de la catégorie dans les 4 langues
    public String CATEGORIE_FR;
    public String CATEGORIE_EN;
    public String CATEGORIE_AR;
    public String CATEGORIE_ALL;

    public SurveyQuestion() {}

    /** Retourne le texte de la question dans la langue demandée. */
    public String getQuestion(String lang) {
        switch (normalize(lang)) {
            case "EN":  return nonempty(Quest_EN,  Quest_FR);
            case "AR":  return nonempty(Quest_AR,  Quest_FR);
            case "ALL": return nonempty(Quest_ALL, Quest_FR);
            default:    return nonempty(Quest_FR,  Quest_EN);
        }
    }

    /** Retourne le libellé de la catégorie dans la langue demandée. */
    public String getCategory(String lang) {
        switch (normalize(lang)) {
            case "EN":  return nonempty(CATEGORIE_EN,  CATEGORIE_FR);
            case "AR":  return nonempty(CATEGORIE_AR,  CATEGORIE_FR);
            case "ALL": return nonempty(CATEGORIE_ALL, CATEGORIE_FR);
            default:    return nonempty(CATEGORIE_FR,  CATEGORIE_EN);
        }
    }

    private static String normalize(String lang) {
        return (lang != null) ? lang.toUpperCase() : "FR";
    }

    private static String nonempty(String preferred, String fallback) {
        return (preferred != null && !preferred.isEmpty()) ? preferred : fallback;
    }
}
