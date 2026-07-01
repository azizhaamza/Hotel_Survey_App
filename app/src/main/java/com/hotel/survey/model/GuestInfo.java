package com.hotel.survey.model;

import java.io.Serializable;

public class GuestInfo implements Serializable {

    public boolean found;
    public String  ip_chambre;
    public String  nom_client;
    public String  prenom_client;
    public String  num_chambre;

    /** Langue de la chambre : "FR", "EN", "AR", "ALL" (allemand). */
    public String  langue;

    /**
     * true si le client a cliqué "Plus tard" il y a moins de 2h (côté serveur).
     * L'app doit fermer silencieusement sans reproposer le sondage.
     */
    public boolean later_active;

    /**
     * État du sondage dans t_chambre.survey :
     *   0 = pas encore fait  → proposer le survey
     *   1 = déjà complété    → ne plus proposer
     *  -1 = jamais (refus)   → ne plus proposer
     */
    public int survey;

    public GuestInfo() {}

    public String getDisplayName() {
        if (!found) return "";
        String first = (prenom_client != null) ? prenom_client.trim() : "";
        String last  = (nom_client    != null) ? nom_client.trim().toUpperCase() : "";
        if (first.isEmpty() && last.isEmpty()) return "";
        if (first.isEmpty()) return last;
        if (last.isEmpty())  return first;
        return first + " " + last;
    }

    /** Langue normalisée, jamais null, défaut "FR". */
    public String getLang() {
        return (langue != null && !langue.isEmpty()) ? langue.toUpperCase() : "FR";
    }
}
