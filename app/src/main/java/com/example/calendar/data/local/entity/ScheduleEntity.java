package com.example.calendar.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "schedules")
public class ScheduleEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String title;

    public long startTime;

    public long endTime;

    @NonNull
    public String priority;

    public int sortOrder;

    public boolean completed;

    public ScheduleEntity(long id, @NonNull String title, long startTime, long endTime, @NonNull String priority,
                          int sortOrder, boolean completed) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priority = priority;
        this.sortOrder = sortOrder;
        this.completed = completed;
    }

    public static ScheduleEntity createDraft(@NonNull String title, long startTime, long endTime) {
        return new ScheduleEntity(0L, title, startTime, endTime, "中", 0, false);
    }

    public static ScheduleEntity fromDomain(long id, @NonNull String title, long startTime, long endTime,
                                            @NonNull String priority, int sortOrder, boolean completed) {
        return new ScheduleEntity(id, title, startTime, endTime, priority, sortOrder, completed);
    }
}
