package com.example.calendar.ui.auth;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.data.repository.LocalAuthRepository;
import com.example.calendar.data.repository.LocalProfileRepository;

public class LoginViewModelFactory implements ViewModelProvider.Factory {
    private final Context context;

    public LoginViewModelFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new LoginViewModel(
                new LocalAuthRepository(context),
                new LocalProfileRepository(context)
        );
    }
}
