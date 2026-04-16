package com.example.calendar.data.repository;

import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.Schedule;

import java.util.ArrayList;
import java.util.List;

public class LocalScheduleRepository implements ScheduleRepository {
    private final ScheduleDao scheduleDao;

    public LocalScheduleRepository(ScheduleDao scheduleDao) {
        this.scheduleDao = scheduleDao;
    }

    @Override
    public long addSchedule(Schedule schedule) {
        ScheduleEntity entity = ScheduleEntity.createDraft(
                schedule.getTitle(),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
        return scheduleDao.insert(entity);
    }

    @Override
    public List<Schedule> getOpenSchedules() {
        List<ScheduleEntity> schedules = scheduleDao.getOpenSchedules();
        List<Schedule> result = new ArrayList<>();
        for (ScheduleEntity schedule : schedules) {
            result.add(new Schedule(schedule.title, schedule.startTime, schedule.endTime));
        }
        return result;
    }

    @Override
    public List<String> getTodaySchedulePreview() {
        List<Schedule> schedules = getOpenSchedules();
        List<String> preview = new ArrayList<>();
        for (Schedule schedule : schedules) {
            preview.add(schedule.getTitle());
        }
        return preview;
    }
}
