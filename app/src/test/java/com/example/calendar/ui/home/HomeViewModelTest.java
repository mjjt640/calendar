package com.example.calendar.ui.home;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.OccurrenceEditScope;
import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.Schedule;

import org.junit.Rule;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HomeViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void defaultState_selectsTodayAndLoadsTodaySchedules() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.addSchedule(new Schedule(1L, "Today item", millisOf(2026, 4, 18, 9, 0), millisOf(2026, 4, 18, 10, 0), "高", 1));
        repository.addSchedule(new Schedule(2L, "Tomorrow item", millisOf(2026, 4, 19, 9, 0), millisOf(2026, 4, 19, 10, 0), "中", 2));
        HomeViewModel viewModel = new HomeViewModel(repository, fixedClock());

        assertEquals("日程安排", viewModel.getScreenTitle());
        assertEquals(YearMonth.of(2026, 4), viewModel.getVisibleMonthForTest());
        assertEquals(LocalDate.of(2026, 4, 18), viewModel.getSelectedDateForTest());
        assertEquals("4月18日安排", viewModel.getSelectedDateLabel().getValue());
        assertEquals(1, viewModel.getSchedules().getValue().size());
        assertEquals("Today item", viewModel.getSchedules().getValue().get(0).getTitle());
        assertEquals(2, viewModel.getMonthState().getValue().getCells().stream().filter(cell -> cell.hasSchedule()).count());
    }

    @Test
    public void selectDate_refreshesSelectedDaySchedules() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.addSchedule(new Schedule(1L, "Today item", millisOf(2026, 4, 18, 9, 0), millisOf(2026, 4, 18, 10, 0), "高", 1));
        repository.addSchedule(new Schedule(2L, "Review", millisOf(2026, 4, 10, 14, 0), millisOf(2026, 4, 10, 15, 0), "中", 2));
        HomeViewModel viewModel = new HomeViewModel(repository, fixedClock());

        viewModel.selectDate(LocalDate.of(2026, 4, 10));

        assertEquals(LocalDate.of(2026, 4, 10), viewModel.getSelectedDateForTest());
        assertEquals("4月10日安排", viewModel.getSelectedDateLabel().getValue());
        assertEquals(1, viewModel.getSchedules().getValue().size());
        assertEquals("Review", viewModel.getSchedules().getValue().get(0).getTitle());
    }

    @Test
    public void showNextMonth_selectsFirstDayWhenPreviousDateIsOutsideNewMonth() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.addSchedule(new Schedule(1L, "May plan", millisOf(2026, 5, 1, 9, 0), millisOf(2026, 5, 1, 10, 0), "中", 1));
        HomeViewModel viewModel = new HomeViewModel(repository, fixedClock());

        viewModel.showNextMonth();

        assertEquals(YearMonth.of(2026, 5), viewModel.getVisibleMonthForTest());
        assertEquals(LocalDate.of(2026, 5, 1), viewModel.getSelectedDateForTest());
        assertEquals("5月1日安排", viewModel.getSelectedDateLabel().getValue());
        assertEquals(1, viewModel.getSchedules().getValue().size());
        assertEquals("May plan", viewModel.getSchedules().getValue().get(0).getTitle());
    }

    @Test
    public void resetToToday_restoresTodayMonthAndSchedules() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.addSchedule(new Schedule(1L, "Today item", millisOf(2026, 4, 18, 9, 0), millisOf(2026, 4, 18, 10, 0), "高", 1));
        repository.addSchedule(new Schedule(2L, "May plan", millisOf(2026, 5, 1, 9, 0), millisOf(2026, 5, 1, 10, 0), "中", 2));
        HomeViewModel viewModel = new HomeViewModel(repository, fixedClock());

        viewModel.showNextMonth();
        viewModel.resetToToday();

        assertEquals(YearMonth.of(2026, 4), viewModel.getVisibleMonthForTest());
        assertEquals(LocalDate.of(2026, 4, 18), viewModel.getSelectedDateForTest());
        assertEquals("4月18日安排", viewModel.getSelectedDateLabel().getValue());
        assertEquals(1, viewModel.getSchedules().getValue().size());
        assertEquals("Today item", viewModel.getSchedules().getValue().get(0).getTitle());
    }

    @Test
    public void persistManualOrder_savesDraggedOrderForCurrentSelection() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        HomeViewModel viewModel = new HomeViewModel(repository, fixedClock());
        List<Schedule> reordered = Arrays.asList(
                new Schedule(2L, "Review", millisOf(2026, 4, 18, 11, 0), millisOf(2026, 4, 18, 12, 0), "中", 2),
                new Schedule(1L, "Today item", millisOf(2026, 4, 18, 9, 0), millisOf(2026, 4, 18, 10, 0), "高", 1)
        );

        viewModel.persistManualOrder(reordered);

        assertEquals(Arrays.asList(2L, 1L), repository.updatedOrderIds);
        assertEquals("Review", viewModel.getSchedules().getValue().get(0).getTitle());
    }

    @Test
    public void persistManualOrder_withRecurringItems_keepsMixedOrder() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        HomeViewModel viewModel = new HomeViewModel(repository, fixedClock());
        List<Schedule> reordered = Arrays.asList(
                new Schedule(
                        30L,
                        "Weekly sync",
                        millisOf(2026, 4, 18, 15, 0),
                        millisOf(2026, 4, 18, 16, 0),
                        Schedule.PRIORITY_MEDIUM,
                        3,
                        "Room B",
                        "",
                        true,
                        930L,
                        millisOf(2026, 4, 18, 15, 0)
                ),
                new Schedule(
                        10L,
                        "Review",
                        millisOf(2026, 4, 18, 9, 0),
                        millisOf(2026, 4, 18, 10, 0),
                        Schedule.PRIORITY_MEDIUM,
                        1
                ),
                new Schedule(
                        20L,
                        "Call",
                        millisOf(2026, 4, 18, 11, 0),
                        millisOf(2026, 4, 18, 12, 0),
                        Schedule.PRIORITY_HIGH,
                        2
                )
        );

        viewModel.persistManualOrder(reordered);

        assertEquals(Arrays.asList(30L, 10L, 20L), repository.updatedOrderIds);
        assertEquals("Weekly sync", viewModel.getSchedules().getValue().get(0).getTitle());
        assertTrue(viewModel.getSchedules().getValue().get(0).isRecurring());
    }

    @Test
    public void deleteSchedule_withRecurringItem_usesRecurringDeleteApi() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        HomeViewModel viewModel = new HomeViewModel(repository, fixedClock());
        Schedule recurring = new Schedule(
                3L,
                "Weekly sync",
                millisOf(2026, 4, 18, 9, 0),
                millisOf(2026, 4, 18, 10, 0),
                Schedule.PRIORITY_MEDIUM,
                1,
                "",
                "",
                true,
                300L,
                millisOf(2026, 4, 18, 9, 0)
        );
        repository.schedules.add(recurring);

        viewModel.deleteSchedule(recurring, OccurrenceEditScope.THIS_AND_FUTURE);

        assertEquals(Arrays.asList(3L), repository.deletedRecurringIds);
        assertEquals(Arrays.asList(OccurrenceEditScope.THIS_AND_FUTURE), repository.deletedRecurringScopes);
        assertEquals(Arrays.asList(millisOf(2026, 4, 18, 9, 0)), repository.deletedRecurringOccurrenceStarts);
    }

    private static Clock fixedClock() {
        return Clock.fixed(
                Instant.parse("2026-04-18T02:00:00Z"),
                ZoneId.systemDefault()
        );
    }

    private static long millisOf(int year, int month, int day, int hour, int minute) {
        return LocalDate.of(year, month, day)
                .atTime(hour, minute)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    private static class FakeScheduleRepository implements ScheduleRepository {
        private final List<Schedule> schedules = new ArrayList<>();
        private final List<Long> updatedOrderIds = new ArrayList<>();
        private final List<Long> deletedRecurringIds = new ArrayList<>();
        private final List<OccurrenceEditScope> deletedRecurringScopes = new ArrayList<>();
        private final List<Long> deletedRecurringOccurrenceStarts = new ArrayList<>();

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
        public List<Schedule> getSchedulesForDay(long dayStartMillis, long dayEndMillis) {
            List<Schedule> result = new ArrayList<>();
            for (Schedule schedule : schedules) {
                if (schedule.getStartTime() >= dayStartMillis && schedule.getStartTime() < dayEndMillis) {
                    result.add(schedule);
                }
            }
            result.sort((a, b) -> {
                int byOrder = Integer.compare(a.getSortOrder(), b.getSortOrder());
                return byOrder != 0 ? byOrder : Long.compare(a.getStartTime(), b.getStartTime());
            });
            return result;
        }

        @Override
        public Set<LocalDate> getScheduleDayMarkers(long monthStartMillis, long monthEndMillis) {
            Set<LocalDate> result = new LinkedHashSet<>();
            for (Schedule schedule : schedules) {
                if (schedule.getStartTime() >= monthStartMillis && schedule.getStartTime() < monthEndMillis) {
                    result.add(Instant.ofEpochMilli(schedule.getStartTime()).atZone(ZoneId.systemDefault()).toLocalDate());
                }
            }
            return result;
        }

        @Override
        public Schedule getScheduleById(long id) {
            for (Schedule schedule : schedules) {
                if (schedule.getId() == id) {
                    return schedule;
                }
            }
            return null;
        }

        @Override
        public int getScheduleCount() {
            return schedules.size();
        }

        @Override
        public RecurrenceDraft getRecurrenceDraft(long scheduleId) {
            return null;
        }

        @Override
        public void updateSchedule(Schedule schedule) {
        }

        @Override
        public long addScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft) {
            schedules.add(schedule);
            return schedule.getId();
        }

        @Override
        public void updateScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft,
                                                 OccurrenceEditScope editScope, long occurrenceStartTime) {
        }

        @Override
        public void deleteSchedule(long id) {
        }

        @Override
        public void deleteScheduleWithRecurrence(long scheduleId, OccurrenceEditScope editScope,
                                                 long occurrenceStartTime) {
            deletedRecurringIds.add(scheduleId);
            deletedRecurringScopes.add(editScope);
            deletedRecurringOccurrenceStarts.add(occurrenceStartTime);
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
