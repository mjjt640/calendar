package com.example.calendar.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.data.repository.ReminderSettingsRepository;
import com.example.calendar.domain.model.ReminderSettings;
import com.example.calendar.reminder.ScheduleReminderCoordinator;

import java.util.Locale;

public class ReminderSettingsViewModel extends ViewModel {
    private final ReminderSettingsRepository repository;
    private final ScheduleReminderCoordinator reminderCoordinator;
    private final MutableLiveData<Boolean> remindersEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> soundEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> popupEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> dndEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> highPriorityBypass = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> dndStartMinutes =
            new MutableLiveData<>(ReminderSettings.DEFAULT_DND_START_MINUTES);
    private final MutableLiveData<Integer> dndEndMinutes =
            new MutableLiveData<>(ReminderSettings.DEFAULT_DND_END_MINUTES);
    private final MutableLiveData<String> dndStartText = new MutableLiveData<>("22:00");
    private final MutableLiveData<String> dndEndText = new MutableLiveData<>("08:00");
    private final MutableLiveData<String> savedMessage = new MutableLiveData<>();

    public ReminderSettingsViewModel(ReminderSettingsRepository repository,
                                     ScheduleReminderCoordinator reminderCoordinator) {
        this.repository = repository;
        this.reminderCoordinator = reminderCoordinator;
        loadSettings();
    }

    public LiveData<Boolean> getRemindersEnabled() {
        return remindersEnabled;
    }

    public LiveData<Boolean> getSoundEnabled() {
        return soundEnabled;
    }

    public LiveData<Boolean> getPopupEnabled() {
        return popupEnabled;
    }

    public LiveData<Boolean> getDndEnabled() {
        return dndEnabled;
    }

    public LiveData<Boolean> getHighPriorityBypass() {
        return highPriorityBypass;
    }

    public LiveData<String> getDndStartText() {
        return dndStartText;
    }

    public LiveData<String> getDndEndText() {
        return dndEndText;
    }

    public LiveData<String> getSavedMessage() {
        return savedMessage;
    }

    public int getCurrentDndStartMinutes() {
        return valueOf(dndStartMinutes, ReminderSettings.DEFAULT_DND_START_MINUTES);
    }

    public int getCurrentDndEndMinutes() {
        return valueOf(dndEndMinutes, ReminderSettings.DEFAULT_DND_END_MINUTES);
    }

    public void updateRemindersEnabled(boolean enabled) {
        remindersEnabled.setValue(enabled);
    }

    public void updateSoundEnabled(boolean enabled) {
        soundEnabled.setValue(enabled);
    }

    public void updatePopupEnabled(boolean enabled) {
        popupEnabled.setValue(enabled);
    }

    public void updateDndEnabled(boolean enabled) {
        dndEnabled.setValue(enabled);
    }

    public void updateHighPriorityBypass(boolean enabled) {
        highPriorityBypass.setValue(enabled);
    }

    public void updateDndStartMinutes(int minutes) {
        int normalizedMinutes = normalizeMinutes(minutes);
        dndStartMinutes.setValue(normalizedMinutes);
        dndStartText.setValue(formatMinutes(normalizedMinutes));
    }

    public void updateDndEndMinutes(int minutes) {
        int normalizedMinutes = normalizeMinutes(minutes);
        dndEndMinutes.setValue(normalizedMinutes);
        dndEndText.setValue(formatMinutes(normalizedMinutes));
    }

    public void saveSettings() {
        ReminderSettings settings = new ReminderSettings(
                Boolean.TRUE.equals(remindersEnabled.getValue()),
                Boolean.TRUE.equals(soundEnabled.getValue()),
                Boolean.TRUE.equals(popupEnabled.getValue()),
                Boolean.TRUE.equals(dndEnabled.getValue()),
                valueOf(dndStartMinutes, ReminderSettings.DEFAULT_DND_START_MINUTES),
                valueOf(dndEndMinutes, ReminderSettings.DEFAULT_DND_END_MINUTES),
                Boolean.TRUE.equals(highPriorityBypass.getValue())
        );
        repository.saveSettings(settings);
        if (settings.isRemindersEnabled()) {
            reminderCoordinator.syncAllScheduleReminders();
        } else {
            reminderCoordinator.cancelAllScheduleReminders();
        }
        savedMessage.setValue("提醒设置已保存");
    }

    public void consumeSavedMessage() {
        savedMessage.setValue(null);
    }

    private void loadSettings() {
        ReminderSettings settings = repository.getSettings();
        remindersEnabled.setValue(settings.isRemindersEnabled());
        soundEnabled.setValue(settings.isSoundEnabled());
        popupEnabled.setValue(settings.isPopupEnabled());
        dndEnabled.setValue(settings.isDndEnabled());
        highPriorityBypass.setValue(settings.isHighPriorityBypass());
        updateDndStartMinutes(settings.getDndStartMinutes());
        updateDndEndMinutes(settings.getDndEndMinutes());
    }

    private String formatMinutes(int minutes) {
        int hour = minutes / 60;
        int minute = minutes % 60;
        return String.format(Locale.CHINA, "%02d:%02d", hour, minute);
    }

    private int normalizeMinutes(int minutes) {
        int normalized = minutes % (24 * 60);
        return normalized < 0 ? normalized + (24 * 60) : normalized;
    }

    private int valueOf(MutableLiveData<Integer> liveData, int fallback) {
        Integer value = liveData.getValue();
        return value == null ? fallback : value;
    }
}
