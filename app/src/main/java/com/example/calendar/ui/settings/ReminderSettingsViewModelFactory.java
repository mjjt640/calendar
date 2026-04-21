package com.example.calendar.ui.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.data.repository.LocalReminderSettingsRepository;
import com.example.calendar.reminder.WorkManagerScheduleReminderCoordinator;

public class ReminderSettingsViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public ReminderSettingsViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ReminderSettingsViewModel(
                new LocalReminderSettingsRepository(context),
                new WorkManagerScheduleReminderCoordinator(context)
        );
    }
}
