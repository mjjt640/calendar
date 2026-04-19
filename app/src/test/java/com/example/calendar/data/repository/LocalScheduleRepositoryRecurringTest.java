package com.example.calendar.data.repository;

import com.example.calendar.data.local.dao.RecurrenceDao;
import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.entity.RecurrenceExceptionEntity;
import com.example.calendar.data.local.entity.RecurrenceSeriesEntity;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;
import com.example.calendar.domain.model.Schedule;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalScheduleRepositoryRecurringTest {

    @Test
    public void getSchedulesForDay_includesResolvedRecurringOccurrences() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                1L,
                "Daily standup",
                millisOf(2026, 4, 18, 9, 0),
                millisOf(2026, 4, 18, 9, 30),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "Room A",
                ""
        ));
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                2L,
                "One-off review",
                millisOf(2026, 4, 19, 14, 0),
                millisOf(2026, 4, 19, 15, 0),
                Schedule.PRIORITY_HIGH,
                2,
                false,
                "",
                ""
        ));
        recurrenceDao.seriesByScheduleId.put(1L, new RecurrenceSeriesEntity(
                100L,
                1L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 18, 9, 0),
                millisOf(2026, 4, 18, 9, 30),
                RecurrenceDurationType.UNTIL_DATE,
                millisOf(2026, 4, 20, 9, 0),
                null
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(2, schedules.size());
        assertEquals("Daily standup", schedules.get(0).getTitle());
        assertTrue(schedules.get(0).isRecurring());
        assertEquals(Long.valueOf(millisOf(2026, 4, 19, 9, 0)), schedules.get(0).getOccurrenceStartTime());
        assertEquals("One-off review", schedules.get(1).getTitle());
    }

    @Test
    public void getScheduleDayMarkers_includesRecurringDatesWithinWindow() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                1L,
                "Pay rent",
                millisOf(2026, 4, 18, 8, 0),
                millisOf(2026, 4, 18, 8, 30),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        recurrenceDao.seriesByScheduleId.put(1L, new RecurrenceSeriesEntity(
                101L,
                1L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 18, 8, 0),
                millisOf(2026, 4, 18, 8, 30),
                RecurrenceDurationType.UNTIL_DATE,
                millisOf(2026, 4, 20, 8, 0),
                null
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        Set<LocalDate> markers = repository.getScheduleDayMarkers(
                millisOf(2026, 4, 1, 0, 0),
                millisOf(2026, 5, 1, 0, 0)
        );

        assertEquals(new LinkedHashSet<>(Arrays.asList(
                LocalDate.of(2026, 4, 18),
                LocalDate.of(2026, 4, 19),
                LocalDate.of(2026, 4, 20)
        )), markers);
    }

    @Test
    public void getSchedulesForDay_withOverrideMovedOutsideWindow_doesNotReturnOccurrence() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                3L,
                "Lunch",
                millisOf(2026, 4, 19, 12, 0),
                millisOf(2026, 4, 19, 13, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        recurrenceDao.seriesByScheduleId.put(3L, new RecurrenceSeriesEntity(
                102L,
                3L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 19, 12, 0),
                millisOf(2026, 4, 19, 13, 0),
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                1
        ));
        recurrenceDao.exceptionsBySeriesId.put(102L, Arrays.asList(
                new RecurrenceExceptionEntity(
                        1L,
                        102L,
                        millisOf(2026, 4, 19, 12, 0),
                        RecurrenceExceptionEntity.TYPE_OVERRIDE,
                        "Moved lunch",
                        millisOf(2026, 4, 20, 12, 0),
                        millisOf(2026, 4, 20, 13, 0),
                        null,
                        null,
                        null
                )
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(0, schedules.size());
    }

    @Test
    public void getSchedulesForDay_withOverrideMovedIntoWindow_returnsOccurrence() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                4L,
                "Dinner",
                millisOf(2026, 4, 21, 18, 0),
                millisOf(2026, 4, 21, 19, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        recurrenceDao.seriesByScheduleId.put(4L, new RecurrenceSeriesEntity(
                103L,
                4L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 21, 18, 0),
                millisOf(2026, 4, 21, 19, 0),
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                1
        ));
        recurrenceDao.exceptionsBySeriesId.put(103L, Arrays.asList(
                new RecurrenceExceptionEntity(
                        2L,
                        103L,
                        millisOf(2026, 4, 21, 18, 0),
                        RecurrenceExceptionEntity.TYPE_OVERRIDE,
                        "Rescheduled dinner",
                        millisOf(2026, 4, 19, 18, 30),
                        millisOf(2026, 4, 19, 19, 30),
                        null,
                        null,
                        null
                )
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(1, schedules.size());
        assertEquals("Rescheduled dinner", schedules.get(0).getTitle());
        assertEquals(millisOf(2026, 4, 19, 18, 30), schedules.get(0).getStartTime());
        assertEquals(Long.valueOf(millisOf(2026, 4, 21, 18, 0)), schedules.get(0).getOccurrenceStartTime());
        assertEquals(1, recurrenceDao.overrideOverlapQueryCount);
    }

    @Test
    public void getSchedulesForDay_withDeleteExceptionForOccurrenceInWindow_skipsOccurrence() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                5L,
                "Focus time",
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        recurrenceDao.seriesByScheduleId.put(5L, new RecurrenceSeriesEntity(
                104L,
                5L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                1
        ));
        recurrenceDao.exceptionsBySeriesId.put(104L, Arrays.asList(
                new RecurrenceExceptionEntity(
                        3L,
                        104L,
                        millisOf(2026, 4, 19, 9, 0),
                        RecurrenceExceptionEntity.TYPE_DELETE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(0, schedules.size());
    }

    @Test
    public void getSchedulesForDay_withOverrideExceptionForOccurrenceInWindow_returnsOverriddenOccurrenceOnce() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                6L,
                "Read",
                millisOf(2026, 4, 19, 20, 0),
                millisOf(2026, 4, 19, 21, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "Desk",
                "Chapter 1"
        ));
        recurrenceDao.seriesByScheduleId.put(6L, new RecurrenceSeriesEntity(
                105L,
                6L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 19, 20, 0),
                millisOf(2026, 4, 19, 21, 0),
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                1
        ));
        recurrenceDao.exceptionsBySeriesId.put(105L, Arrays.asList(
                new RecurrenceExceptionEntity(
                        4L,
                        105L,
                        millisOf(2026, 4, 19, 20, 0),
                        RecurrenceExceptionEntity.TYPE_OVERRIDE,
                        "Read with notes",
                        millisOf(2026, 4, 19, 21, 0),
                        millisOf(2026, 4, 19, 22, 0),
                        Schedule.PRIORITY_HIGH,
                        "Library",
                        "Bring notebook"
                )
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(1, schedules.size());
        assertEquals("Read with notes", schedules.get(0).getTitle());
        assertEquals(millisOf(2026, 4, 19, 21, 0), schedules.get(0).getStartTime());
        assertEquals(Long.valueOf(millisOf(2026, 4, 19, 20, 0)), schedules.get(0).getOccurrenceStartTime());
    }

    @Test
    public void getSchedulesForDay_usesWindowQueriesInsteadOfWholeSeriesExceptions() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                7L,
                "Planning",
                millisOf(2026, 4, 21, 10, 0),
                millisOf(2026, 4, 21, 11, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        recurrenceDao.seriesByScheduleId.put(7L, new RecurrenceSeriesEntity(
                106L,
                7L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 21, 10, 0),
                millisOf(2026, 4, 21, 11, 0),
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                1
        ));
        recurrenceDao.exceptionsBySeriesId.put(106L, Arrays.asList(
                new RecurrenceExceptionEntity(
                        5L,
                        106L,
                        millisOf(2026, 4, 21, 10, 0),
                        RecurrenceExceptionEntity.TYPE_OVERRIDE,
                        "Planning moved in",
                        millisOf(2026, 4, 19, 16, 0),
                        millisOf(2026, 4, 19, 17, 0),
                        null,
                        null,
                        null
                ),
                new RecurrenceExceptionEntity(
                        6L,
                        106L,
                        millisOf(2026, 6, 1, 10, 0),
                        RecurrenceExceptionEntity.TYPE_OVERRIDE,
                        "Far future override",
                        millisOf(2026, 6, 1, 12, 0),
                        millisOf(2026, 6, 1, 13, 0),
                        null,
                        null,
                        null
                )
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(1, schedules.size());
        assertEquals("Planning moved in", schedules.get(0).getTitle());
        assertEquals(1, recurrenceDao.windowExceptionQueryCount);
        assertEquals(1, recurrenceDao.overrideOverlapQueryCount);
    }

    @Test
    public void getSchedulesForDay_withOvernightDeleteException_skipsOccurrence() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                8L,
                "Night shift",
                millisOf(2026, 4, 18, 23, 0),
                millisOf(2026, 4, 19, 1, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        recurrenceDao.seriesByScheduleId.put(8L, new RecurrenceSeriesEntity(
                107L,
                8L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 18, 23, 0),
                millisOf(2026, 4, 19, 1, 0),
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                1
        ));
        recurrenceDao.exceptionsBySeriesId.put(107L, Arrays.asList(
                new RecurrenceExceptionEntity(
                        7L,
                        107L,
                        millisOf(2026, 4, 18, 23, 0),
                        RecurrenceExceptionEntity.TYPE_DELETE,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(0, schedules.size());
    }

    @Test
    public void getSchedulesForDay_withOvernightOverrideException_returnsOverriddenOccurrence() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                9L,
                "Server restart",
                millisOf(2026, 4, 18, 23, 0),
                millisOf(2026, 4, 19, 1, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "Ops room",
                ""
        ));
        recurrenceDao.seriesByScheduleId.put(9L, new RecurrenceSeriesEntity(
                108L,
                9L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 18, 23, 0),
                millisOf(2026, 4, 19, 1, 0),
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                1
        ));
        recurrenceDao.exceptionsBySeriesId.put(108L, Arrays.asList(
                new RecurrenceExceptionEntity(
                        8L,
                        108L,
                        millisOf(2026, 4, 18, 23, 0),
                        RecurrenceExceptionEntity.TYPE_OVERRIDE,
                        "Restart postponed",
                        millisOf(2026, 4, 19, 0, 30),
                        millisOf(2026, 4, 19, 2, 0),
                        Schedule.PRIORITY_HIGH,
                        "War room",
                        "Use backup plan"
                )
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(1, schedules.size());
        assertEquals("Restart postponed", schedules.get(0).getTitle());
        assertEquals(millisOf(2026, 4, 19, 0, 30), schedules.get(0).getStartTime());
        assertEquals(millisOf(2026, 4, 19, 2, 0), schedules.get(0).getEndTime());
        assertEquals(Long.valueOf(millisOf(2026, 4, 18, 23, 0)), schedules.get(0).getOccurrenceStartTime());
    }

    @Test
    public void getSchedulesForDay_withOvernightMovedInOverride_returnsSingleOccurrence() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                10L,
                "Night deploy",
                millisOf(2026, 4, 18, 23, 0),
                millisOf(2026, 4, 19, 1, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        recurrenceDao.seriesByScheduleId.put(10L, new RecurrenceSeriesEntity(
                109L,
                10L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 18, 23, 0),
                millisOf(2026, 4, 19, 1, 0),
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                1
        ));
        recurrenceDao.exceptionsBySeriesId.put(109L, Arrays.asList(
                new RecurrenceExceptionEntity(
                        9L,
                        109L,
                        millisOf(2026, 4, 18, 23, 0),
                        RecurrenceExceptionEntity.TYPE_OVERRIDE,
                        "Deploy shifted",
                        millisOf(2026, 4, 19, 0, 15),
                        millisOf(2026, 4, 19, 1, 30),
                        null,
                        null,
                        null
                )
        ));

        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(1, schedules.size());
        assertEquals("Deploy shifted", schedules.get(0).getTitle());
        assertEquals(Long.valueOf(millisOf(2026, 4, 18, 23, 0)), schedules.get(0).getOccurrenceStartTime());
    }

    @Test
    public void updateManualOrder_withRecurringItem_doesNotRewriteAnchor() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        repository.updateManualOrder(Arrays.asList(
                new Schedule(
                        11L,
                        "Recurring occurrence",
                        millisOf(2026, 4, 19, 9, 0),
                        millisOf(2026, 4, 19, 10, 0),
                        Schedule.PRIORITY_MEDIUM,
                        1,
                        "",
                        "",
                        true,
                        200L,
                        millisOf(2026, 4, 19, 9, 0)
                ),
                new Schedule(12L, "One-time", millisOf(2026, 4, 19, 11, 0), millisOf(2026, 4, 19, 12, 0), Schedule.PRIORITY_HIGH, 2)
        ));

        assertEquals(0, scheduleDao.updatedIds.size());
        assertEquals(0, scheduleDao.updatedSortOrders.size());
    }

    private static long millisOf(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day)
                .atTime(hour, minute)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private static class FakeScheduleDao implements ScheduleDao {
        private final List<ScheduleEntity> savedSchedules = new ArrayList<>();
        private final List<Long> updatedIds = new ArrayList<>();
        private final List<Integer> updatedSortOrders = new ArrayList<>();

        @Override
        public long insert(ScheduleEntity scheduleEntity) {
            scheduleEntity.id = savedSchedules.size() + 1L;
            savedSchedules.add(scheduleEntity);
            return scheduleEntity.id;
        }

        @Override
        public void update(ScheduleEntity scheduleEntity) {
            updatedIds.add(scheduleEntity.id);
            updatedSortOrders.add(scheduleEntity.sortOrder);
        }

        @Override
        public void delete(ScheduleEntity scheduleEntity) {
            savedSchedules.remove(scheduleEntity);
        }

        @Override
        public List<ScheduleEntity> getAll() {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public List<ScheduleEntity> getOpenSchedules() {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public List<ScheduleEntity> getOpenSchedulesByTime() {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public List<ScheduleEntity> getAllOpenSchedules() {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public List<ScheduleEntity> getOpenSchedulesBetween(long startTimeInclusive, long endTimeExclusive) {
            List<ScheduleEntity> result = new ArrayList<>();
            for (ScheduleEntity schedule : savedSchedules) {
                if (!schedule.completed
                        && schedule.startTime >= startTimeInclusive
                        && schedule.startTime < endTimeExclusive) {
                    result.add(schedule);
                }
            }
            return result;
        }

        @Override
        public List<Long> getOpenScheduleStartTimesBetween(long startTimeInclusive, long endTimeExclusive) {
            List<Long> result = new ArrayList<>();
            for (ScheduleEntity schedule : savedSchedules) {
                if (!schedule.completed
                        && schedule.startTime >= startTimeInclusive
                        && schedule.startTime < endTimeExclusive) {
                    result.add(schedule.startTime);
                }
            }
            return result;
        }

        @Override
        public ScheduleEntity getById(long id) {
            for (ScheduleEntity schedule : savedSchedules) {
                if (schedule.id == id) {
                    return schedule;
                }
            }
            return null;
        }

        @Override
        public int countAll() {
            return savedSchedules.size();
        }
    }

    private static class FakeRecurrenceDao implements RecurrenceDao {
        private final Map<Long, RecurrenceSeriesEntity> seriesByScheduleId = new LinkedHashMap<>();
        private final Map<Long, List<RecurrenceExceptionEntity>> exceptionsBySeriesId = new LinkedHashMap<>();
        private int windowExceptionQueryCount;
        private int overrideOverlapQueryCount;

        @Override
        public long insertSeries(RecurrenceSeriesEntity recurrenceSeriesEntity) {
            seriesByScheduleId.put(recurrenceSeriesEntity.scheduleId, recurrenceSeriesEntity);
            return recurrenceSeriesEntity.id;
        }

        @Override
        public void updateSeries(RecurrenceSeriesEntity recurrenceSeriesEntity) {
            seriesByScheduleId.put(recurrenceSeriesEntity.scheduleId, recurrenceSeriesEntity);
        }

        @Override
        public long insertException(RecurrenceExceptionEntity recurrenceExceptionEntity) {
            exceptionsBySeriesId.computeIfAbsent(recurrenceExceptionEntity.seriesId, key -> new ArrayList<>())
                    .add(recurrenceExceptionEntity);
            return recurrenceExceptionEntity.id;
        }

        @Override
        public RecurrenceSeriesEntity getSeriesByScheduleId(long scheduleId) {
            return seriesByScheduleId.get(scheduleId);
        }

        @Override
        public List<RecurrenceSeriesEntity> getAllSeries() {
            return new ArrayList<>(seriesByScheduleId.values());
        }

        @Override
        public List<RecurrenceExceptionEntity> getExceptionsForWindow(long seriesId, long windowStartInclusive,
                                                                      long windowEndExclusive) {
            windowExceptionQueryCount++;
            List<RecurrenceExceptionEntity> result = new ArrayList<>();
            for (RecurrenceExceptionEntity exception : exceptionsBySeriesId.getOrDefault(seriesId, new ArrayList<>())) {
                if (exception.occurrenceStartTime >= windowStartInclusive
                        && exception.occurrenceStartTime < windowEndExclusive) {
                    result.add(exception);
                }
            }
            return result;
        }

        @Override
        public List<RecurrenceExceptionEntity> getOverrideExceptionsOverlappingWindow(long seriesId,
                                                                                      long windowStartInclusive,
                                                                                      long windowEndExclusive) {
            overrideOverlapQueryCount++;
            List<RecurrenceExceptionEntity> result = new ArrayList<>();
            for (RecurrenceExceptionEntity exception : exceptionsBySeriesId.getOrDefault(seriesId, new ArrayList<>())) {
                if (!RecurrenceExceptionEntity.TYPE_OVERRIDE.equals(exception.exceptionType)
                        || exception.overrideStartTime == null) {
                    continue;
                }
                long effectiveEnd = exception.overrideEndTime != null
                        ? exception.overrideEndTime
                        : exception.overrideStartTime;
                if (effectiveEnd > windowStartInclusive && exception.overrideStartTime < windowEndExclusive) {
                    result.add(exception);
                }
            }
            return result;
        }
    }
}
