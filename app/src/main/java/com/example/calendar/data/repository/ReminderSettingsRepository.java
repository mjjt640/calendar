package com.example.calendar.data.repository;

import com.example.calendar.domain.model.ReminderSettings;

public interface ReminderSettingsRepository {
    ReminderSettings getSettings();

    void saveSettings(ReminderSettings settings);
}
