package com.example.calendar.domain.model;

public class Schedule {
    private final long id;
    private final String title;
    private final long startTime;
    private final long endTime;
    private final String priority;
    private final int sortOrder;

    public Schedule(long id, String title, long startTime, long endTime, String priority, int sortOrder) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priority = priority;
        this.sortOrder = sortOrder;
    }

    public Schedule(String title, long startTime, long endTime) {
        this(0L, title, startTime, endTime, "中", 0);
    }

    public long getId() {
        return id;
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

    public String getPriority() {
        return priority;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Schedule copyWithSortOrder(int nextSortOrder) {
        return new Schedule(id, title, startTime, endTime, priority, nextSortOrder);
    }

    public Schedule copyForUpdate(String nextTitle, long nextStartTime, long nextEndTime, String nextPriority) {
        return new Schedule(id, nextTitle, nextStartTime, nextEndTime, nextPriority, sortOrder);
    }
}
