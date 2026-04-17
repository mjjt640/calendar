package com.example.calendar.ui.launcher;

import com.example.calendar.domain.model.AuthSession;
import com.example.calendar.domain.model.UserProfile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LauncherRouteResolverTest {
    private final LauncherRouteResolver resolver = new LauncherRouteResolver();

    @Test
    public void resolve_returnsLoginWhenNotLoggedIn() {
        LauncherRoute route = resolver.resolve(
                new AuthSession("", "", false),
                new UserProfile("", "", "", "", "", "", "", false, false, false)
        );

        assertEquals(LauncherRoute.LOGIN, route);
    }

    @Test
    public void resolve_returnsProfileWhenOnboardingNotHandled() {
        LauncherRoute route = resolver.resolve(
                new AuthSession("token", "refresh", true),
                new UserProfile("demo", "", "", "", "", "", "", false, false, false)
        );

        assertEquals(LauncherRoute.PROFILE_SETUP, route);
    }

    @Test
    public void resolve_returnsHomeWhenOnboardingHandled() {
        LauncherRoute route = resolver.resolve(
                new AuthSession("token", "refresh", true),
                new UserProfile("demo", "", "用户1234", "", "", "", "暂未留下签名", false, true, false)
        );

        assertEquals(LauncherRoute.HOME, route);
    }
}
