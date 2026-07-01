package com.hotel.survey;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hotel.survey.api.ApiClient;
import com.hotel.survey.api.SubmitResponse;
import com.hotel.survey.model.GuestInfo;
import com.hotel.survey.utils.LaterHelper;
import com.hotel.survey.utils.LocalStrings;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends AppCompatActivity {

    public static final String EXTRA_GUEST = "extra_guest";

    // ─── Vues ────────────────────────────────────────────────────────────────
    private TextView tvWelcomeTitle;
    private TextView tvEnquete;
    private TextView tvSlogan;
    private TextView tvNavHint;
    private TextView tvGuestName;
    private TextView tvRoomNumber;
    private TextView tvDeviceIp;
    private Button   btnStartNow;
    private Button   btnMaybeLater;
    private Button   btnNever;

    // ─── Données ─────────────────────────────────────────────────────────────
    private GuestInfo guest;

    // ────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        bindViews();
        handleIntentData(getIntent());

        btnStartNow.setOnClickListener(v -> startSurvey());

        // Plus tard → cooldown 2h (Android local + serveur)
        btnMaybeLater.setOnClickListener(v -> markLaterAndClose());

        // Jamais → demander confirmation avant de marquer en BD
        btnNever.setOnClickListener(v -> showNeverConfirmation());

        btnStartNow.requestFocus();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentData(intent);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Initialisation des vues + langue
    // ────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        tvWelcomeTitle = findViewById(R.id.tv_welcome_title);
        tvEnquete      = findViewById(R.id.tv_enquete);
        tvSlogan       = findViewById(R.id.tv_slogan);
        tvNavHint      = findViewById(R.id.tv_navigation_hint);
        tvGuestName    = findViewById(R.id.tv_guest_name);
        tvRoomNumber   = findViewById(R.id.tv_room_number);
        tvDeviceIp     = findViewById(R.id.tv_device_ip);
        btnStartNow    = findViewById(R.id.btn_start_now);
        btnMaybeLater  = findViewById(R.id.btn_maybe_later);
        btnNever       = findViewById(R.id.btn_never);
    }

    private void handleIntentData(Intent intent) {
        if (intent == null) return;
        guest = (GuestInfo) intent.getSerializableExtra(EXTRA_GUEST);

        String lang = (guest != null) ? guest.getLang() : "FR";

        // Appliquer le sens d'écriture RTL pour l'arabe
        if (LocalStrings.isRtl(lang)) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        // Textes dynamiques selon la langue
        tvWelcomeTitle.setText(LocalStrings.welcome(lang));
        tvEnquete.setText(LocalStrings.enquete(lang));
        tvSlogan.setText(LocalStrings.slogan(lang));
        tvNavHint.setText(LocalStrings.navHint(lang));
        btnStartNow.setText(LocalStrings.btnStart(lang));
        btnMaybeLater.setText(LocalStrings.btnLater(lang));
        btnNever.setText(LocalStrings.btnNever(lang));

        // Infos client
        if (guest != null) {
            String ip = (guest.ip_chambre != null) ? guest.ip_chambre : "";
            if (!ip.isEmpty()) tvDeviceIp.setText("IP: " + ip);

            if (guest.found) {
                String name = guest.getDisplayName();
                String room = (guest.num_chambre != null) ? guest.num_chambre.trim() : "";

                if (!name.isEmpty()) {
                    tvGuestName.setText(name);
                    tvGuestName.setVisibility(View.VISIBLE);
                } else {
                    tvGuestName.setVisibility(View.GONE);
                }

                if (!room.isEmpty()) {
                    tvRoomNumber.setText(LocalStrings.roomLabel(lang, room));
                    tvRoomNumber.setVisibility(View.VISIBLE);
                } else {
                    tvRoomNumber.setVisibility(View.GONE);
                }
            } else {
                tvGuestName.setVisibility(View.GONE);
                tvRoomNumber.setVisibility(View.GONE);
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Actions boutons
    // ────────────────────────────────────────────────────────────────────────

    private void startSurvey() {
        Intent intent = new Intent(this, SurveyActivity.class);
        intent.putExtra(SurveyActivity.EXTRA_GUEST, guest);
        startActivity(intent);
        finish();
    }

    /**
     * "Plus tard" : enregistre le cooldown 2h en local ET sur le serveur, puis ferme.
     * Double protection : local résiste à la perte réseau, serveur résiste au redémarrage.
     */
    private void markLaterAndClose() {
        // 1. Cache local (fonctionne hors réseau)
        if (guest != null && guest.ip_chambre != null) {
            LaterHelper.markLater(this, guest.ip_chambre);
        }
        // 2. Serveur (fire & forget — résiste aux redémarrages de la box TV)
        if (guest != null && guest.ip_chambre != null) {
            ApiClient.getService()
                    .surveyAction(guest.ip_chambre, "later")
                    .enqueue(new Callback<SubmitResponse>() {
                        @Override public void onResponse(Call<SubmitResponse> c, Response<SubmitResponse> r) {}
                        @Override public void onFailure(Call<SubmitResponse> c, Throwable t) {}
                    });
        }
        finishAffinity();
    }

    /**
     * Affiche une boîte de dialogue de confirmation avant de refuser définitivement.
     * Empêche les pressions accidentelles sur la télécommande.
     */
    private void showNeverConfirmation() {
        String lang = (guest != null) ? guest.getLang() : "FR";
        new AlertDialog.Builder(this)
                .setTitle(LocalStrings.neverConfirmTitle(lang))
                .setMessage(LocalStrings.neverConfirmMessage(lang))
                .setPositiveButton(LocalStrings.btnConfirm(lang), (d, w) -> markNeverAndClose())
                .setNegativeButton(LocalStrings.btnCancel(lang), null)
                .show();
    }

    /**
     * "Jamais" : notifie le serveur (fire & forget) puis ferme l'appli.
     * Le serveur met survey = -1 dans t_chambre → ne sera plus proposé.
     */
    private void markNeverAndClose() {
        // Effacer le cooldown "Plus tard" — inutile maintenant que c'est "Jamais"
        LaterHelper.clear(this);

        if (guest != null && guest.ip_chambre != null) {
            ApiClient.getService()
                    .surveyAction(guest.ip_chambre, "never")
                    .enqueue(new Callback<SubmitResponse>() {
                        @Override public void onResponse(Call<SubmitResponse> c, Response<SubmitResponse> r) {}
                        @Override public void onFailure(Call<SubmitResponse> c, Throwable t) {}
                    });
        }
        finishAffinity();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Navigation télécommande
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: {
                View focused = getCurrentFocus();
                if (focused == btnStartNow)   { startSurvey();       return true; }
                if (focused == btnMaybeLater) { finishAffinity();     return true; }
                if (focused == btnNever)      { showNeverConfirmation(); return true; }
                break;
            }
            case KeyEvent.KEYCODE_BACK:
                finishAffinity();
                return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
