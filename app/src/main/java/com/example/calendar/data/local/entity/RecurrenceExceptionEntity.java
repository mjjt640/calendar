package com.example.calendar.data.local.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recurrence_exceptions",
        foreignKeys = @ForeignKey(
                entity = RecurrenceSeriesEntity.class,
                parentColumns = "id",
                childColumns = "seriesId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index(value = {"seriesId", "occurrenceStartTime"}, unique = true)}
)
public class RecurrenceExceptionEntity {
    public static final String TYPE_DELETE = "DELETE";
    public static final String TYPE_OVERRIDE = "OVERRIDE";

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long seriesId;

    public long occurrenceStartTime;

    public String exceptionType;

    public String overrideTitle;

    public Long overrideStartTime;

    public Long overrideEndTime;

    public String overridePriority;

    public String overrideLocation;

    public String overrideNote;

    public RecurrenceExceptionEntity(long id, long seriesId, long occurrenceStartTime, String exceptionType,
                                     String overrideTitle, Long overrideStartTime, Long overrideEndTime,
                                     String overridePriority, String overrideLocation, String overrideNote) {
        this.id = id;
        this.seriesId = seriesId;
        this.occurrenceStartTime = occurrenceStartTime;
        this.exceptionType = exceptionType;
        this.overrideTitle = overrideTitle;
        this.overrideStartTime = overrideStartTime;
        this.overrideEndTime = overrideEndTime;
        this.overridePriority = overridePriority;
        this.overrideLocation = overrideLocation;
        this.overrideNote = overrideNote;
    }
}
