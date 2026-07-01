package com.hotel.survey.utils;

/**
 * Constantes de configuration centralisées.
 * Modifier ici pour ajuster les délais sans toucher aux Activities.
 */
public final class AppConfig {

    private AppConfig() {}

    // ─── Splash : identification de la chambre ───────────────────────────────

    /** Intervalle entre deux tentatives réseau (ms). */
    public static final int SPLASH_RETRY_INTERVAL_MS = 10_000;

    /** Nombre max de tentatives avant abandon (12 × 10s = 2 min). */
    public static final int SPLASH_MAX_RETRIES = 12;

    /** Durée d'affichage du message "TV non enregistrée" avant fermeture (ms). */
    public static final int SPLASH_NOT_REGISTERED_MS = 8_000;

    /** Durée max du WakeLock écran au démarrage (ms). */
    public static final long WAKELOCK_TIMEOUT_MS = 10 * 60 * 1_000L; // 10 min

    // ─── Survey : chargement des questions ───────────────────────────────────

    /** Intervalle entre deux tentatives de chargement des questions (ms). */
    public static final int SURVEY_RETRY_INTERVAL_MS = 10_000;

    /** Nombre max de tentatives avant abandon (24 × 10s = 4 min). */
    public static final int SURVEY_MAX_RETRIES = 24;

    // ─── ThankYou : fermeture automatique ────────────────────────────────────

    /** Durée d'affichage de l'écran de remerciement avant fermeture automatique (ms). */
    public static final int THANK_YOU_AUTO_CLOSE_MS = 10_000;

    // ─── "Plus tard" : délai avant de reproposer le sondage ─────────────────

    /** Durée minimale entre "Plus tard" et la prochaine proposition (ms). */
    //public static final long LATER_DELAY_MS = 2 * 60 * 60 * 1_000L; // 2 heures
    public static final long LATER_DELAY_MS = 5 * 60 * 1_000L; // 5 minutes
}
