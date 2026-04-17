package com.example.calendar.ui.user;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.data.repository.AuthRepository;
import com.example.calendar.data.repository.ProfileRepository;
import com.example.calendar.domain.model.UserProfile;

public class UserViewModel extends ViewModel {
    private final AuthRepository authRepository;
    private final ProfileRepository profileRepository;
    private final MutableLiveData<UserProfile> profileLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutState = new MutableLiveData<>(false);

    public UserViewModel(AuthRepository authRepository, ProfileRepository profileRepository) {
        this.authRepository = authRepository;
        this.profileRepository = profileRepository;
    }

    public LiveData<UserProfile> getProfileLiveData() {
        return profileLiveData;
    }

    public LiveData<Boolean> getLogoutState() {
        return logoutState;
    }

    public void loadProfile() {
        profileLiveData.setValue(profileRepository.getProfile());
    }

    public void logout() {
        authRepository.logout();
        profileRepository.clearProfile();
        logoutState.setValue(true);
    }
}
