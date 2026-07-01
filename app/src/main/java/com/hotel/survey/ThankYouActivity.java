package com.hotel.survey;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hotel.survey.model.GuestInfo;
import com.hotel.survey.utils.AppConfig;
import com.hotel.survey.utils.LocalStrings;

public class ThankYouActivity extends AppCompatActivity {

    public static final String EXTRA_GUEST   = "extra_guest";
    private static final int   AUTO_CLOSE_MS = AppConfig.THANK_YOU_AUTO_CLOSE_MS;

    private CountDownTimer countDown;
    private String         lang = "FR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thank_you);

        GuestInfo guest = (GuestInfo) getIntent().getSerializableExtra(EXTRA_GUEST);
        lang = (guest != null) ? guest.getLang() : "FR";

        // RTL pour l'arabe
        if (LocalStrings.isRtl(lang)) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        // Textes localisés
        TextView tvTitle  = findViewById(R.id.tv_thanks_title);
        TextView tvSlogan = findViewById(R.id.tv_thanks_slogan);
        TextView tvBody   = findViewById(R.id.tv_thanks_body);
        TextView tvTimer  = findViewById(R.id.tv_thanks_timer);
        TextView tvGuest  = findViewById(R.id.tv_guest_name);

        tvTitle.setText(LocalStrings.thankYouTitle(lang));
        tvSlogan.setText(LocalStrings.thankYouSlogan(lang));
        tvBody.setText(LocalStrings.thankYouBody(lang));

        // Nom du client (optionnel)
        if (guest != null && guest.found) {
            String name = guest.getDisplayName();
            String room = (guest.num_chambre != null) ? guest.num_chambre : "";
            if (!name.isEmpty() || !room.isEmpty()) {
                String label = name.isEmpty() ? "Room " + room : name + "  —  Room " + room;
                tvGuest.setText(label);
                tvGuest.setVisibility(View.VISIBLE);
            }
        }

        // Compte à rebours dynamique
        countDown = new CountDownTimer(AUTO_CLOSE_MS, 1_000) {
            @Override public void onTick(long millisUntilFinished) {
                int secs = (int) (millisUntilFinished / 1000) + 1;
                tvTimer.setText(LocalStrings.thankYouTimer(lang, secs));
            }
            @Override public void onFinish() {
                tvTimer.setText(LocalStrings.thankYouTimer(lang, 0));
                closeApp();
            }
        }.start();
    }

    private void closeApp() {
        if (countDown != null) { countDown.cancel(); countDown = null; }
        finishAffinity();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_BACK
                    || code == KeyEvent.KEYCODE_DPAD_CENTER
                    || code == KeyEvent.KEYCODE_ENTER) {
                closeApp();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDown != null) { countDown.cancel(); countDown = null; }
    }
}
