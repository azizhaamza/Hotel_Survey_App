package com.hotel.survey.utils;

/**
 * Tous les textes de l'UI dans les 4 langues supportées.
 * Codes langue issus de t_chambre.langue : "FR", "EN", "AR", "ALL" (allemand).
 */
public final class LocalStrings {

    private LocalStrings() {}

    // ─── Écran d'accueil (WelcomeActivity) ──────────────────────────────────

    public static String welcome(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Welcome!";
            case "AR":  return "مرحباً!";
            case "ALL": return "Willkommen!";
            default:    return "Bienvenue !";
        }
    }

    public static String enquete(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Satisfaction survey";
            case "AR":  return "استبيان رضا";
            case "ALL": return "Zufriedenheitsumfrage";
            default:    return "Enquête de satisfaction";
        }
    }

    public static String slogan(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Your satisfaction is our priority.";
            case "AR":  return "رضاكم أولويتنا.";
            case "ALL": return "Ihre Zufriedenheit ist unsere Priorität.";
            default:    return "Votre satisfaction est notre priorité.";
        }
    }

    public static String btnStart(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Start";
            case "AR":  return "ابدأ";
            case "ALL": return "Starten";
            default:    return "Commencer";
        }
    }

    public static String btnLater(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Later";
            case "AR":  return "لاحقاً";
            case "ALL": return "Später";
            default:    return "Plus tard";
        }
    }

    public static String btnNever(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Never";
            case "AR":  return "لا أريد";
            case "ALL": return "Nie";
            default:    return "Jamais";
        }
    }

    public static String navHint(String lang) {
        switch (norm(lang)) {
            case "EN":  return "◄ ► Navigate  |  OK to Confirm";
            case "AR":  return "◄ ► التنقل  |  موافق للتأكيد";
            case "ALL": return "◄ ► Navigieren  |  OK zum Bestätigen";
            default:    return "◄ ► Naviguer  |  OK pour Confirmer";
        }
    }

    public static String roomLabel(String lang, String room) {
        switch (norm(lang)) {
            case "EN":  return "Room: " + room;
            case "AR":  return "الغرفة: " + room;
            case "ALL": return "Zimmer: " + room;
            default:    return "Chambre : " + room;
        }
    }

    // ─── Écran du sondage (SurveyActivity) ──────────────────────────────────

    public static String hintNext(String lang) {
        switch (norm(lang)) {
            case "EN":  return "◄ ► Select rating  |  OK → Next  |  ↑ Skip";
            case "AR":  return "◄ ► اختر التقييم  |  موافق ← التالي  |  ↑ تخطي";
            case "ALL": return "◄ ► Bewertung wählen  |  OK → Weiter  |  ↑ Überspringen";
            default:    return "◄ ► Choisir la note  |  OK → Suivant  |  ↑ Ignorer";
        }
    }

    public static String hintSubmit(String lang) {
        switch (norm(lang)) {
            case "EN":  return "◄ ► Select rating  |  OK → Submit  |  ↑ Skip";
            case "AR":  return "◄ ► اختر التقييم  |  موافق ← إرسال  |  ↑ تخطي";
            case "ALL": return "◄ ► Bewertung wählen  |  OK → Senden  |  ↑ Überspringen";
            default:    return "◄ ► Choisir la note  |  OK → Envoyer  |  ↑ Ignorer";
        }
    }

    public static String ratingIgnore(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Press ↑ to skip";
            case "AR":  return "اضغط ↑ للتخطي";
            case "ALL": return "↑ drücken zum Überspringen";
            default:    return "Appuyez sur ↑ pour ignorer";
        }
    }

    public static String[] ratingLabels(String lang) {
        switch (norm(lang)) {
            case "EN":  return new String[]{"", "Insufficient", "Average", "Good", "Very good", "Excellent"};
            case "AR":  return new String[]{"", "غير كافٍ", "متوسط", "جيد", "جيد جداً", "ممتاز"};
            case "ALL": return new String[]{"", "Unzureichend", "Mittel", "Gut", "Sehr gut", "Ausgezeichnet"};
            default:    return new String[]{"", "Insuffisant", "Moyen", "Bon", "Très bon", "Excellent"};
        }
    }

    public static String loadingQuestions(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Loading questions...";
            case "AR":  return "جارٍ تحميل الأسئلة...";
            case "ALL": return "Fragen werden geladen...";
            default:    return "Chargement des questions...";
        }
    }

    public static String errorQuestions(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Unable to load questions.\nPlease contact the reception.";
            case "AR":  return "تعذّر تحميل الأسئلة.\nيرجى الاتصال بالاستقبال.";
            case "ALL": return "Fragen konnten nicht geladen werden.\nBitte wenden Sie sich an die Rezeption.";
            default:    return "Impossible de charger les questions.\nVeuillez contacter la réception.";
        }
    }

    // ─── Écran de remerciement (ThankYouActivity) ────────────────────────────

    public static String thankYouTitle(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Thank you!";
            case "AR":  return "شكراً لك!";
            case "ALL": return "Danke!";
            default:    return "Merci !";
        }
    }

    public static String thankYouSlogan(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Your satisfaction is our priority.";
            case "AR":  return "رضاكم أولويتنا.";
            case "ALL": return "Ihre Zufriedenheit ist unsere Priorität.";
            default:    return "Votre satisfaction est notre priorité.";
        }
    }

    public static String thankYouBody(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Your feedback has been recorded.\nWe hope to welcome you again very soon.";
            case "AR":  return "تم تسجيل رأيك.\nنأمل في استقبالك مجدداً قريباً.";
            case "ALL": return "Ihre Meinung wurde gespeichert.\nWir hoffen, Sie bald wieder begrüßen zu dürfen.";
            default:    return "Votre avis a bien été enregistré.\nNous espérons vous accueillir de nouveau très bientôt.";
        }
    }

    /** @param secondsLeft secondes restantes avant fermeture automatique */
    public static String thankYouTimer(String lang, int secondsLeft) {
        switch (norm(lang)) {
            case "EN":  return "This window will close in " + secondsLeft + " second" + (secondsLeft != 1 ? "s" : "") + "...";
            case "AR":  return "ستُغلق هذه النافذة خلال " + secondsLeft + " ثوانٍ...";
            case "ALL": return "Dieses Fenster schließt sich in " + secondsLeft + " Sekunden...";
            default:    return "Cette fenêtre se fermera dans " + secondsLeft + " seconde" + (secondsLeft != 1 ? "s" : "") + "...";
        }
    }

    // Confirmation "Jamais" ────────────────────────────────────────────────────

    public static String neverConfirmTitle(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Are you sure?";
            case "AR":  return "هل أنت متأكد؟";
            case "ALL": return "Sind Sie sicher?";
            default:    return "Confirmer ?";
        }
    }

    public static String neverConfirmMessage(String lang) {
        switch (norm(lang)) {
            case "EN":  return "You will no longer be asked to participate in our survey.";
            case "AR":  return "لن تتلقى دعوات للمشاركة في استبياننا مستقبلاً.";
            case "ALL": return "Sie werden nicht mehr zur Umfrage eingeladen.";
            default:    return "Vous ne serez plus invité à participer à notre enquête.";
        }
    }

    public static String btnConfirm(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Confirm";
            case "AR":  return "تأكيد";
            case "ALL": return "Bestätigen";
            default:    return "Confirmer";
        }
    }

    public static String btnCancel(String lang) {
        switch (norm(lang)) {
            case "EN":  return "Cancel";
            case "AR":  return "إلغاء";
            case "ALL": return "Abbrechen";
            default:    return "Annuler";
        }
    }

    // ─── SplashActivity (réseau) ─────────────────────────────────────────────

    // Les messages Splash sont affichés AVANT que la langue soit connue
    // → on affiche FR + EN + AR simultanément sur 3 lignes.

    public static String splashSearching() {
        return "Identification de la chambre...\n"
             + "Loading room info...\n"
             + "جارٍ تحديد الغرفة...";
    }

    /** Retry générique pour SurveyActivity (chargement questions). */
    public static String splashRetrying(int attempt, int max, long secondsLeft) {
        return "Nouvelle tentative dans " + secondsLeft + "s  (" + attempt + "/" + max + ")\n"
             + "Retrying in " + secondsLeft + "s";
    }

    /** Pas d'IP sur la TV (WiFi non connecté). */
    public static String splashNoWifi(int attempt, int max, long secondsLeft) {
        return "WiFi non connecté — No WiFi connection\n"
             + "Nouvelle tentative dans " + secondsLeft + "s  ("  + attempt + "/" + max + ")\n"
             + "Retrying in " + secondsLeft + "s";
    }

    /** IP présente mais serveur injoignable. */
    public static String splashServerDown(int attempt, int max, long secondsLeft) {
        return "Serveur indisponible — Server unavailable\n"
             + "Nouvelle tentative dans " + secondsLeft + "s  (" + attempt + "/" + max + ")\n"
             + "Retrying in " + secondsLeft + "s";
    }

    public static String splashServerError(int code) {
        return "Erreur serveur (" + code + ") — Server error\n"
             + "Veuillez contacter la réception / Please contact reception";
    }

    /**
     * Affiché quand l'IP de la TV n'est dans aucune chambre de la BD.
     * @param ip adresse IP de la TV
     */
    public static String splashNotRegistered(String ip) {
        return "Cette TV (" + ip + ") n'est pas enregistrée dans le système.\n"
             + "This TV (" + ip + ") is not registered in the system.";
    }

    // ─── Utilitaire ─────────────────────────────────────────────────────────

    /** Vrai si la langue s'écrit de droite à gauche. */
    public static boolean isRtl(String lang) {
        return "AR".equals(norm(lang));
    }

    private static String norm(String lang) {
        return (lang != null && !lang.isEmpty()) ? lang.toUpperCase() : "FR";
    }
}
