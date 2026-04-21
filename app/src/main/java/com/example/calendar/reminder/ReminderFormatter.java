package com.example.calendar.reminder;

import androidx.annotation.NonNull;

import com.example.calendar.domain.model.Schedule;

public final class ReminderFormatter {
    public static final int[] PRESET_MINUTES = new int[]{Schedule.REMINDER_NONE, 5, 15, 30, 60};

    private ReminderFormatter() {
    }

    @NonNull
    public static String formatReminderSummary(int minutesBefore) {
        if (minutesBefore <= Schedule.REMINDER_NONE) {
            return "不提醒";
        }
        if (minutesBefore == 60) {
            return "开始前 1 小时";
        }
        return "开始前 " + minutesBefore + " 分钟";
    }

    public static boolean isPreset(int minutesBefore) {
        for (int presetMinute : PRESET_MINUTES) {
            if (presetMinute == minutesBefore) {
                return true;
            }
        }
        return false;
    }
}
