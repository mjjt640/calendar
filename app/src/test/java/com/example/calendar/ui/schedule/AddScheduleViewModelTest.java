package com.example.calendar.ui.schedule;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.Schedule;

import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AddScheduleViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void saveSchedule_withValidInput_marksSuccess() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        viewModel.saveSchedule("Team sync", "会议室 A", "带上周报");

        assertTrue(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertNull(viewModel.getValidationMessage().getValue());
        assertEquals(1, repository.savedSchedules.size());
        assertEquals("Team sync", repository.savedSchedules.get(0).getTitle());
        assertEquals("会议室 A", repository.savedSchedules.get(0).getLocation());
        assertEquals("带上周报", repository.savedSchedules.get(0).getNote());
    }

    @Test
    public void saveSchedule_withBlankTitle_setsValidationError() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        viewModel.saveSchedule("   ", "会议室 A", "带上周报");

        assertEquals("请输入日程标题", viewModel.getValidationMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertEquals(0, repository.savedSchedules.size());
    }

    @Test
    public void init_formatsChineseTimeLabels() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        assertEquals("2024年4月16日 18:00", viewModel.getStartTimeText().getValue());
        assertEquals("2024年4月16日 19:00", viewModel.getEndTimeText().getValue());
    }

    @Test
    public void updateStartTime_refreshesDisplayedLabel() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        viewModel.updateStartTime(1713349800000L);

        assertEquals("2024年4月17日 18:30", viewModel.getStartTimeText().getValue());
    }

    @Test
    public void saveSchedule_withEndBeforeStart_setsValidationError() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713265200000L,
                1713261600000L
        );

        viewModel.saveSchedule("Team sync", "会议室 A", "带上周报");

        assertEquals("结束时间不能早于开始时间", viewModel.getValidationMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertEquals(0, repository.savedSchedules.size());
    }

    @Test
    public void loadSchedule_withLocationAndNote_populatesFormState() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                9L,
                "客户沟通",
                1713261600000L,
                1713265200000L,
                "高",
                3,
                "线上会议",
                "确认排期"
        ));
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        viewModel.loadSchedule(9L);

        assertEquals("客户沟通", viewModel.getTitleText().getValue());
        assertEquals("线上会议", viewModel.getLocationText().getValue());
        assertEquals("确认排期", viewModel.getNoteText().getValue());
        assertEquals("高", viewModel.getPriority().getValue());
    }

    private static class FakeScheduleRepository implements ScheduleRepository {
        private final List<Schedule> savedSchedules = new ArrayList<>();

        @Override
        public long addSchedule(Schedule schedule) {
            savedSchedules.add(schedule);
            return savedSchedules.size();
        }

        @Override
        public List<Schedule> getOpenSchedules() {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public List<Schedule> getSchedulesOrderedByTime() {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public List<Schedule> getSchedulesForDay(long dayStartMillis, long dayEndMillis) {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public Set<LocalDate> getScheduleDayMarkers(long monthStartMillis, long monthEndMillis) {
            return new LinkedHashSet<>();
        }

        @Override
        public Schedule getScheduleById(long id) {
            for (Schedule schedule : savedSchedules) {
                if (schedule.getId() == id) {
                    return schedule;
                }
            }
            return null;
        }

        @Override
        public int getScheduleCount() {
            return savedSchedules.size();
        }

        @Override
        public void updateSchedule(Schedule schedule) {
        }

        @Override
        public void deleteSchedule(long id) {
        }

        @Override
        public void updateManualOrder(List<Schedule> schedules) {
        }
    }
}
