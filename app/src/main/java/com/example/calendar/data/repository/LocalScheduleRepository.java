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
        int nextSortOrder = scheduleDao.countAll() + 1;
        ScheduleEntity entity = ScheduleEntity.fromDomain(
                0L,
                schedule.getTitle(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getPriority(),
                nextSortOrder,
                false
        );
        return scheduleDao.insert(entity);
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
    public Schedule getScheduleById(long id) {
        ScheduleEntity entity = scheduleDao.getById(id);
        return entity == null ? null : mapSchedule(entity);
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
                false
        ));
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
        for (int index = 0; index < schedules.size(); index++) {
            Schedule schedule = schedules.get(index).copyWithSortOrder(index + 1);
            updateSchedule(schedule);
        }
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
                entity.sortOrder
        );
    }
}
