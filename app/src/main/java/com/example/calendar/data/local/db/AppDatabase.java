package com.example.calendar.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.entity.ScheduleEntity;

@Database(entities = {ScheduleEntity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ScheduleDao scheduleDao();
}
