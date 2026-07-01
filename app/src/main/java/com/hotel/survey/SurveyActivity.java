package com.hotel.survey;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hotel.survey.api.ApiClient;
import com.hotel.survey.api.SubmitResponse;
import com.hotel.survey.model.GuestInfo;
import com.hotel.survey.model.QuestionResult;
import com.hotel.survey.model.SurveyQuestion;
import com.hotel.survey.model.SurveyResult;
import com.hotel.survey.utils.AppConfig;
import com.hotel.survey.utils.DeviceUtils;
import com.hotel.survey.utils.LaterHelper;
import com.hotel.survey.utils.LocalStrings;
import com.hotel.survey.utils.PendingResultHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SurveyActivity extends AppCompatActivity {

    public static final String EXTRA_GUEST = "extra_guest";

    // ─── Paramètres de retry (voir AppConfig pour modifier les durées) ────────
    private static final int RETRY_INTERVAL_MS = AppConfig.SURVEY_RETRY_INTERVAL_MS;
    private static final int MAX_RETRIES       = AppConfig.SURVEY_MAX_RETRIES;

    // ─── Données du sondage ──────────────────────────────────────────────────
    private List<SurveyQuestion> questions     = new ArrayList<>();
    private int[]   ratings;
    private int[]   skipped;
    private int     currentIndex  = 0;
    private int     currentRating = 0;
    private String  lang          = "FR";
    private String[] ratingLabels;
    private GuestInfo guest;

    // ─── Retry state ─────────────────────────────────────────────────────────
    private int           retryCount = 0;
    private CountDownTimer countDown = null;

    // ─── Vues — overlay de chargement ────────────────────────────────────────
    private View        layoutLoading;
    private ProgressBar progressQuestions;
    private TextView    tvLoadingMsg;
    private Button      btnRetryQuestions;

    // ─── Vues — sondage ──────────────────────────────────────────────────────
    private View        layoutSurvey;
    private TextView    tvGuestHeader;
    private TextView    tvQuestionNumber;
    private ProgressBar progressBar;
    private TextView    tvCategory;
    private TextView    tvQuestion;
    private TextView    tvRatingLabel;
    private TextView    tvHint;
    private TextView[]  stars;

    // ────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        guest        = (GuestInfo) getIntent().getSerializableExtra(EXTRA_GUEST);
        lang         = (guest != null) ? guest.getLang() : "FR";
        ratingLabels = LocalStrings.ratingLabels(lang);

        if (LocalStrings.isRtl(lang)) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        bindViews();
        showGuestHeader();

        // Retry manuel : repart de zéro
        btnRetryQuestions.setOnClickListener(v -> {
            retryCount = 0;
            loadQuestionsFromApi();
        });

        loadQuestionsFromApi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelCountDown();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Init vues
    // ────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        layoutLoading     = findViewById(R.id.layout_loading);
        progressQuestions = findViewById(R.id.progress_questions);
        tvLoadingMsg      = findViewById(R.id.tv_loading_msg);
        btnRetryQuestions = findViewById(R.id.btn_retry_questions);

        layoutSurvey     = findViewById(R.id.layout_survey);
        tvGuestHeader    = findViewById(R.id.tv_guest_header);
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        progressBar      = findViewById(R.id.progress_bar);
        tvCategory       = findViewById(R.id.tv_category);
        tvQuestion       = findViewById(R.id.tv_question);
        tvRatingLabel    = findViewById(R.id.tv_rating_label);
        tvHint           = findViewById(R.id.tv_hint);
        stars = new TextView[]{
            findViewById(R.id.star1), findViewById(R.id.star2), findViewById(R.id.star3),
            findViewById(R.id.star4), findViewById(R.id.star5)
        };
    }

    private void showGuestHeader() {
        if (guest != null && guest.found) {
            String name = guest.getDisplayName();
            String room = (guest.num_chambre != null) ? guest.num_chambre : "";
            tvGuestHeader.setText(getString(R.string.header_with_name, name, room));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Chargement des questions avec retry automatique
    // ────────────────────────────────────────────────────────────────────────

    private void loadQuestionsFromApi() {
        cancelCountDown();
        showLoading(LocalStrings.loadingQuestions(lang));

        ApiClient.getService().getQuestions().enqueue(new Callback<List<SurveyQuestion>>() {
            @Override
            public void onResponse(Call<List<SurveyQuestion>> call,
                                   Response<List<SurveyQuestion>> response) {
                if (response.isSuccessful()
                        && response.body() != null
                        && !response.body().isEmpty()) {

                    // Succès — les questions arrivent dans l'ordre de la BD (ORDER BY id)
                    retryCount = 0;
                    questions  = response.body();
                    int total  = questions.size();
                    ratings    = new int[total];
                    skipped    = new int[total];
                    progressBar.setMax(total * 100);

                    showSurveyState();
                    showQuestion(0);

                } else {
                    // Réponse invalide → retry
                    handleLoadFailure();
                }
            }

            @Override
            public void onFailure(Call<List<SurveyQuestion>> call, Throwable t) {
                // Pas de réseau ou serveur injoignable → retry
                handleLoadFailure();
            }
        });
    }

    /** Decide : encore des tentatives disponibles → countdown, sinon abandon. */
    private void handleLoadFailure() {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            scheduleRetry();
        } else {
            showError(LocalStrings.errorQuestions(lang));
        }
    }

    /**
     * Affiche le bouton retry + un compte à rebours de 10 s,
     * puis relance loadQuestionsFromApi() automatiquement.
     * Le bouton est visible dès le 1er échec.
     */
    private void scheduleRetry() {
        final int attempt = retryCount;
        cancelCountDown();
        progressQuestions.setVisibility(View.GONE);
        btnRetryQuestions.setVisibility(View.VISIBLE);
        btnRetryQuestions.requestFocus();

        countDown = new CountDownTimer(RETRY_INTERVAL_MS, 1_000) {
            @Override public void onTick(long millisUntilFinished) {
                tvLoadingMsg.setText(
                        LocalStrings.splashRetrying(attempt, MAX_RETRIES, millisUntilFinished / 1000)
                );
            }
            @Override public void onFinish() {
                loadQuestionsFromApi();
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
    // États visuels (loading / survey / error)
    // ────────────────────────────────────────────────────────────────────────

    private void showLoading(String message) {
        layoutLoading.setVisibility(View.VISIBLE);
        layoutSurvey.setVisibility(View.GONE);
        progressQuestions.setVisibility(View.VISIBLE);
        btnRetryQuestions.setVisibility(View.GONE);
        tvLoadingMsg.setText(message);
    }

    private void showError(String message) {
        cancelCountDown();
        layoutLoading.setVisibility(View.VISIBLE);
        layoutSurvey.setVisibility(View.GONE);
        progressQuestions.setVisibility(View.GONE);
        btnRetryQuestions.setVisibility(View.VISIBLE);
        tvLoadingMsg.setText(message);
        btnRetryQuestions.requestFocus();
    }

    private void showSurveyState() {
        layoutLoading.setVisibility(View.GONE);
        layoutSurvey.setVisibility(View.VISIBLE);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Affichage d'une question (ordre garanti par l'API)
    // ────────────────────────────────────────────────────────────────────────

    private void showQuestion(int index) {
        currentRating = ratings[index];
        int total = questions.size();
        SurveyQuestion q = questions.get(index);

        tvQuestionNumber.setText((index + 1) + " / " + total);
        progressBar.setProgress(index * 100);
        tvCategory.setText(q.getCategory(lang));
        tvQuestion.setText(q.getQuestion(lang));

        boolean isLast = (index == total - 1);
        tvHint.setText(isLast
                ? LocalStrings.hintSubmit(lang)
                : LocalStrings.hintNext(lang));

        updateStars();
    }

    private void updateStars() {
        for (int i = 0; i < 5; i++) {
            boolean filled = i < currentRating;
            stars[i].setText(filled ? "★" : "☆");
            stars[i].setTextColor(ContextCompat.getColor(this,
                    filled ? R.color.star_filled : R.color.star_empty));
        }
        if (currentRating == 0) {
            tvRatingLabel.setText(LocalStrings.ratingIgnore(lang));
            tvRatingLabel.setTextColor(ContextCompat.getColor(this, R.color.skip_color));
        } else {
            tvRatingLabel.setText(ratingLabels[currentRating]);
            tvRatingLabel.setTextColor(ContextCompat.getColor(this, R.color.gold));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Navigation télécommande
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);

        // Bloquer toutes les touches tant que le chargement est actif
        if (layoutLoading.getVisibility() == View.VISIBLE) return true;

        switch (event.getKeyCode()) {

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (currentRating < 5) { currentRating++; updateStars(); }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (currentRating > 0) { currentRating--; updateStars(); }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                confirmAndAdvance(0, 1);   // ignorer cette question
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                // Revenir à la question précédente (même comportement que BACK)
                if (currentIndex > 0) {
                    currentIndex--;
                    showQuestion(currentIndex);
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (currentRating == 0) {
                    tvRatingLabel.animate().alpha(0.2f).setDuration(120)
                            .withEndAction(() -> tvRatingLabel.animate().alpha(1f).setDuration(120));
                    return true;
                }
                confirmAndAdvance(currentRating, 0);
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (currentIndex > 0) {
                    currentIndex--;
                    showQuestion(currentIndex);
                } else {
                    Intent i = new Intent(this, WelcomeActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    i.putExtra(WelcomeActivity.EXTRA_GUEST, guest);
                    startActivity(i);
                    finish();
                }
                return true;
        }
        return super.dispatchKeyEvent(event);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Progression + soumission
    // ────────────────────────────────────────────────────────────────────────

    private void confirmAndAdvance(int rating, int skip) {
        ratings[currentIndex] = rating;
        skipped[currentIndex] = skip;

        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            showQuestion(currentIndex);
        } else {
            submitAndFinish();
        }
    }

    private void submitAndFinish() {
        // ── 1. Construire le résultat ─────────────────────────────────────────
        SurveyResult result  = new SurveyResult();
        result.device_id     = DeviceUtils.getDeviceId(this);
        result.ip_chambre    = (guest != null) ? guest.ip_chambre    : "";
        result.num_chambre   = (guest != null) ? guest.num_chambre   : "";
        result.nom_client    = (guest != null) ? guest.nom_client    : "";
        result.prenom_client = (guest != null) ? guest.prenom_client : "";
        result.langue        = lang;

        List<QuestionResult> reponses = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            reponses.add(new QuestionResult(
                    questions.get(i).id,
                    questions.get(i).category_id,
                    ratings[i],
                    skipped[i]
            ));
        }
        result.reponses = reponses;

        final Context appCtx = getApplicationContext();

        // ── 2. Effacer le cooldown "Plus tard" (sondage complété) ────────────
        LaterHelper.clear(appCtx);

        // ── 3. Sauvegarde locale AVANT toute navigation ───────────────────────
        // Garantit qu'aucune donnée n'est perdue si l'OS tue le process juste après.
        PendingResultHelper.save(appCtx, result);

        // ── 4. Navigation vers ThankYou (après la sauvegarde) ────────────────
        Intent intent = new Intent(this, ThankYouActivity.class);
        intent.putExtra(ThankYouActivity.EXTRA_GUEST, guest);
        startActivity(intent);
        finish();

        // ── 4. Envoi API en arrière-plan ──────────────────────────────────────
        ApiClient.getService().submitSurvey(result).enqueue(new Callback<SubmitResponse>() {
            @Override public void onResponse(Call<SubmitResponse> c, Response<SubmitResponse> r) {
                if (r.isSuccessful()) PendingResultHelper.clear(appCtx);
                // Si échec serveur (4xx/5xx) : résultat conservé → retry au prochain lancement
            }
            @Override public void onFailure(Call<SubmitResponse> c, Throwable t) {
                // Pas de réseau : résultat déjà sauvegardé, sera renvoyé au prochain lancement
            }
        });
    }
}
