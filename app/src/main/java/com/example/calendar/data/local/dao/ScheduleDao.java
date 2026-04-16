package com.example.calendar.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.calendar.data.local.entity.ScheduleEntity;

import java.util.List;

@Dao
public interface ScheduleDao {
    @Insert
    long insert(ScheduleEntity scheduleEntity);

    @Query("SELECT * FROM schedules ORDER BY startTime ASC")
    List<ScheduleEntity> getAll();

    @Query("SELECT * FROM schedules WHERE completed = 0 ORDER BY startTime ASC LIMIT 20")
    List<ScheduleEntity> getOpenSchedules();
}
