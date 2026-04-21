package com.example.calendar.data.repository;

import androidx.annotation.Nullable;

import com.example.calendar.data.local.dao.RecurrenceDao;
import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.entity.RecurrenceExceptionEntity;
import com.example.calendar.data.local.entity.RecurrenceSeriesEntity;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.OccurrenceEditScope;
import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;
import com.example.calendar.domain.model.Schedule;
import com.example.calendar.domain.usecase.ResolveScheduleOccurrencesUseCase;
import com.example.calendar.reminder.ScheduleReminderCoordinator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocalScheduleRepository implements ScheduleRepository {
    private final ScheduleDao scheduleDao;
    private final RecurrenceDao recurrenceDao;
    private final ResolveScheduleOccurrencesUseCase resolveScheduleOccurrencesUseCase;
    @Nullable
    private final ScheduleReminderCoordinator scheduleReminderCoordinator;

    public LocalScheduleRepository(ScheduleDao scheduleDao) {
        this(scheduleDao, null, null);
    }

    public LocalScheduleRepository(ScheduleDao scheduleDao, RecurrenceDao recurrenceDao) {
        this(scheduleDao, recurrenceDao, null);
    }

    public LocalScheduleRepository(ScheduleDao scheduleDao, RecurrenceDao recurrenceDao,
                                   @Nullable ScheduleReminderCoordinator scheduleReminderCoordinator) {
        this.scheduleDao = scheduleDao;
        this.recurrenceDao = recurrenceDao;
        this.resolveScheduleOccurrencesUseCase = new ResolveScheduleOccurrencesUseCase();
        this.scheduleReminderCoordinator = scheduleReminderCoordinator;
    }

    @Override
    public long addSchedule(Schedule schedule) {
        int nextSortOrder = scheduleDao.countAll() + 1;
        ScheduleEntity entity = ScheduleEntity.fromDomain(
                0L,
                schedule.getTitle(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getPriority(),
                nextSortOrder,
                false,
                schedule.getLocation(),
                schedule.getNote(),
                schedule.getReminderMinutesBefore()
        );
        long scheduleId = scheduleDao.insert(entity);
        syncReminder(scheduleId);
        return scheduleId;
    }

    @Override
    public long addScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft) {
        if (recurrenceDraft != null && recurrenceDraft.isRecurring() && recurrenceDao == null) {
            throw new IllegalStateException("Recurring schedule saves require recurrenceDao support.");
        }
        long scheduleId = addSchedule(schedule);
        if (scheduleId == 0L || recurrenceDao == null || recurrenceDraft == null || !recurrenceDraft.isRecurring()) {
            return scheduleId;
        }
        recurrenceDao.insertSeries(buildSeriesEntity(0L, scheduleId, schedule, recurrenceDraft));
        syncReminder(scheduleId);
        return scheduleId;
    }

    @Override
    public List<Schedule> getOpenSchedules() {
        return mapSchedules(scheduleDao.getOpenSchedules());
    }

    @Override
    public List<Schedule> getSchedulesOrderedByTime() {
        return mapSchedules(scheduleDao.getOpenSchedulesByTime());
    }

    @Override
    public List<Schedule> getSchedulesForDay(long dayStartMillis, long dayEndMillis) {
        if (recurrenceDao != null) {
            return resolveSchedulesInWindow(dayStartMillis, dayEndMillis);
        }
        return mapSchedules(scheduleDao.getOpenSchedulesBetween(dayStartMillis, dayEndMillis));
    }

    @Override
    public Set<LocalDate> getScheduleDayMarkers(long monthStartMillis, long monthEndMillis) {
        if (recurrenceDao != null) {
            Set<LocalDate> result = new java.util.LinkedHashSet<>();
            for (Schedule schedule : resolveSchedulesInWindow(monthStartMillis, monthEndMillis)) {
                result.add(Instant.ofEpochMilli(schedule.getStartTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate());
            }
            return result;
        }
        List<Long> startTimes = scheduleDao.getOpenScheduleStartTimesBetween(monthStartMillis, monthEndMillis);
        Set<LocalDate> result = new java.util.LinkedHashSet<>();
        for (Long startTime : startTimes) {
            result.add(Instant.ofEpochMilli(startTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());
        }
        return result;
    }

    @Override
    public Schedule getScheduleById(long id) {
        ScheduleEntity entity = scheduleDao.getById(id);
        return entity == null ? null : mapSchedule(entity);
    }

    @Override
    public RecurrenceDraft getRecurrenceDraft(long scheduleId) {
        if (recurrenceDao == null) {
            return null;
        }
        RecurrenceSeriesEntity series = recurrenceDao.getSeriesByScheduleId(scheduleId);
        return series == null ? null : mapRecurrenceDraft(series);
    }

    @Override
    public int getScheduleCount() {
        return scheduleDao.countAll();
    }

    @Override
    public void updateSchedule(Schedule schedule) {
        scheduleDao.update(ScheduleEntity.fromDomain(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getPriority(),
                schedule.getSortOrder(),
                false,
                schedule.getLocation(),
                schedule.getNote(),
                schedule.getReminderMinutesBefore()
        ));
        syncReminder(schedule.getId());
    }

    @Override
    public void updateScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft,
                                             OccurrenceEditScope editScope,
                                             long occurrenceStartTime) {
        if (recurrenceDao == null) {
            throw new IllegalStateException("Recurring schedule updates require recurrenceDao support.");
        }

        RecurrenceSeriesEntity existingSeries = recurrenceDao.getSeriesByScheduleId(schedule.getId());
        if (existingSeries == null) {
            if (recurrenceDraft == null || !recurrenceDraft.isRecurring()) {
                updateSchedule(schedule);
                return;
            }
            updateSchedule(schedule);
            recurrenceDao.insertSeries(buildSeriesEntity(0L, schedule.getId(), schedule, recurrenceDraft));
            syncReminder(schedule.getId());
            return;
        }

        if (editScope == OccurrenceEditScope.SINGLE) {
            applySingleOccurrenceUpdate(schedule, recurrenceDraft, existingSeries, occurrenceStartTime);
            return;
        }
        if (editScope == OccurrenceEditScope.THIS_AND_FUTURE) {
            applyThisAndFutureUpdate(schedule, recurrenceDraft, existingSeries, occurrenceStartTime);
            return;
        }
        applyEntireSeriesUpdate(schedule, recurrenceDraft, existingSeries);
    }

    @Override
    public void deleteSchedule(long id) {
        ScheduleEntity entity = scheduleDao.getById(id);
        if (entity != null) {
            scheduleDao.delete(entity);
        }
        cancelReminder(id);
    }

    @Override
    public void deleteScheduleWithRecurrence(long scheduleId, OccurrenceEditScope editScope,
                                             long occurrenceStartTime) {
        if (recurrenceDao == null) {
            throw new IllegalStateException("Recurring schedule deletes require recurrenceDao support.");
        }
        RecurrenceSeriesEntity existingSeries = recurrenceDao.getSeriesByScheduleId(scheduleId);
        if (existingSeries == null) {
            deleteSchedule(scheduleId);
            return;
        }
        if (editScope == OccurrenceEditScope.SINGLE) {
            recurrenceDao.insertException(new RecurrenceExceptionEntity(
                    0L,
                    existingSeries.id,
                    occurrenceStartTime,
                    RecurrenceExceptionEntity.TYPE_DELETE,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
            syncReminder(scheduleId);
            return;
        }
        if (editScope == OccurrenceEditScope.THIS_AND_FUTURE) {
            truncateSeriesBeforeOccurrence(existingSeries, occurrenceStartTime);
            retainExceptionsBeforeOccurrence(existingSeries.id, occurrenceStartTime);
            syncReminder(scheduleId);
            return;
        }
        recurrenceDao.deleteSeriesByScheduleId(scheduleId);
        deleteSchedule(scheduleId);
    }

    @Override
    public void updateManualOrder(List<Schedule> schedules) {
        Set<Long> updatedScheduleIds = new HashSet<>();
        int nextSortOrder = 1;
        for (Schedule schedule : schedules) {
            if (!updatedScheduleIds.add(schedule.getId())) {
                continue;
            }
            ScheduleEntity anchorEntity = scheduleDao.getById(schedule.getId());
            if (anchorEntity == null && schedule.isRecurring()) {
                continue;
            }
            if (anchorEntity == null) {
                updateSchedule(schedule.copyWithSortOrder(nextSortOrder++));
                continue;
            }
            updateSchedule(new Schedule(
                    anchorEntity.id,
                    anchorEntity.title,
                    anchorEntity.startTime,
                    anchorEntity.endTime,
                    anchorEntity.priority,
                    nextSortOrder++,
                    anchorEntity.location,
                    anchorEntity.note,
                    false,
                    null,
                    null,
                    anchorEntity.reminderMinutesBefore
            ));
        }
    }

    private void applySingleOccurrenceUpdate(Schedule schedule,
                                             RecurrenceDraft recurrenceDraft,
                                             RecurrenceSeriesEntity existingSeries,
                                             long occurrenceStartTime) {
        if (recurrenceDraft != null && recurrenceDraft.isRecurring()
                && matchesSeriesRule(existingSeries, recurrenceDraft)) {
            recurrenceDao.insertException(new RecurrenceExceptionEntity(
                    0L,
                    existingSeries.id,
                    occurrenceStartTime,
                    RecurrenceExceptionEntity.TYPE_OVERRIDE,
                    schedule.getTitle(),
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    schedule.getPriority(),
                    schedule.getLocation(),
                    schedule.getNote()
            ));
            syncReminder(schedule.getId());
            return;
        }

        recurrenceDao.insertException(new RecurrenceExceptionEntity(
                0L,
                existingSeries.id,
                occurrenceStartTime,
                RecurrenceExceptionEntity.TYPE_DELETE,
                null,
                null,
                null,
                null,
                null,
                null
        ));
        long detachedScheduleId = addSchedule(asDetachedSchedule(schedule));
        syncReminder(schedule.getId());
        syncReminder(detachedScheduleId);
    }

    private void applyThisAndFutureUpdate(Schedule schedule,
                                          RecurrenceDraft recurrenceDraft,
                                          RecurrenceSeriesEntity existingSeries,
                                          long occurrenceStartTime) {
        truncateSeriesBeforeOccurrence(existingSeries, occurrenceStartTime);
        retainExceptionsBeforeOccurrence(existingSeries.id, occurrenceStartTime);

        long newScheduleId = addSchedule(asDetachedSchedule(schedule));
        if (recurrenceDraft != null && recurrenceDraft.isRecurring()) {
            recurrenceDao.insertSeries(buildSeriesEntity(0L, newScheduleId, schedule, recurrenceDraft));
        }
        syncReminder(existingSeries.scheduleId);
        syncReminder(newScheduleId);
    }

    private void applyEntireSeriesUpdate(Schedule schedule,
                                         RecurrenceDraft recurrenceDraft,
                                         RecurrenceSeriesEntity existingSeries) {
        updateSchedule(schedule);
        if (recurrenceDraft == null || !recurrenceDraft.isRecurring()) {
            recurrenceDao.deleteSeriesByScheduleId(schedule.getId());
            syncReminder(schedule.getId());
            return;
        }
        recurrenceDao.updateSeries(buildSeriesEntity(existingSeries.id, schedule.getId(), schedule, recurrenceDraft));
        syncReminder(schedule.getId());
    }

    private void truncateSeriesBeforeOccurrence(RecurrenceSeriesEntity existingSeries, long occurrenceStartTime) {
        recurrenceDao.updateSeries(new RecurrenceSeriesEntity(
                existingSeries.id,
                existingSeries.scheduleId,
                existingSeries.frequency,
                existingSeries.intervalUnit,
                existingSeries.intervalValue,
                existingSeries.anchorStartTime,
                existingSeries.anchorEndTime,
                RecurrenceDurationType.UNTIL_DATE,
                toPreviousDaySameTime(occurrenceStartTime),
                null
        ));
    }

    private void retainExceptionsBeforeOccurrence(long seriesId, long occurrenceStartTime) {
        List<RecurrenceExceptionEntity> exceptions = recurrenceDao.getExceptionsForSeries(seriesId);
        recurrenceDao.deleteExceptionsBySeriesId(seriesId);
        for (RecurrenceExceptionEntity exception : exceptions) {
            if (exception.occurrenceStartTime < occurrenceStartTime) {
                recurrenceDao.insertException(exception);
            }
        }
    }

    private boolean matchesSeriesRule(RecurrenceSeriesEntity existingSeries, RecurrenceDraft recurrenceDraft) {
        return existingSeries.frequency == recurrenceDraft.getFrequency()
                && existingSeries.intervalUnit.equals(recurrenceDraft.getIntervalUnit())
                && existingSeries.intervalValue == recurrenceDraft.getIntervalValue()
                && existingSeries.durationType == recurrenceDraft.getDurationType()
                && equalsNullable(existingSeries.untilTime, recurrenceDraft.getUntilTime())
                && equalsNullable(existingSeries.occurrenceCount, recurrenceDraft.getOccurrenceCount());
    }

    private Schedule asDetachedSchedule(Schedule schedule) {
        return new Schedule(
                0L,
                schedule.getTitle(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getPriority(),
                schedule.getSortOrder(),
                schedule.getLocation(),
                schedule.getNote(),
                false,
                null,
                null,
                schedule.getReminderMinutesBefore()
        );
    }

    private long toPreviousDaySameTime(long timeMillis) {
        return Instant.ofEpochMilli(timeMillis)
                .atZone(ZoneId.systemDefault())
                .minusDays(1)
                .toInstant()
                .toEpochMilli();
    }

    private boolean equalsNullable(Object left, Object right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
    }

    private List<Schedule> resolveSchedulesInWindow(long windowStart, long windowEnd) {
        List<Schedule> results = new ArrayList<>();
        Set<String> recurringKeys = new HashSet<>();
        for (ScheduleEntity entity : scheduleDao.getAllOpenSchedules()) {
            RecurrenceSeriesEntity series = recurrenceDao.getSeriesByScheduleId(entity.id);
            if (series == null) {
                if (entity.startTime >= windowStart && entity.startTime < windowEnd) {
                    results.add(mapSchedule(entity));
                }
                continue;
            }

            long originalDuration = Math.max(0L, resolveOriginalEndTime(entity, series)
                    - resolveOriginalStartTime(entity, series));
            long sourceWindowStart = Math.max(0L, windowStart - originalDuration);
            List<RecurrenceExceptionEntity> windowExceptions =
                    recurrenceDao.getExceptionsForWindow(series.id, sourceWindowStart, windowEnd);
            for (Schedule schedule : resolveScheduleOccurrencesUseCase.resolve(
                    entity,
                    series,
                    windowExceptions,
                    windowStart,
                    windowEnd
            )) {
                addRecurringIfAbsent(results, recurringKeys, schedule);
            }
            addMovedInOverrides(results, recurringKeys, entity, series.id, windowStart, windowEnd);
        }

        results.sort(Comparator.comparingInt(Schedule::getSortOrder)
                .thenComparingLong(Schedule::getStartTime));
        return results;
    }

    private void addMovedInOverrides(List<Schedule> results, Set<String> recurringKeys,
                                     ScheduleEntity anchor, long seriesId,
                                     long windowStart, long windowEnd) {
        List<RecurrenceExceptionEntity> overlappingOverrides =
                recurrenceDao.getOverrideExceptionsOverlappingWindow(seriesId, windowStart, windowEnd);
        for (RecurrenceExceptionEntity exception : overlappingOverrides) {
            if (containsRecurringOccurrence(recurringKeys, seriesId, exception.occurrenceStartTime)) {
                continue;
            }
            Schedule overrideOccurrence =
                    resolveScheduleOccurrencesUseCase.createOverrideOccurrence(anchor, seriesId, exception);
            if (overrideOccurrence != null && resolveScheduleOccurrencesUseCase.overlapsWindow(
                    overrideOccurrence.getStartTime(),
                    overrideOccurrence.getEndTime(),
                    windowStart,
                    windowEnd
            )) {
                addRecurringIfAbsent(results, recurringKeys, overrideOccurrence);
            }
        }
    }

    private long resolveOriginalStartTime(ScheduleEntity entity, RecurrenceSeriesEntity series) {
        return series.anchorStartTime > 0 ? series.anchorStartTime : entity.startTime;
    }

    private long resolveOriginalEndTime(ScheduleEntity entity, RecurrenceSeriesEntity series) {
        return series.anchorEndTime > 0 ? series.anchorEndTime : entity.endTime;
    }

    private void addRecurringIfAbsent(List<Schedule> results, Set<String> recurringKeys, Schedule schedule) {
        if (!schedule.isRecurring()) {
            results.add(schedule);
            return;
        }
        String key = recurringKey(schedule.getRecurrenceSeriesId(), schedule.getOccurrenceStartTime());
        if (recurringKeys.add(key)) {
            results.add(schedule);
        }
    }

    private boolean containsRecurringOccurrence(Set<String> recurringKeys, long seriesId, long occurrenceStartTime) {
        return recurringKeys.contains(recurringKey(seriesId, occurrenceStartTime));
    }

    private String recurringKey(Long seriesId, Long occurrenceStartTime) {
        return String.valueOf(seriesId) + "#" + String.valueOf(occurrenceStartTime);
    }

    private RecurrenceSeriesEntity buildSeriesEntity(long seriesId, long scheduleId, Schedule schedule,
                                                     RecurrenceDraft recurrenceDraft) {
        RecurrenceFrequency frequency = recurrenceDraft.getFrequency() == null
                ? RecurrenceFrequency.NONE
                : recurrenceDraft.getFrequency();
        String intervalUnit = recurrenceDraft.getIntervalUnit() == null
                ? RecurrenceDraft.UNIT_DAY
                : recurrenceDraft.getIntervalUnit();
        int intervalValue = recurrenceDraft.getIntervalValue() > 0 ? recurrenceDraft.getIntervalValue() : 1;
        RecurrenceDurationType durationType = recurrenceDraft.getDurationType() == null
                ? RecurrenceDurationType.NONE
                : recurrenceDraft.getDurationType();
        return new RecurrenceSeriesEntity(
                seriesId,
                scheduleId,
                frequency,
                intervalUnit,
                intervalValue,
                schedule.getStartTime(),
                schedule.getEndTime(),
                durationType,
                recurrenceDraft.getUntilTime(),
                recurrenceDraft.getOccurrenceCount()
        );
    }

    private RecurrenceDraft mapRecurrenceDraft(RecurrenceSeriesEntity series) {
        return new RecurrenceDraft(
                series.frequency != RecurrenceFrequency.NONE,
                series.id,
                series.frequency,
                series.intervalUnit,
                series.intervalValue,
                series.durationType,
                series.untilTime,
                series.occurrenceCount
        );
    }

    private List<Schedule> mapSchedules(List<ScheduleEntity> entities) {
        List<Schedule> result = new ArrayList<>();
        for (ScheduleEntity entity : entities) {
            result.add(mapSchedule(entity));
        }
        return result;
    }

    private Schedule mapSchedule(ScheduleEntity entity) {
        return new Schedule(
                entity.id,
                entity.title,
                entity.startTime,
                entity.endTime,
                entity.priority,
                entity.sortOrder,
                entity.location,
                entity.note,
                false,
                null,
                null,
                entity.reminderMinutesBefore
        );
    }

    private void syncReminder(long scheduleId) {
        if (scheduleReminderCoordinator == null || scheduleId == 0L) {
            return;
        }
        scheduleReminderCoordinator.syncScheduleReminder(scheduleId);
    }

    private void cancelReminder(long scheduleId) {
        if (scheduleReminderCoordinator == null || scheduleId == 0L) {
            return;
        }
        scheduleReminderCoordinator.cancelScheduleReminder(scheduleId);
    }
}
