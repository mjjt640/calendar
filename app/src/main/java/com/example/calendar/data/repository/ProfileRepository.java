package com.example.calendar.data.repository;

import com.example.calendar.domain.model.UserProfile;

public interface ProfileRepository {
    UserProfile getProfile();

    void saveProfile(UserProfile userProfile);

    UserProfile createSkippedProfile(String account);
}
