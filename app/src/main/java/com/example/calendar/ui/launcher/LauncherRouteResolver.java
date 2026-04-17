package com.example.calendar.ui.launcher;

import com.example.calendar.domain.model.AuthSession;
import com.example.calendar.domain.model.UserProfile;

public class LauncherRouteResolver {
    public LauncherRoute resolve(AuthSession session, UserProfile profile) {
        if (session == null || !session.isLoggedIn()) {
            return LauncherRoute.LOGIN;
        }
        if (profile == null || !profile.isOnboardingHandled()) {
            return LauncherRoute.PROFILE_SETUP;
        }
        return LauncherRoute.HOME;
    }
}
