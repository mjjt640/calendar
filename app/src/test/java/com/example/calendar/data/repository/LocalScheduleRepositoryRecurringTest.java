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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    public void updateManualOrder_withRecurringItem_rewritesAnchorSortOrder() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                11L,
                "Recurring anchor",
                millisOf(2026, 4, 18, 9, 0),
                millisOf(2026, 4, 18, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                2,
                false,
                "Room A",
                ""
        ));
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                12L,
                "One-time",
                millisOf(2026, 4, 19, 11, 0),
                millisOf(2026, 4, 19, 12, 0),
                Schedule.PRIORITY_HIGH,
                1,
                false,
                "",
                ""
        ));
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(11L, new RecurrenceSeriesEntity(
                200L,
                11L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 18, 9, 0),
                millisOf(2026, 4, 18, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
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

        assertEquals(Arrays.asList(11L, 12L), scheduleDao.updatedIds);
        assertEquals(Arrays.asList(1, 2), scheduleDao.updatedSortOrders);
        assertEquals(1, scheduleDao.getById(11L).sortOrder);
        assertEquals(2, scheduleDao.getById(12L).sortOrder);
    }

    @Test
    public void getRecurrenceDraft_withExistingSeries_returnsMappedDraft() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(12L, new RecurrenceSeriesEntity(
                110L,
                12L,
                RecurrenceFrequency.MONTHLY,
                RecurrenceDraft.UNIT_MONTH,
                2,
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                5
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        RecurrenceDraft draft = repository.getRecurrenceDraft(12L);

        assertTrue(draft.isRecurring());
        assertEquals(Long.valueOf(110L), draft.getSeriesId());
        assertEquals(RecurrenceFrequency.MONTHLY, draft.getFrequency());
        assertEquals(Integer.valueOf(5), draft.getOccurrenceCount());
    }

    @Test
    public void updateScheduleWithRecurrence_whenEntireSeriesChangedToSingle_deletesExistingSeries() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                13L,
                "Weekly sync",
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(13L, new RecurrenceSeriesEntity(
                111L,
                13L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        recurrenceDao.exceptionsBySeriesId.put(111L, Arrays.asList(
                new RecurrenceExceptionEntity(
                        10L,
                        111L,
                        millisOf(2026, 4, 26, 9, 0),
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

        repository.updateScheduleWithRecurrence(
                new Schedule(
                        13L,
                        "Weekly sync",
                        millisOf(2026, 4, 19, 9, 0),
                        millisOf(2026, 4, 19, 10, 0),
                        Schedule.PRIORITY_MEDIUM,
                        1,
                        "",
                        ""
                ),
                new RecurrenceDraft(
                        false,
                        111L,
                        RecurrenceFrequency.NONE,
                        RecurrenceDraft.UNIT_DAY,
                        1,
                        RecurrenceDurationType.NONE,
                        null,
                        null
                ),
                OccurrenceEditScope.ENTIRE_SERIES,
                millisOf(2026, 4, 19, 9, 0)
        );

        assertEquals(Arrays.asList(13L), recurrenceDao.deletedSeriesByScheduleId);
        assertNull(recurrenceDao.getSeriesByScheduleId(13L));
        assertNull(repository.getRecurrenceDraft(13L));
    }

    @org.junit.Ignore("Replaced by Task 2 recurring single-scope behavior tests")
    @Test
    public void updateScheduleWithRecurrence_withExistingSeriesAndRecurringDraft_singleScopeThrows() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(14L, new RecurrenceSeriesEntity(
                112L,
                14L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        try {
            repository.updateScheduleWithRecurrence(
                    new Schedule(
                            14L,
                            "Series edit",
                            millisOf(2026, 4, 19, 9, 0),
                            millisOf(2026, 4, 19, 10, 0),
                            Schedule.PRIORITY_MEDIUM,
                            1,
                            "",
                            ""
                    ),
                    new RecurrenceDraft(
                            true,
                            112L,
                            RecurrenceFrequency.MONTHLY,
                            RecurrenceDraft.UNIT_MONTH,
                            1,
                            RecurrenceDurationType.NONE,
                            null,
                            null
                    ),
                    OccurrenceEditScope.SINGLE,
                    millisOf(2026, 4, 19, 9, 0)
            );
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("编辑已有重复系列规则"));
        }

        assertEquals(0, scheduleDao.updatedIds.size());
    }

    @org.junit.Ignore("Replaced by Task 2 recurring split behavior tests")
    @Test
    public void updateScheduleWithRecurrence_withThisAndFuture_throwsUnsupportedOperationException() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        try {
            repository.updateScheduleWithRecurrence(
                    new Schedule(
                            15L,
                            "Series edit",
                            millisOf(2026, 4, 19, 9, 0),
                            millisOf(2026, 4, 19, 10, 0),
                            Schedule.PRIORITY_MEDIUM,
                            1,
                            "",
                            ""
                    ),
                    new RecurrenceDraft(
                            true,
                            113L,
                            RecurrenceFrequency.WEEKLY,
                            RecurrenceDraft.UNIT_WEEK,
                            1,
                            RecurrenceDurationType.NONE,
                            null,
                            null
                    ),
                    OccurrenceEditScope.THIS_AND_FUTURE,
                    millisOf(2026, 4, 19, 9, 0)
            );
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage().contains("THIS_AND_FUTURE"));
        }
    }

    @Test
    public void updateScheduleWithRecurrence_withSingleScopeAndSameRule_insertsOverrideException() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(140L, new RecurrenceSeriesEntity(
                1120L,
                140L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        repository.updateScheduleWithRecurrence(
                new Schedule(
                        140L,
                        "Series edit",
                        millisOf(2026, 4, 19, 9, 30),
                        millisOf(2026, 4, 19, 10, 30),
                        Schedule.PRIORITY_HIGH,
                        1,
                        "Room C",
                        "Bring notes"
                ),
                new RecurrenceDraft(
                        true,
                        1120L,
                        RecurrenceFrequency.WEEKLY,
                        RecurrenceDraft.UNIT_WEEK,
                        1,
                        RecurrenceDurationType.NONE,
                        null,
                        null
                ),
                OccurrenceEditScope.SINGLE,
                millisOf(2026, 4, 19, 9, 0)
        );

        assertEquals(0, scheduleDao.updatedIds.size());
        assertEquals(1, recurrenceDao.insertedExceptions.size());
        RecurrenceExceptionEntity inserted = recurrenceDao.insertedExceptions.get(0);
        assertEquals(1120L, inserted.seriesId);
        assertEquals(millisOf(2026, 4, 19, 9, 0), inserted.occurrenceStartTime);
        assertEquals(RecurrenceExceptionEntity.TYPE_OVERRIDE, inserted.exceptionType);
        assertEquals("Series edit", inserted.overrideTitle);
        assertEquals(Long.valueOf(millisOf(2026, 4, 19, 9, 30)), inserted.overrideStartTime);
        assertEquals(Long.valueOf(millisOf(2026, 4, 19, 10, 30)), inserted.overrideEndTime);
        assertEquals(Schedule.PRIORITY_HIGH, inserted.overridePriority);
        assertEquals("Room C", inserted.overrideLocation);
        assertEquals("Bring notes", inserted.overrideNote);
    }

    @Test
    public void updateScheduleWithRecurrence_withSingleScopeAndChangedRule_detachesStandaloneItem() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                150L,
                "Series edit",
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(150L, new RecurrenceSeriesEntity(
                1130L,
                150L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        repository.updateScheduleWithRecurrence(
                new Schedule(
                        150L,
                        "Detached item",
                        millisOf(2026, 4, 19, 11, 0),
                        millisOf(2026, 4, 19, 12, 0),
                        Schedule.PRIORITY_HIGH,
                        1,
                        "Room D",
                        "Detached"
                ),
                new RecurrenceDraft(
                        true,
                        1130L,
                        RecurrenceFrequency.MONTHLY,
                        RecurrenceDraft.UNIT_MONTH,
                        1,
                        RecurrenceDurationType.NONE,
                        null,
                        null
                ),
                OccurrenceEditScope.SINGLE,
                millisOf(2026, 4, 19, 9, 0)
        );

        assertEquals(1, recurrenceDao.insertedExceptions.size());
        assertEquals(RecurrenceExceptionEntity.TYPE_DELETE, recurrenceDao.insertedExceptions.get(0).exceptionType);
        assertEquals(2, scheduleDao.savedSchedules.size());
        assertEquals("Detached item", scheduleDao.savedSchedules.get(1).title);
        assertEquals(0, recurrenceDao.insertedSeries.size());
    }

    @Test
    public void updateScheduleWithRecurrence_withThisAndFuture_splitsSeries() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                160L,
                "Series edit",
                millisOf(2026, 4, 7, 9, 0),
                millisOf(2026, 4, 7, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(160L, new RecurrenceSeriesEntity(
                1140L,
                160L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                millisOf(2026, 4, 7, 9, 0),
                millisOf(2026, 4, 7, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        repository.updateScheduleWithRecurrence(
                new Schedule(
                        160L,
                        "Split future",
                        millisOf(2026, 4, 21, 11, 0),
                        millisOf(2026, 4, 21, 12, 0),
                        Schedule.PRIORITY_HIGH,
                        1,
                        "Room C",
                        "Split"
                ),
                new RecurrenceDraft(
                        true,
                        1140L,
                        RecurrenceFrequency.WEEKLY,
                        RecurrenceDraft.UNIT_WEEK,
                        1,
                        RecurrenceDurationType.NONE,
                        null,
                        null
                ),
                OccurrenceEditScope.THIS_AND_FUTURE,
                millisOf(2026, 4, 21, 9, 0)
        );

        assertEquals(1, recurrenceDao.updatedSeries.size());
        assertEquals(1, recurrenceDao.insertedSeries.size());
        assertEquals(2, scheduleDao.savedSchedules.size());
        assertEquals("Split future", scheduleDao.savedSchedules.get(1).title);
        assertEquals(RecurrenceDurationType.UNTIL_DATE, recurrenceDao.updatedSeries.get(0).durationType);
        assertEquals(Long.valueOf(millisOf(2026, 4, 20, 9, 0)), recurrenceDao.updatedSeries.get(0).untilTime);
    }

    @Test
    public void updateScheduleWithRecurrence_withEntireSeries_updatesAnchorAndSeries() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                170L,
                "Series edit",
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(170L, new RecurrenceSeriesEntity(
                1150L,
                170L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        repository.updateScheduleWithRecurrence(
                new Schedule(
                        170L,
                        "Series updated",
                        millisOf(2026, 4, 19, 11, 0),
                        millisOf(2026, 4, 19, 12, 0),
                        Schedule.PRIORITY_HIGH,
                        1,
                        "Room E",
                        "All updated"
                ),
                new RecurrenceDraft(
                        true,
                        1150L,
                        RecurrenceFrequency.MONTHLY,
                        RecurrenceDraft.UNIT_MONTH,
                        2,
                        RecurrenceDurationType.OCCURRENCE_COUNT,
                        null,
                        5
                ),
                OccurrenceEditScope.ENTIRE_SERIES,
                millisOf(2026, 4, 19, 9, 0)
        );

        assertEquals(Arrays.asList(170L), scheduleDao.updatedIds);
        assertEquals(1, recurrenceDao.updatedSeries.size());
        RecurrenceSeriesEntity updatedSeries = recurrenceDao.updatedSeries.get(0);
        assertEquals(1150L, updatedSeries.id);
        assertEquals(RecurrenceFrequency.MONTHLY, updatedSeries.frequency);
        assertEquals(RecurrenceDraft.UNIT_MONTH, updatedSeries.intervalUnit);
        assertEquals(2, updatedSeries.intervalValue);
        assertEquals(RecurrenceDurationType.OCCURRENCE_COUNT, updatedSeries.durationType);
        assertEquals(Integer.valueOf(5), updatedSeries.occurrenceCount);
    }

    @Test
    public void updateScheduleWithRecurrence_withoutRecurrenceDao_failsBeforeUpdatingSchedule() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, null);

        try {
            repository.updateScheduleWithRecurrence(
                    new Schedule(
                            16L,
                            "Unsupported recurring edit",
                            millisOf(2026, 4, 19, 9, 0),
                            millisOf(2026, 4, 19, 10, 0),
                            Schedule.PRIORITY_MEDIUM,
                            1,
                            "",
                            ""
                    ),
                    new RecurrenceDraft(
                            true,
                            null,
                            RecurrenceFrequency.WEEKLY,
                            RecurrenceDraft.UNIT_WEEK,
                            1,
                            RecurrenceDurationType.NONE,
                            null,
                            null
                    ),
                    OccurrenceEditScope.SINGLE,
                    millisOf(2026, 4, 19, 9, 0)
            );
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("recurrenceDao"));
        }

        assertEquals(0, scheduleDao.updatedIds.size());
    }

    @Test
    public void deleteScheduleWithRecurrence_withEntireSeries_deletesScheduleAndSeries() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                17L,
                "Weekly sync",
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(17L, new RecurrenceSeriesEntity(
                114L,
                17L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        repository.deleteScheduleWithRecurrence(
                17L,
                OccurrenceEditScope.ENTIRE_SERIES,
                millisOf(2026, 4, 19, 9, 0)
        );

        assertNull(scheduleDao.getById(17L));
        assertNull(recurrenceDao.getSeriesByScheduleId(17L));
    }

    @Test
    public void deleteScheduleWithRecurrence_withSingleScope_throwsUnsupportedOperationException() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                18L,
                "Weekly sync",
                millisOf(2026, 4, 21, 9, 0),
                millisOf(2026, 4, 21, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(18L, new RecurrenceSeriesEntity(
                118L,
                18L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                millisOf(2026, 4, 21, 9, 0),
                millisOf(2026, 4, 21, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        repository.deleteScheduleWithRecurrence(
                18L,
                OccurrenceEditScope.SINGLE,
                millisOf(2026, 4, 23, 9, 0)
        );

        assertNotNull(scheduleDao.getById(18L));
        assertEquals(1, recurrenceDao.insertedExceptions.size());
        assertEquals(RecurrenceExceptionEntity.TYPE_DELETE, recurrenceDao.insertedExceptions.get(0).exceptionType);
        assertEquals(millisOf(2026, 4, 23, 9, 0), recurrenceDao.insertedExceptions.get(0).occurrenceStartTime);
    }

    @Test
    public void deleteScheduleWithRecurrence_withThisAndFutureScope_throwsUnsupportedOperationException() {
        FakeScheduleDao scheduleDao = new FakeScheduleDao();
        scheduleDao.savedSchedules.add(new ScheduleEntity(
                19L,
                "Weekly sync",
                millisOf(2026, 4, 19, 9, 0),
                millisOf(2026, 4, 19, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        ));
        FakeRecurrenceDao recurrenceDao = new FakeRecurrenceDao();
        recurrenceDao.seriesByScheduleId.put(19L, new RecurrenceSeriesEntity(
                119L,
                19L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                millisOf(2026, 4, 7, 9, 0),
                millisOf(2026, 4, 7, 10, 0),
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(scheduleDao, recurrenceDao);

        repository.deleteScheduleWithRecurrence(
                19L,
                OccurrenceEditScope.THIS_AND_FUTURE,
                millisOf(2026, 4, 21, 9, 0)
        );

        assertNotNull(scheduleDao.getById(19L));
        assertEquals(1, recurrenceDao.updatedSeries.size());
        assertEquals(RecurrenceDurationType.UNTIL_DATE, recurrenceDao.updatedSeries.get(0).durationType);
        assertEquals(Long.valueOf(millisOf(2026, 4, 20, 9, 0)), recurrenceDao.updatedSeries.get(0).untilTime);
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
            for (int index = 0; index < savedSchedules.size(); index++) {
                if (savedSchedules.get(index).id == scheduleEntity.id) {
                    savedSchedules.set(index, scheduleEntity);
                    return;
                }
            }
        }

        @Override
        public void delete(ScheduleEntity scheduleEntity) {
            savedSchedules.remove(scheduleEntity);
        }

        @Override
        public void deleteAll() {
            savedSchedules.clear();
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
        private final List<Long> deletedSeriesByScheduleId = new ArrayList<>();
        private final List<RecurrenceSeriesEntity> insertedSeries = new ArrayList<>();
        private final List<RecurrenceSeriesEntity> updatedSeries = new ArrayList<>();
        private final List<RecurrenceExceptionEntity> insertedExceptions = new ArrayList<>();
        private int windowExceptionQueryCount;
        private int overrideOverlapQueryCount;

        @Override
        public long insertSeries(RecurrenceSeriesEntity recurrenceSeriesEntity) {
            insertedSeries.add(recurrenceSeriesEntity);
            seriesByScheduleId.put(recurrenceSeriesEntity.scheduleId, recurrenceSeriesEntity);
            return recurrenceSeriesEntity.id;
        }

        @Override
        public void updateSeries(RecurrenceSeriesEntity recurrenceSeriesEntity) {
            updatedSeries.add(recurrenceSeriesEntity);
            seriesByScheduleId.put(recurrenceSeriesEntity.scheduleId, recurrenceSeriesEntity);
        }

        @Override
        public long insertException(RecurrenceExceptionEntity recurrenceExceptionEntity) {
            insertedExceptions.add(recurrenceExceptionEntity);
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
        public void deleteSeriesByScheduleId(long scheduleId) {
            deletedSeriesByScheduleId.add(scheduleId);
            RecurrenceSeriesEntity removed = seriesByScheduleId.remove(scheduleId);
            if (removed != null) {
                exceptionsBySeriesId.remove(removed.id);
            }
        }

        @Override
        public void deleteAllSeries() {
            seriesByScheduleId.clear();
            exceptionsBySeriesId.clear();
        }

        @Override
        public List<RecurrenceExceptionEntity> getExceptionsForSeries(long seriesId) {
            return new ArrayList<>(exceptionsBySeriesId.getOrDefault(seriesId, new ArrayList<>()));
        }

        @Override
        public List<RecurrenceExceptionEntity> getAllExceptions() {
            List<RecurrenceExceptionEntity> result = new ArrayList<>();
            for (List<RecurrenceExceptionEntity> exceptions : exceptionsBySeriesId.values()) {
                result.addAll(exceptions);
            }
            return result;
        }

        @Override
        public void deleteExceptionsBySeriesId(long seriesId) {
            exceptionsBySeriesId.remove(seriesId);
        }

        @Override
        public void deleteAllExceptions() {
            exceptionsBySeriesId.clear();
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
