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
        assertEquals("MEDIUM", dao.savedSchedules.get(0).priority);
    }

    @Test
    public void getOpenSchedules_returnsMappedDomainModels() {
        FakeScheduleDao dao = new FakeScheduleDao();
        dao.savedSchedules.add(new ScheduleEntity("Read notes", 1713261600000L, 1713265200000L, "HIGH", false));
        LocalScheduleRepository repository = new LocalScheduleRepository(dao);

        List<Schedule> schedules = repository.getOpenSchedules();

        assertEquals(1, schedules.size());
        assertEquals("Read notes", schedules.get(0).getTitle());
    }

    private static class FakeScheduleDao implements ScheduleDao {
        private final List<ScheduleEntity> savedSchedules = new ArrayList<>();

        @Override
        public long insert(ScheduleEntity scheduleEntity) {
            savedSchedules.add(scheduleEntity);
            return savedSchedules.size();
        }

        @Override
        public List<ScheduleEntity> getAll() {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public List<ScheduleEntity> getOpenSchedules() {
            return new ArrayList<>(savedSchedules);
        }
    }
}
