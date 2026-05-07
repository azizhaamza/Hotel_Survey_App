package com.hotel.survey.model;

import java.io.Serializable;

public class GuestInfo implements Serializable {
    public boolean found;
    public String ip_chambre;
    public String nom_client;
    public String prenom_client;
    public String num_chambre;

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
}
