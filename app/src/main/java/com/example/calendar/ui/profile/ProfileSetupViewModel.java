package com.example.calendar.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.common.constants.AppConstants;
import com.example.calendar.data.repository.ProfileRepository;
import com.example.calendar.domain.model.UserProfile;

public class ProfileSetupViewModel extends ViewModel {
    private final ProfileRepository profileRepository;
    private final MutableLiveData<Boolean> finished = new MutableLiveData<>(false);
    private final MutableLiveData<String> nicknameValue = new MutableLiveData<>("");
    private final String account;

    public ProfileSetupViewModel(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
        UserProfile profile = profileRepository.getProfile();
        this.account = profile == null ? "" : profile.getAccount();
        if (profile != null && !profile.getNickname().isEmpty()) {
            nicknameValue.setValue(profile.getNickname());
        }
    }

    public LiveData<Boolean> getFinished() {
        return finished;
    }

    public LiveData<String> getNicknameValue() {
        return nicknameValue;
    }

    public void saveProfile(String nickname, String gender, String birthday, String city, String signature) {
        String finalNickname = clean(nickname);
        if (finalNickname.isEmpty()) {
            finalNickname = profileRepository.createSkippedProfile(account).getNickname();
        }
        String finalSignature = clean(signature);
        if (finalSignature.isEmpty()) {
            finalSignature = AppConstants.DEFAULT_SIGNATURE;
        }
        profileRepository.saveProfile(new UserProfile(
                account,
                "",
                finalNickname,
                clean(gender),
                clean(birthday),
                clean(city),
                finalSignature,
                true,
                true,
                false
        ));
        finished.setValue(true);
    }

    public void skipProfile() {
        profileRepository.saveProfile(profileRepository.createSkippedProfile(account));
        finished.setValue(true);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
