package com.example.calendar.reminder;

public interface ScheduleReminderCoordinator {
    void syncScheduleReminder(long scheduleId);

    void syncScheduleReminderAfterOccurrence(long scheduleId, long occurrenceStartTime);

    void cancelScheduleReminder(long scheduleId);
}
