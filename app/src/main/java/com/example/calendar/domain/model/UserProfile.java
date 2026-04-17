package com.example.calendar.domain.model;

public class UserProfile {
    private final String account;
    private final String phone;
    private final String nickname;
    private final String gender;
    private final String birthday;
    private final String city;
    private final String signature;
    private final boolean profileCompleted;
    private final boolean onboardingHandled;
    private final boolean phoneBound;

    public UserProfile(String account, String phone, String nickname, String gender, String birthday,
                       String city, String signature, boolean profileCompleted,
                       boolean onboardingHandled, boolean phoneBound) {
        this.account = account;
        this.phone = phone;
        this.nickname = nickname;
        this.gender = gender;
        this.birthday = birthday;
        this.city = city;
        this.signature = signature;
        this.profileCompleted = profileCompleted;
        this.onboardingHandled = onboardingHandled;
        this.phoneBound = phoneBound;
    }

    public String getAccount() {
        return account;
    }

    public String getPhone() {
        return phone;
    }

    public String getNickname() {
        return nickname;
    }

    public String getGender() {
        return gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public String getCity() {
        return city;
    }

    public String getSignature() {
        return signature;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public boolean isOnboardingHandled() {
        return onboardingHandled;
    }

    public boolean isPhoneBound() {
        return phoneBound;
    }
}
