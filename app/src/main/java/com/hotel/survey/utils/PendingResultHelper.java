package com.hotel.survey.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.hotel.survey.model.SurveyResult;

/**
 * Sauvegarde le SurveyResult en local (SharedPreferences + JSON Gson)
 * avant de l'envoyer au serveur.
 *
 * Cycle de vie :
 *   1. submitAndFinish() → save()      ← avant l'appel API
 *   2. onResponse() succès            → clear()
 *   3. onFailure() / pas de réseau    → rien (résultat conservé)
 *   4. Prochain lancement (Splash)    → hasPending() → resend() → clear()
 */
public final class PendingResultHelper {

    private static final String PREFS = "survey_pending";
    private static final String KEY   = "pending_result";

    private PendingResultHelper() {}

    /** Sérialise et stocke le résultat localement. */
    public static void save(Context ctx, SurveyResult result) {
        String json = new Gson().toJson(result);
        prefs(ctx).edit().putString(KEY, json).apply();
    }

    /** Retourne le résultat en attente, ou null s'il n'y en a pas. */
    public static SurveyResult load(Context ctx) {
        String json = prefs(ctx).getString(KEY, null);
        if (json == null) return null;
        try {
            return new Gson().fromJson(json, SurveyResult.class);
        } catch (Exception e) {
            clear(ctx); // JSON corrompu → supprimer
            return null;
        }
    }

    /** Supprime le résultat en attente (après envoi réussi). */
    public static void clear(Context ctx) {
        prefs(ctx).edit().remove(KEY).apply();
    }

    /** Vrai s'il y a un résultat non encore envoyé. */
    public static boolean hasPending(Context ctx) {
        return prefs(ctx).contains(KEY);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                  .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
