package com.example.calendar.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;

@Entity(
        tableName = "recurrence_series",
        foreignKeys = @ForeignKey(
                entity = ScheduleEntity.class,
                parentColumns = "id",
                childColumns = "scheduleId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = {"scheduleId"}, unique = true)}
)
public class RecurrenceSeriesEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long scheduleId;

    @NonNull
    public RecurrenceFrequency frequency;

    @NonNull
    public String intervalUnit;

    public int intervalValue;

    public long anchorStartTime;

    public long anchorEndTime;

    @NonNull
    public RecurrenceDurationType durationType;

    public Long untilTime;

    public Integer occurrenceCount;

    public RecurrenceSeriesEntity(long id, long scheduleId, @NonNull RecurrenceFrequency frequency,
                                  @NonNull String intervalUnit, int intervalValue, long anchorStartTime,
                                  long anchorEndTime, @NonNull RecurrenceDurationType durationType, Long untilTime,
                                  Integer occurrenceCount) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.frequency = frequency;
        this.intervalUnit = intervalUnit;
        this.intervalValue = intervalValue;
        this.anchorStartTime = anchorStartTime;
        this.anchorEndTime = anchorEndTime;
        this.durationType = durationType;
        this.untilTime = untilTime;
        this.occurrenceCount = occurrenceCount;
    }
}
