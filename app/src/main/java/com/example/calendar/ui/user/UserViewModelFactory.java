package com.example.calendar.ui.user;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.data.repository.LocalAuthRepository;
import com.example.calendar.data.repository.LocalProfileRepository;

public class UserViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public UserViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new UserViewModel(
                new LocalAuthRepository(context),
                new LocalProfileRepository(context)
        );
    }
}
