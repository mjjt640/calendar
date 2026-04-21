package com.example.calendar.domain.model;

public class ReminderSettings {
    private static final int MINUTES_PER_DAY = 24 * 60;

    public static final int DEFAULT_DND_START_MINUTES = 22 * 60;
    public static final int DEFAULT_DND_END_MINUTES = 8 * 60;

    private final boolean remindersEnabled;
    private final boolean soundEnabled;
    private final boolean popupEnabled;
    private final boolean dndEnabled;
    private final int dndStartMinutes;
    private final int dndEndMinutes;
    private final boolean highPriorityBypass;

    public ReminderSettings(boolean remindersEnabled, boolean soundEnabled, boolean popupEnabled,
                            boolean dndEnabled, int dndStartMinutes, int dndEndMinutes,
                            boolean highPriorityBypass) {
        this.remindersEnabled = remindersEnabled;
        this.soundEnabled = soundEnabled;
        this.popupEnabled = popupEnabled;
        this.dndEnabled = dndEnabled;
        this.dndStartMinutes = normalizeMinutes(dndStartMinutes);
        this.dndEndMinutes = normalizeMinutes(dndEndMinutes);
        this.highPriorityBypass = highPriorityBypass;
    }

    public static ReminderSettings createDefault() {
        return new ReminderSettings(true, true, true, false,
                DEFAULT_DND_START_MINUTES, DEFAULT_DND_END_MINUTES, true);
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isPopupEnabled() {
        return popupEnabled;
    }

    public boolean isDndEnabled() {
        return dndEnabled;
    }

    public int getDndStartMinutes() {
        return dndStartMinutes;
    }

    public int getDndEndMinutes() {
        return dndEndMinutes;
    }

    public boolean isHighPriorityBypass() {
        return highPriorityBypass;
    }

    public boolean isInDndWindow(int minutesOfDay) {
        if (!dndEnabled) {
            return false;
        }
        int normalizedMinutes = normalizeMinutes(minutesOfDay);
        if (dndStartMinutes == dndEndMinutes) {
            return true;
        }
        if (dndStartMinutes < dndEndMinutes) {
            return normalizedMinutes >= dndStartMinutes && normalizedMinutes < dndEndMinutes;
        }
        return normalizedMinutes >= dndStartMinutes || normalizedMinutes < dndEndMinutes;
    }

    public boolean isReminderBlockedAt(int minutesOfDay, boolean highPriority) {
        if (!isInDndWindow(minutesOfDay)) {
            return false;
        }
        return !(highPriority && highPriorityBypass);
    }

    private int normalizeMinutes(int minutes) {
        int normalized = minutes % MINUTES_PER_DAY;
        return normalized < 0 ? normalized + MINUTES_PER_DAY : normalized;
    }
}
