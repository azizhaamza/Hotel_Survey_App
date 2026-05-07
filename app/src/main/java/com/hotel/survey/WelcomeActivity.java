package com.hotel.survey;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hotel.survey.model.GuestInfo;

public class WelcomeActivity extends AppCompatActivity {

    public static final String EXTRA_GUEST = "extra_guest";

    private Button btnStartNow;
    private Button btnMaybeLater;
    private Button btnNever;
    private GuestInfo guest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        guest = (GuestInfo) getIntent().getSerializableExtra(EXTRA_GUEST);

        TextView tvGuestName  = findViewById(R.id.tv_guest_name);
        TextView tvRoomNumber = findViewById(R.id.tv_room_number);
        TextView tvDeviceIp   = findViewById(R.id.tv_device_ip);
        btnStartNow   = findViewById(R.id.btn_start_now);
        btnMaybeLater = findViewById(R.id.btn_maybe_later);
        btnNever      = findViewById(R.id.btn_never);

        if (guest != null) {
            // Always show the detected IP (bottom of left panel) for admin verification
            String ip = (guest.ip_chambre != null) ? guest.ip_chambre : "";
            if (!ip.isEmpty()) {
                tvDeviceIp.setText("IP: " + ip);
            }

            if (guest.found) {
                String name = guest.getDisplayName();
                String room = (guest.num_chambre != null && !guest.num_chambre.isEmpty())
                        ? guest.num_chambre : "";

                if (!name.isEmpty()) {
                    tvGuestName.setText(name);
                    tvGuestName.setVisibility(View.VISIBLE);
                }
                if (!room.isEmpty()) {
                    tvRoomNumber.setText("Room  " + room);
                    tvRoomNumber.setVisibility(View.VISIBLE);
                }
            }
        }

        btnStartNow.setOnClickListener(v -> startSurvey());
        btnMaybeLater.setOnClickListener(v -> finishAffinity());
        btnNever.setOnClickListener(v -> finishAffinity());

        btnStartNow.requestFocus();
    }

    private void startSurvey() {
        Intent intent = new Intent(this, SurveyActivity.class);
        intent.putExtra(SurveyActivity.EXTRA_GUEST, guest);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);

        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: {
                View focused = getCurrentFocus();
                if (focused == btnStartNow)   { startSurvey();    return true; }
                if (focused == btnMaybeLater) { finishAffinity(); return true; }
                if (focused == btnNever)      { finishAffinity(); return true; }
                break;
            }
            case KeyEvent.KEYCODE_BACK:
                finishAffinity();
                return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
