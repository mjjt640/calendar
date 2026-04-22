package com.example.calendar.ui.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.data.backup.ScheduleBackupJsonConverter;
import com.example.calendar.data.local.db.AppDatabase;
import com.example.calendar.data.local.db.DatabaseProvider;
import com.example.calendar.data.repository.LocalScheduleBackupRepository;
import com.example.calendar.reminder.WorkManagerScheduleReminderCoordinator;

public class DataManagementViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public DataManagementViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        AppDatabase database = DatabaseProvider.getInstance(context);
        return (T) new DataManagementViewModel(
                new LocalScheduleBackupRepository(
                        database,
                        database.scheduleDao(),
                        database.recurrenceDao(),
                        new ScheduleBackupJsonConverter(),
                        new WorkManagerScheduleReminderCoordinator(context)
                )
        );
    }
}
