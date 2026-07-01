package com.hotel.survey.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gère le délai "Plus tard" : si le client a cliqué "Plus tard",
 * le sondage ne sera pas reproposé avant AppConfig.LATER_DELAY_MS (2h par défaut).
 *
 * Stockage : SharedPreferences "survey_later"
 *   - later_ip        : IP de la chambre au moment du clic
 *   - later_timestamp : horodatage Unix (ms) du clic
 *
 * Le cooldown est annulé si :
 *   - L'IP de la TV change (nouveau client)
 *   - Le sondage est complété (submitAndFinish)
 *   - Le client clique "Jamais"
 */
public final class LaterHelper {

    private static final String PREFS  = "survey_later";
    private static final String KEY_TS = "later_timestamp";
    private static final String KEY_IP = "later_ip";

    private LaterHelper() {}

    /** Enregistre l'heure du clic "Plus tard" pour cette IP. */
    public static void markLater(Context ctx, String ip) {
        prefs(ctx).edit()
                .putLong(KEY_TS, System.currentTimeMillis())
                .putString(KEY_IP, ip != null ? ip : "")
                .apply();
    }

    /**
     * Retourne true si le délai de 2h n'est pas encore écoulé pour cette IP.
     * Si l'IP est différente (nouveau client), efface le cooldown et retourne false.
     */
    public static boolean isInCooldown(Context ctx, String ip) {
        SharedPreferences p = prefs(ctx);
        String savedIp = p.getString(KEY_IP, "");

        // IP différente = nouveau client → on efface et on laisse passer
        if (ip == null || !ip.equals(savedIp)) {
            clear(ctx);
            return false;
        }

        long ts = p.getLong(KEY_TS, 0L);
        if (ts == 0L) return false;

        return (System.currentTimeMillis() - ts) < AppConfig.LATER_DELAY_MS;
    }

    /** Retourne le temps restant en millisecondes (0 si pas de cooldown actif). */
    public static long remainingMs(Context ctx, String ip) {
        if (!isInCooldown(ctx, ip)) return 0L;
        long ts = prefs(ctx).getLong(KEY_TS, 0L);
        long elapsed = System.currentTimeMillis() - ts;
        return Math.max(0L, AppConfig.LATER_DELAY_MS - elapsed);
    }

    /** Efface le cooldown (survey complété ou "Jamais" confirmé). */
    public static void clear(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
