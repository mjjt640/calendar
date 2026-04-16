package com.example.calendar.domain.usecase;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.Schedule;

public class AddScheduleUseCase {
    private final ScheduleRepository scheduleRepository;

    public AddScheduleUseCase(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public long invoke(Schedule schedule) {
        return scheduleRepository.addSchedule(schedule);
    }
}
