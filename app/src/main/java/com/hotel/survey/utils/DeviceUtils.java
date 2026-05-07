package com.hotel.survey.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

public class DeviceUtils {

    private static final String PREFS = "survey_prefs";
    private static final String KEY_DEVICE_ID = "device_id";

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = prefs.getString(KEY_DEVICE_ID, null);
        if (id == null) {
            id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (id == null || id.isEmpty()) id = "unknown";
            prefs.edit().putString(KEY_DEVICE_ID, id).apply();
        }
        return id;
    }
}
