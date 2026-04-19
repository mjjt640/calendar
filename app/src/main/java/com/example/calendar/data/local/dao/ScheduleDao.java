package com.example.calendar.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.calendar.data.local.entity.ScheduleEntity;

import java.util.List;

@Dao
public interface ScheduleDao {
    @Insert
    long insert(ScheduleEntity scheduleEntity);

    @Update
    void update(ScheduleEntity scheduleEntity);

    @Delete
    void delete(ScheduleEntity scheduleEntity);

    @Query("SELECT * FROM schedules ORDER BY sortOrder ASC, startTime ASC")
    List<ScheduleEntity> getAll();

    @Query("SELECT * FROM schedules WHERE completed = 0 ORDER BY sortOrder ASC, startTime ASC LIMIT 50")
    List<ScheduleEntity> getOpenSchedules();

    @Query("SELECT * FROM schedules WHERE completed = 0 ORDER BY startTime ASC LIMIT 50")
    List<ScheduleEntity> getOpenSchedulesByTime();

    @Query("SELECT * FROM schedules WHERE completed = 0 ORDER BY sortOrder ASC, startTime ASC")
    List<ScheduleEntity> getAllOpenSchedules();

    @Query("SELECT * FROM schedules WHERE completed = 0 AND startTime >= :startTimeInclusive AND startTime < :endTimeExclusive ORDER BY sortOrder ASC, startTime ASC")
    List<ScheduleEntity> getOpenSchedulesBetween(long startTimeInclusive, long endTimeExclusive);

    @Query("SELECT DISTINCT startTime FROM schedules WHERE completed = 0 AND startTime >= :startTimeInclusive AND startTime < :endTimeExclusive ORDER BY startTime ASC")
    List<Long> getOpenScheduleStartTimesBetween(long startTimeInclusive, long endTimeExclusive);

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    ScheduleEntity getById(long id);

    @Query("SELECT COUNT(*) FROM schedules")
    int countAll();
}
