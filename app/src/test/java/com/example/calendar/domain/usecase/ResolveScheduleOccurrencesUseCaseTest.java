package com.example.calendar.domain.usecase;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ResolveScheduleOccurrencesUseCaseTest {
    private final ResolveScheduleOccurrencesUseCase useCase = new ResolveScheduleOccurrencesUseCase();

    @Test
    public void resolveDailySeries_withUntilDate_returnsThreeOccurrences() {
        ScheduleEntity anchor = scheduleEntity(11L, "Daily standup", 2026, 4, 18, 9, 0, 10, 0);
        RecurrenceSeriesEntity series = seriesEntity(
                91L,
                anchor.id,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.UNTIL_DATE,
                millisOf(2026, 4, 20, 9, 0),
                null
        );

        List<Schedule> occurrences = useCase.resolve(
                anchor,
                series,
                Collections.emptyList(),
                millisOf(2026, 4, 18, 0, 0),
                millisOf(2026, 4, 21, 0, 0)
        );

        assertEquals(3, occurrences.size());
        assertEquals(millisOf(2026, 4, 18, 9, 0), occurrences.get(0).getOccurrenceStartTime().longValue());
        assertEquals(millisOf(2026, 4, 19, 9, 0), occurrences.get(1).getOccurrenceStartTime().longValue());
        assertEquals(millisOf(2026, 4, 20, 9, 0), occurrences.get(2).getOccurrenceStartTime().longValue());
        assertTrue(occurrences.stream().allMatch(Schedule::isRecurring));
        assertTrue(occurrences.stream().allMatch(schedule -> schedule.getId() == anchor.id));
        assertTrue(occurrences.stream().allMatch(schedule -> schedule.getRecurrenceSeriesId().equals(series.id)));
    }

    @Test
    public void resolveSeries_withDeleteException_skipsDeletedOccurrence() {
        ScheduleEntity anchor = scheduleEntity(12L, "Workout", 2026, 4, 18, 7, 0, 8, 0);
        RecurrenceSeriesEntity series = seriesEntity(
                92L,
                anchor.id,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        );
        RecurrenceExceptionEntity deleteException = new RecurrenceExceptionEntity(
                1L,
                series.id,
                millisOf(2026, 4, 19, 7, 0),
                RecurrenceExceptionEntity.TYPE_DELETE,
                null,
                null,
                null,
                null,
                null,
                null
        );

        List<Schedule> occurrences = useCase.resolve(
                anchor,
                series,
                Collections.singletonList(deleteException),
                millisOf(2026, 4, 18, 0, 0),
                millisOf(2026, 4, 21, 0, 0)
        );

        assertEquals(2, occurrences.size());
        assertEquals(millisOf(2026, 4, 18, 7, 0), occurrences.get(0).getOccurrenceStartTime().longValue());
        assertEquals(millisOf(2026, 4, 20, 7, 0), occurrences.get(1).getOccurrenceStartTime().longValue());
    }

    @Test
    public void resolveSeries_withOverrideException_usesOverrideContent() {
        ScheduleEntity anchor = scheduleEntity(13L, "Read", 2026, 4, 18, 20, 0, 21, 0);
        anchor.location = "Desk";
        anchor.note = "Chapter 1";
        RecurrenceSeriesEntity series = seriesEntity(
                93L,
                anchor.id,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        );
        RecurrenceExceptionEntity overrideException = new RecurrenceExceptionEntity(
                2L,
                series.id,
                millisOf(2026, 4, 19, 20, 0),
                RecurrenceExceptionEntity.TYPE_OVERRIDE,
                "Read with notes",
                millisOf(2026, 4, 19, 21, 0),
                millisOf(2026, 4, 19, 22, 30),
                Schedule.PRIORITY_HIGH,
                "Library",
                "Bring notebook"
        );

        List<Schedule> occurrences = useCase.resolve(
                anchor,
                series,
                Collections.singletonList(overrideException),
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(1, occurrences.size());
        Schedule occurrence = occurrences.get(0);
        assertEquals("Read with notes", occurrence.getTitle());
        assertEquals(millisOf(2026, 4, 19, 21, 0), occurrence.getStartTime());
        assertEquals(millisOf(2026, 4, 19, 22, 30), occurrence.getEndTime());
        assertEquals(Schedule.PRIORITY_HIGH, occurrence.getPriority());
        assertEquals("Library", occurrence.getLocation());
        assertEquals("Bring notebook", occurrence.getNote());
        assertEquals(millisOf(2026, 4, 19, 20, 0), occurrence.getOccurrenceStartTime().longValue());
    }

    @Test
    public void resolveSeries_withOccurrenceCount_stopsAtDeclaredLimit() {
        ScheduleEntity anchor = scheduleEntity(14L, "Practice", 2026, 4, 18, 18, 0, 19, 0);
        RecurrenceSeriesEntity series = seriesEntity(
                94L,
                anchor.id,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                2
        );

        List<Schedule> occurrences = useCase.resolve(
                anchor,
                series,
                Collections.emptyList(),
                millisOf(2026, 4, 18, 0, 0),
                millisOf(2026, 4, 25, 0, 0)
        );

        assertEquals(Arrays.asList(
                millisOf(2026, 4, 18, 18, 0),
                millisOf(2026, 4, 19, 18, 0)
        ), Arrays.asList(
                occurrences.get(0).getOccurrenceStartTime(),
                occurrences.get(1).getOccurrenceStartTime()
        ));
        assertEquals(2, occurrences.size());
    }

    @Test
    public void resolveSeries_withDailyIntervalValueGreaterThanOne_skipsIntermediateDays() {
        ScheduleEntity anchor = scheduleEntity(15L, "Meditation", 2026, 4, 18, 6, 0, 6, 30);
        RecurrenceSeriesEntity series = seriesEntity(
                95L,
                anchor.id,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                2,
                RecurrenceDurationType.OCCURRENCE_COUNT,
                null,
                3
        );

        List<Schedule> occurrences = useCase.resolve(
                anchor,
                series,
                Collections.emptyList(),
                millisOf(2026, 4, 18, 0, 0),
                millisOf(2026, 4, 24, 0, 0)
        );

        assertEquals(Arrays.asList(
                millisOf(2026, 4, 18, 6, 0),
                millisOf(2026, 4, 20, 6, 0),
                millisOf(2026, 4, 22, 6, 0)
        ), Arrays.asList(
                occurrences.get(0).getOccurrenceStartTime(),
                occurrences.get(1).getOccurrenceStartTime(),
                occurrences.get(2).getOccurrenceStartTime()
        ));
    }

    @Test
    public void resolveSeries_withOverrideMovedOutsideWindow_doesNotReturnOccurrence() {
        ScheduleEntity anchor = scheduleEntity(16L, "Lunch", 2026, 4, 19, 12, 0, 13, 0);
        RecurrenceSeriesEntity series = seriesEntity(
                96L,
                anchor.id,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.NONE,
                null,
                2
        );
        RecurrenceExceptionEntity overrideException = new RecurrenceExceptionEntity(
                3L,
                series.id,
                millisOf(2026, 4, 19, 12, 0),
                RecurrenceExceptionEntity.TYPE_OVERRIDE,
                "Moved lunch",
                millisOf(2026, 4, 20, 12, 0),
                millisOf(2026, 4, 20, 13, 0),
                null,
                null,
                null
        );

        List<Schedule> occurrences = useCase.resolve(
                anchor,
                series,
                Collections.singletonList(overrideException),
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(0, occurrences.size());
    }

    @Test
    public void resolveSeries_withOverrideMovedIntoWindow_doesNotReturnOccurrenceWhenOriginalOccurrenceIsOutsideWindow() {
        ScheduleEntity anchor = scheduleEntity(17L, "Dinner", 2026, 4, 21, 18, 0, 19, 0);
        RecurrenceSeriesEntity series = seriesEntity(
                97L,
                anchor.id,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.NONE,
                null,
                1
        );
        RecurrenceExceptionEntity overrideException = new RecurrenceExceptionEntity(
                4L,
                series.id,
                millisOf(2026, 4, 21, 18, 0),
                RecurrenceExceptionEntity.TYPE_OVERRIDE,
                "Rescheduled dinner",
                millisOf(2026, 4, 19, 18, 30),
                millisOf(2026, 4, 19, 19, 30),
                null,
                null,
                null
        );

        List<Schedule> occurrences = useCase.resolve(
                anchor,
                series,
                Collections.singletonList(overrideException),
                millisOf(2026, 4, 19, 0, 0),
                millisOf(2026, 4, 20, 0, 0)
        );

        assertEquals(0, occurrences.size());
    }

    @Test
    public void createOverrideOccurrence_buildsMovedInScheduleFromException() {
        ScheduleEntity anchor = scheduleEntity(18L, "Planning", 2026, 4, 21, 10, 0, 11, 0);
        RecurrenceExceptionEntity overrideException = new RecurrenceExceptionEntity(
                5L,
                98L,
                millisOf(2026, 4, 21, 10, 0),
                RecurrenceExceptionEntity.TYPE_OVERRIDE,
                "Planning moved in",
                millisOf(2026, 4, 19, 16, 0),
                millisOf(2026, 4, 19, 17, 0),
                Schedule.PRIORITY_HIGH,
                "War room",
                "Bring roadmap"
        );

        Schedule occurrence = useCase.createOverrideOccurrence(anchor, 98L, overrideException);

        assertEquals("Planning moved in", occurrence.getTitle());
        assertEquals(millisOf(2026, 4, 19, 16, 0), occurrence.getStartTime());
        assertEquals(millisOf(2026, 4, 19, 17, 0), occurrence.getEndTime());
        assertEquals(Schedule.PRIORITY_HIGH, occurrence.getPriority());
        assertEquals("War room", occurrence.getLocation());
        assertEquals("Bring roadmap", occurrence.getNote());
        assertEquals(Long.valueOf(millisOf(2026, 4, 21, 10, 0)), occurrence.getOccurrenceStartTime());
        assertEquals(Long.valueOf(98L), occurrence.getRecurrenceSeriesId());
    }

    private static ScheduleEntity scheduleEntity(long id, String title, int year, int month, int day,
                                                 int startHour, int startMinute, int endHour, int endMinute) {
        return new ScheduleEntity(
                id,
                title,
                millisOf(year, month, day, startHour, startMinute),
                millisOf(year, month, day, endHour, endMinute),
                Schedule.PRIORITY_MEDIUM,
                1,
                false,
                "",
                ""
        );
    }

    private static RecurrenceSeriesEntity seriesEntity(long id, long scheduleId, RecurrenceFrequency frequency,
                                                       String intervalUnit, int intervalValue,
                                                       RecurrenceDurationType durationType, Long untilTime,
                                                       Integer occurrenceCount) {
        return new RecurrenceSeriesEntity(
                id,
                scheduleId,
                frequency,
                intervalUnit,
                intervalValue,
                0L,
                0L,
                durationType,
                untilTime,
                occurrenceCount
        );
    }

    private static long millisOf(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day)
                .atTime(hour, minute)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }
}
