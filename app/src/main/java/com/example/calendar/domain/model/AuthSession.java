package com.example.calendar.domain.model;

public class AuthSession {
    private final String accessToken;
    private final String refreshToken;
    private final boolean loggedIn;

    public AuthSession(String accessToken, String refreshToken, boolean loggedIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.loggedIn = loggedIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
}
