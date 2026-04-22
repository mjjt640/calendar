package com.example.calendar.data.repository;

import androidx.annotation.NonNull;

import com.example.calendar.data.backup.ScheduleBackupJsonConverter;
import com.example.calendar.data.backup.ScheduleBackupPayload;
import com.example.calendar.data.local.dao.RecurrenceDao;
import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.db.AppDatabase;
import com.example.calendar.data.local.entity.RecurrenceExceptionEntity;
import com.example.calendar.data.local.entity.RecurrenceSeriesEntity;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.ScheduleBackupOverview;
import com.example.calendar.reminder.ScheduleReminderCoordinator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocalScheduleBackupRepository implements ScheduleBackupRepository {
    private final AppDatabase database;
    private final ScheduleDao scheduleDao;
    private final RecurrenceDao recurrenceDao;
    private final ScheduleBackupJsonConverter converter;
    private final ScheduleReminderCoordinator reminderCoordinator;

    public LocalScheduleBackupRepository(@NonNull AppDatabase database,
                                         @NonNull ScheduleDao scheduleDao,
                                         @NonNull RecurrenceDao recurrenceDao,
                                         @NonNull ScheduleBackupJsonConverter converter,
                                         ScheduleReminderCoordinator reminderCoordinator) {
        this.database = database;
        this.scheduleDao = scheduleDao;
        this.recurrenceDao = recurrenceDao;
        this.converter = converter;
        this.reminderCoordinator = reminderCoordinator;
    }

    @Override
    public ScheduleBackupOverview getOverview() {
        List<ScheduleEntity> schedules = scheduleDao.getAll();
        List<RecurrenceSeriesEntity> series = recurrenceDao.getAllSeries();
        List<RecurrenceExceptionEntity> exceptions = recurrenceDao.getAllExceptions();
        return new ScheduleBackupOverview(schedules.size(), series.size(), exceptions.size());
    }

    @Override
    public String exportBackupJson() {
        ScheduleBackupPayload payload = new ScheduleBackupPayload();
        payload.exportedAt = System.currentTimeMillis();
        payload.schedules = scheduleDao.getAll();
        payload.recurrenceSeries = recurrenceDao.getAllSeries();
        payload.recurrenceExceptions = recurrenceDao.getAllExceptions();
        return converter.toJson(payload);
    }

    @Override
    public ScheduleBackupOverview importBackupJson(String json) {
        ScheduleBackupPayload payload = converter.fromJson(json);
        validatePayload(payload);

        database.runInTransaction(() -> {
            if (reminderCoordinator != null) {
                reminderCoordinator.cancelAllScheduleReminders();
            }
            recurrenceDao.deleteAllExceptions();
            recurrenceDao.deleteAllSeries();
            scheduleDao.deleteAll();
            for (ScheduleEntity entity : payload.schedules) {
                scheduleDao.insert(entity);
            }
            for (RecurrenceSeriesEntity entity : payload.recurrenceSeries) {
                recurrenceDao.insertSeries(entity);
            }
            for (RecurrenceExceptionEntity entity : payload.recurrenceExceptions) {
                recurrenceDao.insertException(entity);
            }
        });

        if (reminderCoordinator != null) {
            reminderCoordinator.syncAllScheduleReminders();
        }
        return getOverview();
    }

    private void validatePayload(ScheduleBackupPayload payload) {
        if (payload.schemaVersion <= 0) {
            throw new IllegalArgumentException("Backup schema version is invalid.");
        }

        Set<Long> scheduleIds = new HashSet<>();
        for (ScheduleEntity entity : payload.schedules) {
            if (entity == null || entity.id <= 0L) {
                throw new IllegalArgumentException("Backup contains invalid schedule data.");
            }
            scheduleIds.add(entity.id);
        }

        Set<Long> seriesIds = new HashSet<>();
        for (RecurrenceSeriesEntity entity : payload.recurrenceSeries) {
            if (entity == null || entity.id <= 0L || !scheduleIds.contains(entity.scheduleId)) {
                throw new IllegalArgumentException("Backup contains invalid recurrence series.");
            }
            seriesIds.add(entity.id);
        }

        for (RecurrenceExceptionEntity entity : payload.recurrenceExceptions) {
            if (entity == null || entity.id <= 0L || !seriesIds.contains(entity.seriesId)) {
                throw new IllegalArgumentException("Backup contains invalid recurrence exceptions.");
            }
        }
    }
}
