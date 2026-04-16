package com.example.calendar.data.repository;

import com.example.calendar.domain.model.Schedule;

import java.util.List;

public interface ScheduleRepository {
    long addSchedule(Schedule schedule);

    List<Schedule> getOpenSchedules();

    List<String> getTodaySchedulePreview();
}
