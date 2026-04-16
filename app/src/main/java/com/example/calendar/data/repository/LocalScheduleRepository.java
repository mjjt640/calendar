package com.example.calendar.data.repository;

import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.entity.ScheduleEntity;

import java.util.ArrayList;
import java.util.List;

public class LocalScheduleRepository implements ScheduleRepository {
    private final ScheduleDao scheduleDao;

    public LocalScheduleRepository(ScheduleDao scheduleDao) {
        this.scheduleDao = scheduleDao;
    }

    @Override
    public List<String> getTodaySchedulePreview() {
        List<ScheduleEntity> schedules = scheduleDao.getOpenSchedules();
        List<String> preview = new ArrayList<>();
        for (ScheduleEntity schedule : schedules) {
            preview.add(schedule.title);
        }
        return preview;
    }
}
