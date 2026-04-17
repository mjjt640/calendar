package com.example.calendar.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.data.repository.AuthRepository;
import com.example.calendar.data.repository.ProfileRepository;
import com.example.calendar.domain.model.AuthSession;
import com.example.calendar.domain.model.UserProfile;

public class LoginViewModel extends ViewModel {
    public enum LoginDestination {
        PROFILE,
        HOME
    }

    private final AuthRepository authRepository;
    private final ProfileRepository profileRepository;
    private final MutableLiveData<String> accountError = new MutableLiveData<>();
    private final MutableLiveData<String> passwordError = new MutableLiveData<>();
    private final MutableLiveData<LoginDestination> destination = new MutableLiveData<>();

    public LoginViewModel(AuthRepository authRepository, ProfileRepository profileRepository) {
        this.authRepository = authRepository;
        this.profileRepository = profileRepository;
    }

    public LiveData<String> getAccountError() {
        return accountError;
    }

    public LiveData<String> getPasswordError() {
        return passwordError;
    }

    public LiveData<LoginDestination> getDestination() {
        return destination;
    }

    public void login(String account, String password) {
        String trimmedAccount = account == null ? "" : account.trim();
        String trimmedPassword = password == null ? "" : password.trim();

        boolean valid = true;
        if (trimmedAccount.isEmpty()) {
            accountError.setValue("请输入账号");
            valid = false;
        } else {
            accountError.setValue(null);
        }
        if (trimmedPassword.isEmpty()) {
            passwordError.setValue("请输入密码");
            valid = false;
        } else {
            passwordError.setValue(null);
        }
        if (!valid) {
            return;
        }

        AuthSession session = authRepository.login(trimmedAccount, trimmedPassword);
        if (session == null || !session.isLoggedIn()) {
            passwordError.setValue("账号或密码错误");
            return;
        }

        UserProfile currentProfile = profileRepository.getProfile();
        if (currentProfile == null || currentProfile.getAccount().isEmpty()) {
            profileRepository.saveProfile(new UserProfile(
                    trimmedAccount, "", "", "", "", "", "", false, false, false
            ));
            destination.setValue(LoginDestination.PROFILE);
            return;
        }

        destination.setValue(currentProfile.isOnboardingHandled()
                ? LoginDestination.HOME
                : LoginDestination.PROFILE);
    }
}
