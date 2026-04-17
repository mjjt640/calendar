package com.example.calendar.ui.home;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.Schedule;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class HomeViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void defaultTitle_isChineseScheduleTitle() {
        HomeViewModel viewModel = new HomeViewModel(new FakeScheduleRepository());

        assertEquals("日程安排", viewModel.getScreenTitle());
    }

    @Test
    public void getTimeSortedSchedules_reordersSchedulesByStartTime() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.schedules.add(new Schedule(2L, "项目复盘", 200L, 300L, "中", 2));
        repository.schedules.add(new Schedule(1L, "团队晨会", 100L, 200L, "高", 1));
        HomeViewModel viewModel = new HomeViewModel(repository);

        List<Schedule> sorted = viewModel.getTimeSortedSchedules();

        assertEquals("团队晨会", sorted.get(0).getTitle());
        assertEquals("项目复盘", sorted.get(1).getTitle());
    }

    @Test
    public void persistManualOrder_savesDraggedOrder() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        HomeViewModel viewModel = new HomeViewModel(repository);
        List<Schedule> reordered = Arrays.asList(
                new Schedule(2L, "项目复盘", 200L, 300L, "中", 2),
                new Schedule(1L, "团队晨会", 100L, 200L, "高", 1)
        );

        viewModel.persistManualOrder(reordered);

        assertEquals("项目复盘", viewModel.getSchedules().getValue().get(0).getTitle());
        assertEquals(Arrays.asList(2L, 1L), repository.updatedOrderIds);
    }

    private static class FakeScheduleRepository implements ScheduleRepository {
        private final List<Schedule> schedules = new ArrayList<>();
        private final List<Long> updatedOrderIds = new ArrayList<>();

        @Override
        public long addSchedule(Schedule schedule) {
            schedules.add(schedule);
            return schedule.getId();
        }

        @Override
        public List<Schedule> getOpenSchedules() {
            return new ArrayList<>(schedules);
        }

        @Override
        public List<Schedule> getSchedulesOrderedByTime() {
            List<Schedule> result = new ArrayList<>(schedules);
            result.sort((a, b) -> Long.compare(a.getStartTime(), b.getStartTime()));
            return result;
        }

        @Override
        public Schedule getScheduleById(long id) {
            return null;
        }

        @Override
        public int getScheduleCount() {
            return schedules.size();
        }

        @Override
        public void updateSchedule(Schedule schedule) {
        }

        @Override
        public void deleteSchedule(long id) {
        }

        @Override
        public void updateManualOrder(List<Schedule> schedules) {
            updatedOrderIds.clear();
            for (Schedule schedule : schedules) {
                updatedOrderIds.add(schedule.getId());
            }
            this.schedules.clear();
            this.schedules.addAll(schedules);
        }
    }
}
