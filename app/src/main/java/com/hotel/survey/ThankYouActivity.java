package com.hotel.survey;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hotel.survey.model.GuestInfo;

public class ThankYouActivity extends AppCompatActivity {

    public static final String EXTRA_GUEST = "extra_guest";

    private static final int AUTO_CLOSE_MS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable closeAction = this::closeApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thank_you);

        GuestInfo guest = (GuestInfo) getIntent().getSerializableExtra(EXTRA_GUEST);
        TextView tvGuestName = findViewById(R.id.tv_guest_name);

        if (guest != null && guest.found) {
            String name = guest.getDisplayName();
            String room = guest.num_chambre != null ? guest.num_chambre : "";
            if (!name.isEmpty() || !room.isEmpty()) {
                String label = name.isEmpty() ? "Room " + room : name + "  —  Room " + room;
                tvGuestName.setText(label);
                tvGuestName.setVisibility(View.VISIBLE);
            }
        }

        handler.postDelayed(closeAction, AUTO_CLOSE_MS);
    }

    private void closeApp() {
        finishAffinity();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int code = event.getKeyCode();
            if (code == KeyEvent.KEYCODE_BACK ||
                code == KeyEvent.KEYCODE_DPAD_CENTER ||
                code == KeyEvent.KEYCODE_ENTER) {
                handler.removeCallbacks(closeAction);
                closeApp();
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(closeAction);
    }
}
