package com.example.calendar.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.calendar.common.constants.AppConstants;
import com.example.calendar.domain.model.UserProfile;

import java.util.Random;

public class LocalProfileRepository implements ProfileRepository {
    private final SharedPreferences preferences;
    private final Random random;

    public LocalProfileRepository(Context context) {
        this(context, new Random());
    }

    LocalProfileRepository(Context context, Random random) {
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(AppConstants.PREFS_PROFILE, Context.MODE_PRIVATE);
        this.random = random;
    }

    @Override
    public UserProfile getProfile() {
        return new UserProfile(
                preferences.getString(AppConstants.KEY_ACCOUNT, ""),
                preferences.getString(AppConstants.KEY_PHONE, ""),
                preferences.getString(AppConstants.KEY_NICKNAME, ""),
                preferences.getString(AppConstants.KEY_GENDER, ""),
                preferences.getString(AppConstants.KEY_BIRTHDAY, ""),
                preferences.getString(AppConstants.KEY_CITY, ""),
                preferences.getString(AppConstants.KEY_SIGNATURE, ""),
                preferences.getBoolean(AppConstants.KEY_PROFILE_COMPLETED, false),
                preferences.getBoolean(AppConstants.KEY_ONBOARDING_HANDLED, false),
                preferences.getBoolean(AppConstants.KEY_PHONE_BOUND, false)
        );
    }

    @Override
    public void saveProfile(UserProfile userProfile) {
        preferences.edit()
                .putString(AppConstants.KEY_ACCOUNT, userProfile.getAccount())
                .putString(AppConstants.KEY_PHONE, userProfile.getPhone())
                .putString(AppConstants.KEY_NICKNAME, userProfile.getNickname())
                .putString(AppConstants.KEY_GENDER, userProfile.getGender())
                .putString(AppConstants.KEY_BIRTHDAY, userProfile.getBirthday())
                .putString(AppConstants.KEY_CITY, userProfile.getCity())
                .putString(AppConstants.KEY_SIGNATURE, userProfile.getSignature())
                .putBoolean(AppConstants.KEY_PROFILE_COMPLETED, userProfile.isProfileCompleted())
                .putBoolean(AppConstants.KEY_ONBOARDING_HANDLED, userProfile.isOnboardingHandled())
                .putBoolean(AppConstants.KEY_PHONE_BOUND, userProfile.isPhoneBound())
                .apply();
    }

    @Override
    public UserProfile createSkippedProfile(String account) {
        int suffix = 1000 + random.nextInt(9000);
        return new UserProfile(
                account,
                "",
                "用户" + suffix,
                "",
                "",
                "",
                AppConstants.DEFAULT_SIGNATURE,
                false,
                true,
                false
        );
    }
}
