package com.example.calendar.data.repository;

import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.Schedule;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LocalScheduleRepositoryTest {

    @Test
    public void addSchedule_insertsMappedEntity() {
        FakeScheduleDao dao = new FakeScheduleDao();
        LocalScheduleRepository repository = new LocalScheduleRepository(dao);

        long id = repository.addSchedule(new Schedule("Deep work", 1713261600000L, 1713265200000L));

        assertEquals(1L, id);
        assertEquals(1, dao.savedSchedules.size());
        assertEquals("Deep work", dao.savedSchedules.get(0).title);
        assertEquals("中", dao.savedSchedules.get(0).priority);
    }

    @Test
    public void getOpenSchedules_returnsMappedDomainModels() {
        FakeScheduleDao dao = new FakeScheduleDao();
        dao.savedSchedules.add(new ScheduleEntity(1L, "Read notes", 1713261600000L, 1713265200000L, "高", 1, false));
        LocalScheduleRepository repository = new LocalScheduleRepository(dao);

        List<Schedule> schedules = repository.getOpenSchedules();

        assertEquals(1, schedules.size());
        assertEquals("Read notes", schedules.get(0).getTitle());
        assertEquals("高", schedules.get(0).getPriority());
    }

    private static class FakeScheduleDao implements ScheduleDao {
        private final List<ScheduleEntity> savedSchedules = new ArrayList<>();

        @Override
        public long insert(ScheduleEntity scheduleEntity) {
            scheduleEntity.id = savedSchedules.size() + 1L;
            savedSchedules.add(scheduleEntity);
            return scheduleEntity.id;
        }

        @Override
        public void update(ScheduleEntity scheduleEntity) {
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
}
