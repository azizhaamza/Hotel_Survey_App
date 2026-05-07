package com.hotel.survey;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hotel.survey.api.ApiClient;
import com.hotel.survey.api.SubmitResponse;
import com.hotel.survey.model.GuestInfo;
import com.hotel.survey.model.SurveyResult;
import com.hotel.survey.utils.DeviceUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SurveyActivity extends AppCompatActivity {

    public static final String EXTRA_GUEST = "extra_guest";

    private static final String[] RATING_LABELS =
            {"", "Inadequate", "Average", "Good", "Very Good", "Excellent"};

    // 5 hardcoded questions per CDC v0.4
    private static final String[] CATEGORIES =
            {"YOUR ROOM", "FRONT DESK", "BREAKFAST", "SPA CENTER", "OVERALL STAY"};
    private static final String[] QUESTIONS = {
            "How do you rate your room?",
            "How do you rate the reception?",
            "How do you rate the breakfast?",
            "How do you rate the Spa Center?",
            "How do you rate your overall stay?"
    };
    private static final int TOTAL = 5;

    private int currentIndex = 0;
    private int currentRating = 0;
    private final int[] ratings  = new int[TOTAL]; // 0 = not rated
    private final int[] skipped  = new int[TOTAL]; // 1 = skipped

    private GuestInfo guest;

    private TextView tvGuestHeader;
    private TextView tvQuestionNumber;
    private ProgressBar progressBar;
    private TextView tvCategory;
    private TextView tvQuestion;
    private TextView tvRatingLabel;
    private TextView tvHint;
    private TextView[] stars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        guest = (GuestInfo) getIntent().getSerializableExtra(EXTRA_GUEST);

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

        if (guest != null && guest.found) {
            String name = guest.getDisplayName();
            String room = guest.num_chambre != null ? guest.num_chambre : "";
            String header = name.isEmpty() ? "Room " + room : name + "  —  Room " + room;
            tvGuestHeader.setText(header);
        }

        progressBar.setMax(TOTAL * 100);
        showQuestion(0);
    }

    private void showQuestion(int index) {
        //currentRating = 0;
        currentRating = ratings[index];

        tvQuestionNumber.setText((index + 1) + " / " + TOTAL);
        progressBar.setProgress((index) * 100);
        tvCategory.setText(CATEGORIES[index]);
        tvQuestion.setText(QUESTIONS[index]);

        boolean isLast = (index == TOTAL - 1);
        tvHint.setText("◄ ► select rating   |   " + (isLast ? "OK → submit" : "OK → next") + "   |   ↑ skip");

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
            tvRatingLabel.setText("Press ↑ to skip");
            tvRatingLabel.setTextColor(ContextCompat.getColor(this, R.color.skip_color));
        } else {
            tvRatingLabel.setText(RATING_LABELS[currentRating]);
            tvRatingLabel.setTextColor(ContextCompat.getColor(this, R.color.gold));
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);

        switch (event.getKeyCode()) {

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (currentRating < 5) { currentRating++; updateStars(); }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (currentRating > 0) { currentRating--; updateStars(); }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                confirmAndAdvance(0, 1);
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (currentRating == 0) {
                    // Pulse hint to prompt user
                    tvRatingLabel.animate().alpha(0.2f).setDuration(120)
                        .withEndAction(() -> tvRatingLabel.animate().alpha(1f).setDuration(120));
                    return true;
                }
                confirmAndAdvance(currentRating, 0);
                return true;

            /*case KeyEvent.KEYCODE_BACK:
                // Back navigation disabled in v0.4
                return true;*/
            case KeyEvent.KEYCODE_BACK:
                if (currentIndex > 0) {
                    // Reculer d'une question
                    currentIndex--;
                    showQuestion(currentIndex);

                    // Optionnel : Restaurer la note précédemment saisie
                    currentRating = ratings[currentIndex];
                    updateStars();
                } else {
                    // On est à la première question : on retourne explicitement au Welcome
                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish(); // On ferme SurveyActivity
                }
                return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void confirmAndAdvance(int rating, int skip) {
        ratings[currentIndex] = rating;
        skipped[currentIndex] = skip;

        if (currentIndex < TOTAL - 1) {
            currentIndex++;
            showQuestion(currentIndex);
        } else {
            submitAndFinish();
        }
    }

    private void submitAndFinish() {
        Intent intent = new Intent(this, ThankYouActivity.class);
        intent.putExtra(ThankYouActivity.EXTRA_GUEST, guest);
        startActivity(intent);
        finish();

        // Build flat result and submit in background
        SurveyResult result = new SurveyResult();
        result.device_id     = DeviceUtils.getDeviceId(this);
        result.ip_chambre    = (guest != null) ? guest.ip_chambre    : "";
        result.num_chambre   = (guest != null) ? guest.num_chambre   : "";
        result.nom_client    = (guest != null) ? guest.nom_client    : "";
        result.prenom_client = (guest != null) ? guest.prenom_client : "";

        result.q1_room_rating  = ratings[0]; result.q1_room_skipped  = skipped[0];
        result.q2_fd_rating    = ratings[1]; result.q2_fd_skipped    = skipped[1];
        result.q3_bk_rating    = ratings[2]; result.q3_bk_skipped    = skipped[2];
        result.q4_spa_rating   = ratings[3]; result.q4_spa_skipped   = skipped[3];
        result.q5_ov_rating    = ratings[4]; result.q5_ov_skipped    = skipped[4];

        ApiClient.getService().submitSurvey(result).enqueue(new Callback<SubmitResponse>() {
            @Override public void onResponse(Call<SubmitResponse> call, Response<SubmitResponse> r) {}
            @Override public void onFailure(Call<SubmitResponse> call, Throwable t) {}
        });
    }
}
