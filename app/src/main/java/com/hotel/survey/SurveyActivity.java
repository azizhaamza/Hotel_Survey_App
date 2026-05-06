package com.hotel.survey;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.hotel.survey.api.ApiClient;
import com.hotel.survey.model.Question;
import com.hotel.survey.model.QuestionResponse;
import com.hotel.survey.model.SurveyResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SurveyActivity extends AppCompatActivity {

    private static final String[] LABELS = {"", "Inadequate", "Average", "Good", "Very Good", "Excellent"};

    private List<Question> questions;
    private int currentIndex = 0;
    private int currentRating = 0;

    private TextView tvQuestionNumber;
    private TextView tvCategory;
    private TextView tvSubcategory;
    private TextView tvQuestion;
    private TextView tvRatingLabel;
    private TextView tvHint;
    private ProgressBar progressBar;
    private TextView[] stars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);
        initQuestions();
        initViews();
        showQuestion(0);
    }

    private void initQuestions() {
        questions = Arrays.asList(
            // Room & Reception
            new Question(1,  "YOUR ROOM",     null,              "How do you rate your room?"),
            new Question(2,  "FRONT DESK",    null,              "How do you rate the reception?"),

            // Breakfast
            new Question(3,  "LA TOPAZE",     "Breakfast",       "Quality of the breakfast buffet"),
            new Question(4,  "LA TOPAZE",     "Breakfast",       "Service"),

            // Restaurants
            new Question(5,  "IL DELFINO",    "Restaurant",      "Quality of meals"),
            new Question(6,  "IL DELFINO",    "Restaurant",      "Service"),
            new Question(7,  "LE GOURMET",    "Restaurant",      "Quality of meals"),
            new Question(8,  "LE GOURMET",    "Restaurant",      "Service"),
            new Question(9,  "L'OLIVIER",     "Restaurant",      "Quality of meals"),
            new Question(10, "L'OLIVIER",     "Restaurant",      "Service"),
            new Question(11, "LE VENUS",      "Restaurant",      "Quality of meals"),
            new Question(12, "LE VENUS",      "Restaurant",      "Service"),

            // Bars & Other
            new Question(13, "LA CASCADE",    null,              "How do you rate La Cascade?"),
            new Question(14, "LA BRISE",      null,              "How do you rate La Brise?"),
            new Question(15, "LOBBY BAR",     null,              "How do you rate the Lobby Bar?"),
            new Question(16, "ROOM SERVICE",  null,              "How do you rate the Room Service?"),

            // Spa & Services
            new Question(17, "SPA CENTER",    "Thalassotherapy", "Quality of welcome"),
            new Question(18, "SPA CENTER",    "Thalassotherapy", "Quality of treatments"),
            new Question(19, "LAUNDRY",       null,              "How do you rate the Laundry service?"),

            // Overall
            new Question(20, "OVERALL STAY",  null,              "How do you rate your overall stay?")
        );
    }

    private void initViews() {
        tvQuestionNumber = findViewById(R.id.tv_question_number);
        tvCategory       = findViewById(R.id.tv_category);
        tvSubcategory    = findViewById(R.id.tv_subcategory);
        tvQuestion       = findViewById(R.id.tv_question);
        tvRatingLabel    = findViewById(R.id.tv_rating_label);
        tvHint           = findViewById(R.id.tv_hint);
        progressBar      = findViewById(R.id.progress_bar);

        stars = new TextView[]{
            findViewById(R.id.star1), findViewById(R.id.star2), findViewById(R.id.star3),
            findViewById(R.id.star4), findViewById(R.id.star5)
        };
    }

    private void showQuestion(int index) {
        Question q = questions.get(index);
        currentRating = q.getRating();

        tvQuestionNumber.setText((index + 1) + " / " + questions.size());
        progressBar.setMax(questions.size());
        progressBar.setProgress(index + 1);

        tvCategory.setText(q.getCategory());

        if (q.getSubcategory() != null) {
            tvSubcategory.setText(q.getSubcategory());
            tvSubcategory.setVisibility(View.VISIBLE);
        } else {
            tvSubcategory.setVisibility(View.GONE);
        }

        tvQuestion.setText(q.getText());

        boolean isLast = (index == questions.size() - 1);
        String action = isLast ? "OK → submit" : "OK → next";
        tvHint.setText("◄ ► select rating   |   " + action + "   |   ↑ skip");

        updateStars();
    }

    private void updateStars() {
        for (int i = 0; i < 5; i++) {
            boolean filled = i < currentRating;
            stars[i].setText(filled ? "★" : "☆");
            stars[i].setTextColor(getResources().getColor(
                filled ? R.color.star_filled : R.color.star_empty, null));
        }
        if (currentRating == 0) {
            tvRatingLabel.setText("Not yet rated — press ↑ to skip");
            tvRatingLabel.setTextColor(getResources().getColor(R.color.skip_color, null));
        } else {
            tvRatingLabel.setText(LABELS[currentRating]);
            tvRatingLabel.setTextColor(getResources().getColor(R.color.gold, null));
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
                // Skip this question (not applicable)
                saveAndAdvance(0);
                return true;

            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (currentRating == 0) {
                    // Pulse the hint to remind user to rate or skip
                    tvRatingLabel.animate().alpha(0.2f).setDuration(150)
                        .withEndAction(() -> tvRatingLabel.animate().alpha(1f).setDuration(150));
                    return true;
                }
                saveAndAdvance(currentRating);
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (currentIndex > 0) {
                    questions.get(currentIndex).setRating(currentRating);
                    currentIndex--;
                    showQuestion(currentIndex);
                    return true;
                }
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    private void saveAndAdvance(int rating) {
        questions.get(currentIndex).setRating(rating);
        if (currentIndex < questions.size() - 1) {
            currentIndex++;
            showQuestion(currentIndex);
        } else {
            submitAndFinish();
        }
    }

    private void submitAndFinish() {
        startActivity(new Intent(this, ThankYouActivity.class));
        finish();

        List<QuestionResponse> responses = new ArrayList<>();
        for (Question q : questions) {
            responses.add(new QuestionResponse(
                q.getId(), q.getCategory(), q.getSubcategory(),
                q.getText(), q.getRating(), q.isSkipped()
            ));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        SurveyResult result = new SurveyResult(sdf.format(new Date()), deviceId, responses);
        ApiClient.getService().submitSurvey(result).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> r) { }
            @Override public void onFailure(Call<Void> call, Throwable t) { }
        });
    }
}
