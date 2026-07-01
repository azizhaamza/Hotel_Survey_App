package com.hotel.survey;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hotel.survey.api.ApiClient;
import com.hotel.survey.api.SubmitResponse;
import com.hotel.survey.model.GuestInfo;
import com.hotel.survey.model.SurveyResult;
import com.hotel.survey.utils.AppConfig;
import com.hotel.survey.utils.LaterHelper;
import com.hotel.survey.utils.LocalStrings;
import com.hotel.survey.utils.NetworkUtils;
import com.hotel.survey.utils.PendingResultHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    // ─── Paramètres de retry (voir AppConfig pour modifier les durées) ────────
    private static final int RETRY_INTERVAL_MS = AppConfig.SPLASH_RETRY_INTERVAL_MS;
    private static final int MAX_RETRIES       = AppConfig.SPLASH_MAX_RETRIES;

    // ─── Vues ────────────────────────────────────────────────────────────────
    private TextView    tvStatus;
    private ProgressBar progressLoading;
    private Button      btnRetry;

    // ─── État interne ────────────────────────────────────────────────────────
    private int                  retryCount = 0;
    private boolean              noIp       = false;   // true = pas d'IP, false = serveur down
    private CountDownTimer       countDown  = null;
    private PowerManager.WakeLock wakeLock  = null;

    // ────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Allumer l'écran si la TV est en veille (avant setContentView)
        wakeUpScreen();

        setContentView(R.layout.activity_splash);

        tvStatus        = findViewById(R.id.tv_status);
        progressLoading = findViewById(R.id.progress_loading);
        btnRetry        = findViewById(R.id.btn_retry);

        // Bouton retry manuel : repart de zéro
        btnRetry.setOnClickListener(v -> {
            retryCount = 0;
            startLookup();
        });

        startLookup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCountDown();
        releaseWakeLock();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Allumage écran TV
    // ────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private void wakeUpScreen() {
        // Flags de fenêtre — force l'affichage par-dessus le verrou et allume l'écran
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        // WakeLock PowerManager — réveille physiquement le rétroéclairage
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "HotelSurvey:WakeLock"
            );
            wakeLock.acquire(AppConfig.WAKELOCK_TIMEOUT_MS);
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Lookup IP → chambre
    // ────────────────────────────────────────────────────────────────────────

    private void startLookup() {
        cancelCountDown();
        showLoading(LocalStrings.splashSearching());

        String ip = NetworkUtils.getActiveIp(this);

        if (ip == null || ip.isEmpty()) {
            // Pas d'IP sur la TV (WiFi non connecté)
            noIp = true;
            handleFailure();
            return;
        }

        // IP trouvée → appel API
        noIp = false;
        retryCount = 0;
        tvStatus.setText("Identification de la chambre...  (" + ip + ")");

        ApiClient.getService().getGuest(ip).enqueue(new Callback<GuestInfo>() {
            @Override
            public void onResponse(Call<GuestInfo> call, Response<GuestInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GuestInfo guest = response.body();
                    guest.ip_chambre = ip;

                    // TV non enregistrée dans la BD → afficher message puis fermer
                    if (!guest.found) {
                        showNotRegistered(ip);
                        return;
                    }

                    // Survey déjà complété ou refusé → fermer silencieusement
                    if (guest.survey != 0) {
                        finishAffinity();
                        return;
                    }

                    // "Plus tard" : vérifier d'abord le serveur, puis le cache local
                    // (le serveur résiste aux redémarrages de la box)
                    if (guest.later_active || LaterHelper.isInCooldown(SplashActivity.this, ip)) {
                        finishAffinity();
                        return;
                    }

                    launchWelcome(guest);
                } else {
                    showError(LocalStrings.splashServerError(response.code()));
                }
            }

            @Override
            public void onFailure(Call<GuestInfo> call, Throwable t) {
                // IP présente mais serveur injoignable → retry automatique
                noIp = false;
                handleFailure();
            }
        });
    }

    /**
     * Gère un échec réseau (no IP) ou serveur (onFailure).
     * Prop 2 : après MAX_RETRIES → fermeture silencieuse (finishAffinity).
     */
    private void handleFailure() {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            scheduleCountDown();
        } else {
            // Prop 2 : timeout → fermer silencieusement, le survey sera reproposé
            finishAffinity();
        }
    }

    /**
     * Affiche le bouton retry + un compte à rebours de 10 s, puis réessaie.
     * Prop 1 : le message distingue "WiFi non connecté" vs "Serveur indisponible".
     */
    private void scheduleCountDown() {
        final int     attempt    = retryCount;
        final boolean isNoIp     = noIp;
        cancelCountDown();

        progressLoading.setVisibility(View.GONE);
        btnRetry.setVisibility(View.VISIBLE);
        btnRetry.requestFocus();

        countDown = new CountDownTimer(RETRY_INTERVAL_MS, 1_000) {
            @Override public void onTick(long millisUntilFinished) {
                long s = millisUntilFinished / 1000;
                tvStatus.setText(isNoIp
                        ? LocalStrings.splashNoWifi(attempt, MAX_RETRIES, s)
                        : LocalStrings.splashServerDown(attempt, MAX_RETRIES, s));
            }
            @Override public void onFinish() {
                startLookup();
            }
        }.start();
    }

    private void cancelCountDown() {
        if (countDown != null) {
            countDown.cancel();
            countDown = null;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Navigation
    // ────────────────────────────────────────────────────────────────────────

    /**
     * TV non enregistrée dans la BD :
     * affiche un message FR+EN pendant 8 secondes puis ferme l'application.
     */
    private void showNotRegistered(String ip) {
        cancelCountDown();
        progressLoading.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        tvStatus.setText(LocalStrings.splashNotRegistered(ip));

        countDown = new CountDownTimer(AppConfig.SPLASH_NOT_REGISTERED_MS, AppConfig.SPLASH_NOT_REGISTERED_MS) {
            @Override public void onTick(long ms) {}
            @Override public void onFinish() { finishAffinity(); }
        }.start();
    }

    private void launchWelcome(GuestInfo guest) {
        // Tenter de renvoyer un résultat en attente (connexion perdue lors du dernier submit)
        trySendPendingResult(guest.ip_chambre);

        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra(WelcomeActivity.EXTRA_GUEST, guest);
        startActivity(intent);
        finish();
    }

    /**
     * Renvoie silencieusement le résultat en attente si :
     *   - il y en a un sauvegardé localement
     *   - il appartient à la même chambre (même IP) que le client actuel
     *
     * Si l'IP ne correspond pas (nouveau client dans la même chambre),
     * on supprime le pending obsolète plutôt que d'envoyer de fausses données.
     */
    private void trySendPendingResult(String currentIp) {
        if (!PendingResultHelper.hasPending(this)) return;

        SurveyResult pending = PendingResultHelper.load(this);
        if (pending == null) return;

        // Vérifier que le pending appartient bien à la chambre actuelle
        if (currentIp != null && !currentIp.isEmpty()
                && !currentIp.equals(pending.ip_chambre)) {
            // Chambre différente → supprimer le résultat obsolète
            PendingResultHelper.clear(this);
            return;
        }

        final Context appCtx = getApplicationContext();
        ApiClient.getService().submitSurvey(pending).enqueue(new Callback<SubmitResponse>() {
            @Override public void onResponse(Call<SubmitResponse> c, Response<SubmitResponse> r) {
                if (r.isSuccessful()) PendingResultHelper.clear(appCtx);
            }
            @Override public void onFailure(Call<SubmitResponse> c, Throwable t) {
                // Toujours pas de réseau → on réessaiera au prochain lancement
            }
        });
    }

    // ────────────────────────────────────────────────────────────────────────
    // États visuels
    // ────────────────────────────────────────────────────────────────────────

    private void showLoading(String message) {
        progressLoading.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);
        tvStatus.setText(message);
    }

    private void showError(String message) {
        cancelCountDown();
        progressLoading.setVisibility(View.GONE);
        btnRetry.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
        btnRetry.requestFocus();
    }
}
