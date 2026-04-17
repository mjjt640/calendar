package com.example.calendar.ui.profile;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.calendar.data.repository.ProfileRepository;
import com.example.calendar.domain.model.UserProfile;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfileSetupViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void skipProfile_savesGeneratedDefaults() {
        FakeProfileRepository repository = new FakeProfileRepository();
        ProfileSetupViewModel viewModel = new ProfileSetupViewModel(repository);

        viewModel.skipProfile();

        assertEquals("用户4827", repository.profile.getNickname());
        assertEquals("暂未留下签名", repository.profile.getSignature());
        assertTrue(repository.profile.isOnboardingHandled());
    }

    @Test
    public void saveProfile_withBlankSignature_usesDefaultSignature() {
        FakeProfileRepository repository = new FakeProfileRepository();
        ProfileSetupViewModel viewModel = new ProfileSetupViewModel(repository);

        viewModel.saveProfile("阿远", "男", "1999-10-01", "杭州", " ");

        assertEquals("阿远", repository.profile.getNickname());
        assertEquals("暂未留下签名", repository.profile.getSignature());
        assertTrue(repository.profile.isProfileCompleted());
    }

    private static class FakeProfileRepository implements ProfileRepository {
        private UserProfile profile = new UserProfile("demo_user", "", "", "", "", "", "", false, false, false);

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
            return new UserProfile(account, "", "用户4827", "", "", "", "暂未留下签名", false, true, false);
        }

        @Override
        public void clearProfile() {
            profile = new UserProfile("", "", "", "", "", "", "", false, false, false);
        }
    }
}
