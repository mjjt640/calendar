package com.example.calendar.ui.profile;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.data.repository.LocalProfileRepository;

public class ProfileSetupViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public ProfileSetupViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ProfileSetupViewModel(new LocalProfileRepository(context));
    }
}
