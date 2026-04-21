package com.example.calendar.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.calendar.domain.model.Schedule;

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

    @NonNull
    public String location;

    @NonNull
    public String note;

    public int reminderMinutesBefore;

    public ScheduleEntity(long id, @NonNull String title, long startTime, long endTime, @NonNull String priority,
                          int sortOrder, boolean completed, @NonNull String location, @NonNull String note,
                          int reminderMinutesBefore) {
        this.id = id;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.priority = priority;
        this.sortOrder = sortOrder;
        this.completed = completed;
        this.location = location;
        this.note = note;
        this.reminderMinutesBefore = reminderMinutesBefore;
    }

    @Ignore
    public ScheduleEntity(long id, @NonNull String title, long startTime, long endTime, @NonNull String priority,
                          int sortOrder, boolean completed, @NonNull String location, @NonNull String note) {
        this(id, title, startTime, endTime, priority, sortOrder, completed, location, note, Schedule.REMINDER_NONE);
    }

    @Ignore
    public ScheduleEntity(long id, @NonNull String title, long startTime, long endTime, @NonNull String priority,
                          int sortOrder, boolean completed) {
        this(id, title, startTime, endTime, priority, sortOrder, completed, "", "", Schedule.REMINDER_NONE);
    }

    public static ScheduleEntity createDraft(@NonNull String title, long startTime, long endTime) {
        return new ScheduleEntity(
                0L,
                title,
                startTime,
                endTime,
                Schedule.PRIORITY_MEDIUM,
                0,
                false,
                "",
                "",
                Schedule.REMINDER_NONE
        );
    }

    public static ScheduleEntity fromDomain(long id, @NonNull String title, long startTime, long endTime,
                                            @NonNull String priority, int sortOrder, boolean completed) {
        return fromDomain(id, title, startTime, endTime, priority, sortOrder, completed, "", "");
    }

    public static ScheduleEntity fromDomain(long id, @NonNull String title, long startTime, long endTime,
                                            @NonNull String priority, int sortOrder, boolean completed,
                                            @NonNull String location, @NonNull String note) {
        return new ScheduleEntity(
                id,
                title,
                startTime,
                endTime,
                priority,
                sortOrder,
                completed,
                location,
                note,
                Schedule.REMINDER_NONE
        );
    }

    public static ScheduleEntity fromDomain(long id, @NonNull String title, long startTime, long endTime,
                                            @NonNull String priority, int sortOrder, boolean completed,
                                            @NonNull String location, @NonNull String note,
                                            int reminderMinutesBefore) {
        return new ScheduleEntity(
                id,
                title,
                startTime,
                endTime,
                priority,
                sortOrder,
                completed,
                location,
                note,
                reminderMinutesBefore
        );
    }

    public static ScheduleEntity fromDomain(@NonNull Schedule schedule, boolean completed) {
        return fromDomain(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getPriority(),
                schedule.getSortOrder(),
                completed,
                schedule.getLocation(),
                schedule.getNote(),
                schedule.getReminderMinutesBefore()
        );
    }
}
