package com.example.calendar.data.repository;

import com.example.calendar.domain.model.ScheduleBackupOverview;

public interface ScheduleBackupRepository {
    ScheduleBackupOverview getOverview();

    String exportBackupJson();

    ScheduleBackupOverview importBackupJson(String json);
}
