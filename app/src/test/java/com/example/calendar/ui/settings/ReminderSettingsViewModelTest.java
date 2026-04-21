package com.example.calendar.ui.settings;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.calendar.data.repository.ReminderSettingsRepository;
import com.example.calendar.domain.model.ReminderSettings;
import com.example.calendar.reminder.ScheduleReminderCoordinator;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReminderSettingsViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void init_readsDefaultSettings() {
        FakeReminderSettingsRepository repository = new FakeReminderSettingsRepository();
        ReminderSettingsViewModel viewModel =
                new ReminderSettingsViewModel(repository, new FakeScheduleReminderCoordinator());

        assertTrue(Boolean.TRUE.equals(viewModel.getRemindersEnabled().getValue()));
        assertTrue(Boolean.TRUE.equals(viewModel.getSoundEnabled().getValue()));
        assertEquals("22:00", viewModel.getDndStartText().getValue());
        assertEquals("08:00", viewModel.getDndEndText().getValue());
    }

    @Test
    public void saveSettings_whenEnabled_persistsAndResyncsAllReminders() {
        FakeReminderSettingsRepository repository = new FakeReminderSettingsRepository();
        FakeScheduleReminderCoordinator coordinator = new FakeScheduleReminderCoordinator();
        ReminderSettingsViewModel viewModel = new ReminderSettingsViewModel(repository, coordinator);

        viewModel.updatePopupEnabled(false);
        viewModel.updateDndEnabled(true);
        viewModel.updateDndStartMinutes(21 * 60 + 30);
        viewModel.updateDndEndMinutes(7 * 60 + 15);
        viewModel.saveSettings();

        assertTrue(coordinator.syncAllCalled);
        assertFalse(coordinator.cancelAllCalled);
        assertFalse(repository.lastSavedSettings.isPopupEnabled());
        assertTrue(repository.lastSavedSettings.isDndEnabled());
        assertEquals(21 * 60 + 30, repository.lastSavedSettings.getDndStartMinutes());
        assertEquals("提醒设置已保存", viewModel.getSavedMessage().getValue());
    }

    @Test
    public void saveSettings_whenDisabled_cancelsAllPendingReminders() {
        FakeReminderSettingsRepository repository = new FakeReminderSettingsRepository();
        FakeScheduleReminderCoordinator coordinator = new FakeScheduleReminderCoordinator();
        ReminderSettingsViewModel viewModel = new ReminderSettingsViewModel(repository, coordinator);

        viewModel.updateRemindersEnabled(false);
        viewModel.saveSettings();

        assertFalse(coordinator.syncAllCalled);
        assertTrue(coordinator.cancelAllCalled);
        assertFalse(repository.lastSavedSettings.isRemindersEnabled());
    }

    private static class FakeReminderSettingsRepository implements ReminderSettingsRepository {
        private ReminderSettings lastSavedSettings = ReminderSettings.createDefault();

        @Override
        public ReminderSettings getSettings() {
            return lastSavedSettings;
        }

        @Override
        public void saveSettings(ReminderSettings settings) {
            lastSavedSettings = settings;
        }
    }

    private static class FakeScheduleReminderCoordinator implements ScheduleReminderCoordinator {
        private boolean syncAllCalled;
        private boolean cancelAllCalled;

        @Override
        public void syncScheduleReminder(long scheduleId) {
        }

        @Override
        public void syncScheduleReminderAfterOccurrence(long scheduleId, long occurrenceStartTime) {
        }

        @Override
        public void cancelScheduleReminder(long scheduleId) {
        }

        @Override
        public void syncAllScheduleReminders() {
            syncAllCalled = true;
        }

        @Override
        public void cancelAllScheduleReminders() {
            cancelAllCalled = true;
        }
    }
}
