package com.hotel.survey;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hotel.survey.api.ApiClient;
import com.hotel.survey.model.GuestInfo;
import com.hotel.survey.utils.NetworkUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private TextView tvStatus;
    private ProgressBar progressLoading;
    private Button btnRetry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        tvStatus       = findViewById(R.id.tv_status);
        progressLoading = findViewById(R.id.progress_loading);
        btnRetry       = findViewById(R.id.btn_retry);

        btnRetry.setOnClickListener(v -> startLookup());

        startLookup();
    }

    private void startLookup() {
        showLoading("Identification de votre chambre...");

        String ip = NetworkUtils.getActiveIp(this);

        if (ip == null || ip.isEmpty()) {
            showError("Aucune connexion réseau.\n" + "Veuillez vérifier le câble Ethernet ou la connexion Wi-Fi de votre téléviseur.");
            return;
        }

        tvStatus.setText("Identification de la chambre... : " + ip);

        ApiClient.getService().getGuest(ip).enqueue(new Callback<GuestInfo>() {
            @Override
            public void onResponse(Call<GuestInfo> call, Response<GuestInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GuestInfo guest = response.body();
                    guest.ip_chambre = ip;
                    launchSurvey(guest);
                } else {
                    showError("Erreur serveur (" + response.code() + ").\nVeuillez contacter la réception.");
                }
            }

            @Override
            public void onFailure(Call<GuestInfo> call, Throwable t) {
                showError("Impossible de joindre le serveur.\\nVeuillez contacter la réception.");
            }
        });
    }

    private void launchSurvey(GuestInfo guest) {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra(WelcomeActivity.EXTRA_GUEST, guest);
        startActivity(intent);
        finish();
    }

    private void showLoading(String message) {
        progressLoading.setVisibility(View.VISIBLE);
        btnRetry.setVisibility(View.GONE);
        tvStatus.setText(message);
    }

    private void showError(String message) {
        progressLoading.setVisibility(View.GONE);
        btnRetry.setVisibility(View.VISIBLE);
        tvStatus.setText(message);
        btnRetry.requestFocus();
    }
}
