package com.example.calendar.data.repository;

import com.example.calendar.domain.model.Schedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface ScheduleRepository {
    long addSchedule(Schedule schedule);

    List<Schedule> getOpenSchedules();

    List<Schedule> getSchedulesOrderedByTime();

    List<Schedule> getSchedulesForDay(long dayStartMillis, long dayEndMillis);

    Set<LocalDate> getScheduleDayMarkers(long monthStartMillis, long monthEndMillis);

    Schedule getScheduleById(long id);

    int getScheduleCount();

    void updateSchedule(Schedule schedule);

    void deleteSchedule(long id);

    void updateManualOrder(List<Schedule> schedules);
}
