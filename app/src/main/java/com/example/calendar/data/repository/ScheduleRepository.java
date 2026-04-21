package com.example.calendar.data.repository;

import com.example.calendar.domain.model.OccurrenceEditScope;
import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface ScheduleRepository {
    long addSchedule(Schedule schedule);

    long addScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft);

    List<Schedule> getOpenSchedules();

    List<Schedule> getSchedulesOrderedByTime();

    List<Schedule> getSchedulesForDay(long dayStartMillis, long dayEndMillis);

    Set<LocalDate> getScheduleDayMarkers(long monthStartMillis, long monthEndMillis);

    Schedule getScheduleById(long id);

    RecurrenceDraft getRecurrenceDraft(long scheduleId);

    int getScheduleCount();

    void updateSchedule(Schedule schedule);

    void updateScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft,
                                      OccurrenceEditScope editScope, long occurrenceStartTime);

    default void updateScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft,
                                              OccurrenceEditScope editScope) {
        long resolvedOccurrenceStartTime = schedule.getOccurrenceStartTime() == null
                ? schedule.getStartTime()
                : schedule.getOccurrenceStartTime();
        updateScheduleWithRecurrence(
                schedule,
                recurrenceDraft,
                editScope,
                resolvedOccurrenceStartTime
        );
    }

    void deleteSchedule(long id);

    void deleteScheduleWithRecurrence(long scheduleId, OccurrenceEditScope editScope,
                                      long occurrenceStartTime);

    void updateManualOrder(List<Schedule> schedules);
}
