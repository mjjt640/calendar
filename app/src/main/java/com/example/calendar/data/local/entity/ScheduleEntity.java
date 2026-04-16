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

    public boolean completed;

    public ScheduleEntity(@NonNull String title, long startTime, long endTime, @NonNull String priority,
                          boolean completed) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priority = priority;
        this.completed = completed;
    }

    public static ScheduleEntity createDraft(@NonNull String title, long startTime, long endTime) {
        return new ScheduleEntity(title, startTime, endTime, "MEDIUM", false);
    }
}
