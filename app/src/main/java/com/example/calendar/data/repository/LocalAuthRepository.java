package com.example.calendar.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.calendar.common.constants.AppConstants;
import com.example.calendar.domain.model.AuthSession;

public class LocalAuthRepository implements AuthRepository {
    private final SharedPreferences preferences;

    public LocalAuthRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(AppConstants.PREFS_AUTH, Context.MODE_PRIVATE);
    }

    @Override
    public AuthSession login(String account, String password) {
        String accessToken = "access_" + account + "_" + System.currentTimeMillis();
        String refreshToken = "refresh_" + account + "_" + System.currentTimeMillis();
        preferences.edit()
                .putString(AppConstants.KEY_ACCESS_TOKEN, accessToken)
                .putString(AppConstants.KEY_REFRESH_TOKEN, refreshToken)
                .putBoolean(AppConstants.KEY_LOGGED_IN, true)
                .apply();
        return new AuthSession(accessToken, refreshToken, true);
    }

    @Override
    public AuthSession getSession() {
        return new AuthSession(
                preferences.getString(AppConstants.KEY_ACCESS_TOKEN, ""),
                preferences.getString(AppConstants.KEY_REFRESH_TOKEN, ""),
                preferences.getBoolean(AppConstants.KEY_LOGGED_IN, false)
        );
    }

    @Override
    public void logout() {
        preferences.edit().clear().apply();
    }
}
