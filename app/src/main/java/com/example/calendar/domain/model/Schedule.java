package com.example.calendar.domain.model;

public class Schedule {
    private final String title;
    private final long startTime;
    private final long endTime;

    public Schedule(String title, long startTime, long endTime) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getTitle() {
        return title;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
