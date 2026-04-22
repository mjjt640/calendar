package com.example.calendar.data.repository;

import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.Schedule;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class LocalScheduleRepositoryTest {

    @Test
    public void addSchedule_insertsMappedEntity() {
        FakeScheduleDao dao = new FakeScheduleDao();
        LocalScheduleRepository repository = new LocalScheduleRepository(dao);

        long id = repository.addSchedule(new Schedule(
                0L,
                "Deep work",
                1713261600000L,
                1713265200000L,
                "中",
                0,
                "三楼工位",
                "保持专注"
        ));

        assertEquals(1L, id);
        assertEquals(1, dao.savedSchedules.size());
        assertEquals("Deep work", dao.savedSchedules.get(0).title);
        assertEquals("中", dao.savedSchedules.get(0).priority);
        assertEquals("三楼工位", dao.savedSchedules.get(0).location);
        assertEquals("保持专注", dao.savedSchedules.get(0).note);
    }

    @Test
    public void getOpenSchedules_returnsMappedDomainModels() {
        FakeScheduleDao dao = new FakeScheduleDao();
        dao.savedSchedules.add(new ScheduleEntity(
                1L,
                "Read notes",
                1713261600000L,
                1713265200000L,
                "高",
                1,
                false,
                "会议室 B",
                "同步会议纪要"
        ));
        LocalScheduleRepository repository = new LocalScheduleRepository(dao);

        List<Schedule> schedules = repository.getOpenSchedules();

        assertEquals(1, schedules.size());
        assertEquals("Read notes", schedules.get(0).getTitle());
        assertEquals("高", schedules.get(0).getPriority());
        assertEquals("会议室 B", schedules.get(0).getLocation());
        assertEquals("同步会议纪要", schedules.get(0).getNote());
    }

    @Test
    public void getSchedulesForDay_returnsOnlySchedulesInsideRequestedDay() {
        FakeScheduleDao dao = new FakeScheduleDao();
        dao.savedSchedules.add(new ScheduleEntity(1L, "Morning sync", millisOf(2026, 4, 18, 9, 0), millisOf(2026, 4, 18, 10, 0), "高", 1, false));
        dao.savedSchedules.add(new ScheduleEntity(2L, "Tomorrow plan", millisOf(2026, 4, 19, 9, 0), millisOf(2026, 4, 19, 10, 0), "中", 2, false));
        LocalScheduleRepository repository = new LocalScheduleRepository(dao);

        List<Schedule> schedules = repository.getSchedulesForDay(
                millisOf(2026, 4, 18, 0, 0),
                millisOf(2026, 4, 19, 0, 0)
        );

        assertEquals(1, schedules.size());
        assertEquals("Morning sync", schedules.get(0).getTitle());
    }

    @Test
    public void getScheduleDayMarkers_returnsDistinctLocalDatesWithinMonth() {
        FakeScheduleDao dao = new FakeScheduleDao();
        dao.savedSchedules.add(new ScheduleEntity(1L, "Morning sync", millisOf(2026, 4, 10, 9, 0), millisOf(2026, 4, 10, 10, 0), "高", 1, false));
        dao.savedSchedules.add(new ScheduleEntity(2L, "Client review", millisOf(2026, 4, 10, 14, 0), millisOf(2026, 4, 10, 15, 0), "中", 2, false));
        dao.savedSchedules.add(new ScheduleEntity(3L, "Weekly wrap", millisOf(2026, 4, 18, 18, 0), millisOf(2026, 4, 18, 19, 0), "低", 3, false));
        dao.savedSchedules.add(new ScheduleEntity(4L, "Next month prep", millisOf(2026, 5, 2, 9, 0), millisOf(2026, 5, 2, 10, 0), "中", 4, false));
        LocalScheduleRepository repository = new LocalScheduleRepository(dao);

        Set<LocalDate> markers = repository.getScheduleDayMarkers(
                millisOf(2026, 4, 1, 0, 0),
                millisOf(2026, 5, 1, 0, 0)
        );

        assertEquals(new HashSet<>(Arrays.asList(
                LocalDate.of(2026, 4, 10),
                LocalDate.of(2026, 4, 18)
        )), markers);
    }

    @Test
    public void updateManualOrder_withOneTimeSchedules_rewritesSortOrder() {
        FakeScheduleDao dao = new FakeScheduleDao();
        LocalScheduleRepository repository = new LocalScheduleRepository(dao);

        repository.updateManualOrder(Arrays.asList(
                new Schedule(2L, "Second", millisOf(2026, 4, 18, 11, 0), millisOf(2026, 4, 18, 12, 0), Schedule.PRIORITY_MEDIUM, 2),
                new Schedule(1L, "First", millisOf(2026, 4, 18, 9, 0), millisOf(2026, 4, 18, 10, 0), Schedule.PRIORITY_HIGH, 1)
        ));

        assertEquals(Arrays.asList(2L, 1L), dao.updatedIds);
        assertEquals(Arrays.asList(1, 2), dao.updatedSortOrders);
    }

    private static long millisOf(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day)
                .atTime(hour, minute)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private static class FakeScheduleDao implements ScheduleDao {
        private final List<ScheduleEntity> savedSchedules = new ArrayList<>();
        private final List<Long> updatedIds = new ArrayList<>();
        private final List<Integer> updatedSortOrders = new ArrayList<>();

        @Override
        public long insert(ScheduleEntity scheduleEntity) {
            scheduleEntity.id = savedSchedules.size() + 1L;
            savedSchedules.add(scheduleEntity);
            return scheduleEntity.id;
        }

        @Override
        public void update(ScheduleEntity scheduleEntity) {
            updatedIds.add(scheduleEntity.id);
            updatedSortOrders.add(scheduleEntity.sortOrder);
        }

        @Override
        public void delete(ScheduleEntity scheduleEntity) {
            savedSchedules.remove(scheduleEntity);
        }

        @Override
        public void deleteAll() {
            savedSchedules.clear();
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
        public List<ScheduleEntity> getAllOpenSchedules() {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public List<ScheduleEntity> getOpenSchedulesBetween(long startTimeInclusive, long endTimeExclusive) {
            List<ScheduleEntity> result = new ArrayList<>();
            for (ScheduleEntity schedule : savedSchedules) {
                if (!schedule.completed
                        && schedule.startTime >= startTimeInclusive
                        && schedule.startTime < endTimeExclusive) {
                    result.add(schedule);
                }
            }
            return result;
        }

        @Override
        public List<Long> getOpenScheduleStartTimesBetween(long startTimeInclusive, long endTimeExclusive) {
            List<Long> result = new ArrayList<>();
            for (ScheduleEntity schedule : savedSchedules) {
                if (!schedule.completed
                        && schedule.startTime >= startTimeInclusive
                        && schedule.startTime < endTimeExclusive) {
                    result.add(schedule.startTime);
                }
            }
            return result;
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
