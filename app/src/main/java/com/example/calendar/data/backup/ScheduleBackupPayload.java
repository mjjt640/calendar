package com.example.calendar.data.backup;

import com.example.calendar.data.local.entity.RecurrenceExceptionEntity;
import com.example.calendar.data.local.entity.RecurrenceSeriesEntity;
import com.example.calendar.data.local.entity.ScheduleEntity;

import java.util.ArrayList;
import java.util.List;

public class ScheduleBackupPayload {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public long exportedAt;
    public List<ScheduleEntity> schedules = new ArrayList<>();
    public List<RecurrenceSeriesEntity> recurrenceSeries = new ArrayList<>();
    public List<RecurrenceExceptionEntity> recurrenceExceptions = new ArrayList<>();
}
