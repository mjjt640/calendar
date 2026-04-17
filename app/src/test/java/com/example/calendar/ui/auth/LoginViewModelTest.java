package com.example.calendar.ui.auth;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.calendar.data.repository.AuthRepository;
import com.example.calendar.data.repository.ProfileRepository;
import com.example.calendar.domain.model.AuthSession;
import com.example.calendar.domain.model.UserProfile;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LoginViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void login_withBlankAccount_setsValidationError() {
        LoginViewModel viewModel = new LoginViewModel(
                new FakeAuthRepository(),
                new FakeProfileRepository()
        );

        viewModel.login("   ", "123456");

        assertEquals("请输入账号", viewModel.getAccountError().getValue());
        assertNull(viewModel.getDestination().getValue());
    }

    @Test
    public void login_withValidCredentials_routesToProfileWhenOnboardingMissing() {
        FakeProfileRepository profileRepository = new FakeProfileRepository();
        LoginViewModel viewModel = new LoginViewModel(new FakeAuthRepository(), profileRepository);

        viewModel.login("demo_user", "123456");

        assertEquals(LoginViewModel.LoginDestination.PROFILE, viewModel.getDestination().getValue());
        assertEquals("demo_user", profileRepository.profile.getAccount());
    }

    @Test
    public void login_withHandledProfile_routesToHome() {
        FakeProfileRepository profileRepository = new FakeProfileRepository();
        profileRepository.profile = new UserProfile(
                "demo_user", "", "用户1234", "", "", "", "暂未留下签名", false, true, false
        );
        LoginViewModel viewModel = new LoginViewModel(new FakeAuthRepository(), profileRepository);

        viewModel.login("demo_user", "123456");

        assertEquals(LoginViewModel.LoginDestination.HOME, viewModel.getDestination().getValue());
    }

    private static class FakeAuthRepository implements AuthRepository {
        @Override
        public AuthSession login(String account, String password) {
            return new AuthSession("token", "refresh", true);
        }

        @Override
        public AuthSession getSession() {
            return new AuthSession("", "", false);
        }

        @Override
        public void logout() {
        }
    }

    private static class FakeProfileRepository implements ProfileRepository {
        private UserProfile profile = new UserProfile("", "", "", "", "", "", "", false, false, false);

        @Override
        public UserProfile getProfile() {
            return profile;
        }

        @Override
        public void saveProfile(UserProfile userProfile) {
            profile = userProfile;
        }

        @Override
        public UserProfile createSkippedProfile(String account) {
            return new UserProfile(account, "", "用户5678", "", "", "", "暂未留下签名", false, true, false);
        }

        @Override
        public void clearProfile() {
            profile = new UserProfile("", "", "", "", "", "", "", false, false, false);
        }
    }
}
