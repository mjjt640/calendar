package com.example.calendar.domain.model;

public class Schedule {
    public static final String PRIORITY_HIGH = "高";
    public static final String PRIORITY_MEDIUM = "中";
    public static final String PRIORITY_LOW = "低";

    private final long id;
    private final String title;
    private final long startTime;
    private final long endTime;
    private final String priority;
    private final int sortOrder;
    private final String location;
    private final String note;

    public Schedule(long id, String title, long startTime, long endTime, String priority, int sortOrder) {
        this(id, title, startTime, endTime, priority, sortOrder, "", "");
    }

    public Schedule(long id, String title, long startTime, long endTime, String priority, int sortOrder,
                    String location, String note) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priority = priority == null || priority.trim().isEmpty() ? PRIORITY_MEDIUM : priority;
        this.sortOrder = sortOrder;
        this.location = location == null ? "" : location;
        this.note = note == null ? "" : note;
    }

    public Schedule(String title, long startTime, long endTime) {
        this(0L, title, startTime, endTime, PRIORITY_MEDIUM, 0, "", "");
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

    public String getLocation() {
        return location;
    }

    public String getNote() {
        return note;
    }

    public Schedule copyWithSortOrder(int nextSortOrder) {
        return new Schedule(id, title, startTime, endTime, priority, nextSortOrder, location, note);
    }

    public Schedule copyForUpdate(String nextTitle, long nextStartTime, long nextEndTime, String nextPriority) {
        return copyForUpdate(nextTitle, nextStartTime, nextEndTime, nextPriority, location, note);
    }

    public Schedule copyForUpdate(String nextTitle, long nextStartTime, long nextEndTime, String nextPriority,
                                  String nextLocation, String nextNote) {
        return new Schedule(
                id,
                nextTitle,
                nextStartTime,
                nextEndTime,
                nextPriority,
                sortOrder,
                nextLocation,
                nextNote
        );
    }
}
