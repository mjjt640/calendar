package com.example.calendar.data.repository;

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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LocalScheduleRepository implements ScheduleRepository {
    private final ScheduleDao scheduleDao;
    private final RecurrenceDao recurrenceDao;
    private final ResolveScheduleOccurrencesUseCase resolveScheduleOccurrencesUseCase;

    public LocalScheduleRepository(ScheduleDao scheduleDao) {
        this(scheduleDao, null);
    }

    public LocalScheduleRepository(ScheduleDao scheduleDao, RecurrenceDao recurrenceDao) {
        this.scheduleDao = scheduleDao;
        this.recurrenceDao = recurrenceDao;
        this.resolveScheduleOccurrencesUseCase = new ResolveScheduleOccurrencesUseCase();
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
                schedule.getNote()
        );
        return scheduleDao.insert(entity);
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
            Set<LocalDate> result = new LinkedHashSet<>();
            for (Schedule schedule : resolveSchedulesInWindow(monthStartMillis, monthEndMillis)) {
                result.add(Instant.ofEpochMilli(schedule.getStartTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate());
            }
            return result;
        }
        List<Long> startTimes = scheduleDao.getOpenScheduleStartTimesBetween(monthStartMillis, monthEndMillis);
        Set<LocalDate> result = new LinkedHashSet<>();
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
                schedule.getNote()
        ));
    }

    @Override
    public void updateScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft,
                                             OccurrenceEditScope editScope) {
        if (editScope == OccurrenceEditScope.THIS_AND_FUTURE) {
            throw new UnsupportedOperationException("OccurrenceEditScope.THIS_AND_FUTURE is not supported yet.");
        }
        if (recurrenceDao == null) {
            throw new IllegalStateException("Recurring schedule updates require recurrenceDao support.");
        }

        RecurrenceSeriesEntity existingSeries = recurrenceDao.getSeriesByScheduleId(schedule.getId());
        if (recurrenceDraft == null || !recurrenceDraft.isRecurring()) {
            updateSchedule(schedule);
            recurrenceDao.deleteSeriesByScheduleId(schedule.getId());
            return;
        }

        if (existingSeries != null) {
            throw new UnsupportedOperationException("编辑已有重复系列规则暂未支持。");
        }

        updateSchedule(schedule);
        recurrenceDao.insertSeries(buildSeriesEntity(0L, schedule.getId(), schedule, recurrenceDraft));
    }

    @Override
    public void deleteSchedule(long id) {
        ScheduleEntity entity = scheduleDao.getById(id);
        if (entity != null) {
            scheduleDao.delete(entity);
        }
    }

    @Override
    public void updateManualOrder(List<Schedule> schedules) {
        for (Schedule schedule : schedules) {
            if (schedule.isRecurring()) {
                return;
            }
        }
        for (int index = 0; index < schedules.size(); index++) {
            Schedule schedule = schedules.get(index).copyWithSortOrder(index + 1);
            updateSchedule(schedule);
        }
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
                entity.note
        );
    }
}
