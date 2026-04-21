package com.example.calendar.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.calendar.common.constants.AppConstants;
import com.example.calendar.domain.model.ReminderSettings;

public class LocalReminderSettingsRepository implements ReminderSettingsRepository {
    private final SharedPreferences preferences;

    public LocalReminderSettingsRepository(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(
                AppConstants.PREFS_REMINDER_SETTINGS,
                Context.MODE_PRIVATE
        );
    }

    @Override
    public ReminderSettings getSettings() {
        ReminderSettings defaults = ReminderSettings.createDefault();
        return new ReminderSettings(
                preferences.getBoolean(AppConstants.KEY_REMINDERS_ENABLED, defaults.isRemindersEnabled()),
                preferences.getBoolean(AppConstants.KEY_REMINDER_SOUND_ENABLED, defaults.isSoundEnabled()),
                preferences.getBoolean(AppConstants.KEY_REMINDER_POPUP_ENABLED, defaults.isPopupEnabled()),
                preferences.getBoolean(AppConstants.KEY_REMINDER_DND_ENABLED, defaults.isDndEnabled()),
                preferences.getInt(AppConstants.KEY_REMINDER_DND_START_MINUTES, defaults.getDndStartMinutes()),
                preferences.getInt(AppConstants.KEY_REMINDER_DND_END_MINUTES, defaults.getDndEndMinutes()),
                preferences.getBoolean(
                        AppConstants.KEY_REMINDER_HIGH_PRIORITY_BYPASS,
                        defaults.isHighPriorityBypass()
                )
        );
    }

    @Override
    public void saveSettings(ReminderSettings settings) {
        preferences.edit()
                .putBoolean(AppConstants.KEY_REMINDERS_ENABLED, settings.isRemindersEnabled())
                .putBoolean(AppConstants.KEY_REMINDER_SOUND_ENABLED, settings.isSoundEnabled())
                .putBoolean(AppConstants.KEY_REMINDER_POPUP_ENABLED, settings.isPopupEnabled())
                .putBoolean(AppConstants.KEY_REMINDER_DND_ENABLED, settings.isDndEnabled())
                .putInt(AppConstants.KEY_REMINDER_DND_START_MINUTES, settings.getDndStartMinutes())
                .putInt(AppConstants.KEY_REMINDER_DND_END_MINUTES, settings.getDndEndMinutes())
                .putBoolean(AppConstants.KEY_REMINDER_HIGH_PRIORITY_BYPASS, settings.isHighPriorityBypass())
                .apply();
    }
}
