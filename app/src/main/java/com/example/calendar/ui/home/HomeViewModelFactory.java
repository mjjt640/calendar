package com.example.calendar.ui.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.data.local.db.DatabaseProvider;
import com.example.calendar.data.repository.LocalScheduleRepository;

public class HomeViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public HomeViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new HomeViewModel(
                new LocalScheduleRepository(DatabaseProvider.getInstance(context).scheduleDao())
        );
    }
}
