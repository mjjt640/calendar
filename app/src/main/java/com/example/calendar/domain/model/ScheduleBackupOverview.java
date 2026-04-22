package com.example.calendar.domain.model;

public class ScheduleBackupOverview {
    private final int scheduleCount;
    private final int recurrenceSeriesCount;
    private final int recurrenceExceptionCount;

    public ScheduleBackupOverview(int scheduleCount, int recurrenceSeriesCount, int recurrenceExceptionCount) {
        this.scheduleCount = scheduleCount;
        this.recurrenceSeriesCount = recurrenceSeriesCount;
        this.recurrenceExceptionCount = recurrenceExceptionCount;
    }

    public int getScheduleCount() {
        return scheduleCount;
    }

    public int getRecurrenceSeriesCount() {
        return recurrenceSeriesCount;
    }

    public int getRecurrenceExceptionCount() {
        return recurrenceExceptionCount;
    }
}
