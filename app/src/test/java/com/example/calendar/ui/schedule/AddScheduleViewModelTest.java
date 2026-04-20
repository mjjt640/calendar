package com.example.calendar.ui.schedule;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.OccurrenceEditScope;
import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;
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
    public void init_withValidTimeRange_showsRecurrenceCardAndDefaultsToSingleSummary() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        assertTrue(Boolean.TRUE.equals(viewModel.getShowRecurrenceCard().getValue()));
        assertEquals("单次", viewModel.getRecurrenceSummary().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsRecurringSchedule().getValue()));
        assertEquals(OccurrenceEditScope.SINGLE, viewModel.getOccurrenceEditScope().getValue());
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
    public void applyRecurrenceDraft_refreshesSummary() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        viewModel.applyRecurrenceDraft(new RecurrenceDraft(
                true,
                null,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));

        assertEquals("每日", viewModel.getRecurrenceSummary().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getIsRecurringSchedule().getValue()));
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

    @Test
    public void loadSchedule_withRecurringDraft_restoresRecurrenceState() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                10L,
                "晨会",
                1713261600000L,
                1713265200000L,
                "中",
                3,
                "线上",
                ""
        ));
        RecurrenceDraft recurringDraft = new RecurrenceDraft(
                true,
                77L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        );
        repository.recurrenceDraftByScheduleId.add(10L, recurringDraft);
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        viewModel.loadSchedule(10L);

        assertEquals(recurringDraft, viewModel.getRecurrenceDraft().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getIsRecurringSchedule().getValue()));
        assertEquals("每周", viewModel.getRecurrenceSummary().getValue());
    }

    @Test
    public void saveSchedule_withRecurringDraft_usesRecurringSaveBranch() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );
        RecurrenceDraft draft = new RecurrenceDraft(
                true,
                null,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        );

        viewModel.applyRecurrenceDraft(draft);
        viewModel.saveSchedule("Team sync", "会议室 A", "带上周报");

        assertTrue(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertEquals(0, repository.savedSchedules.size());
        assertEquals(1, repository.savedRecurringSchedules.size());
        assertEquals("Team sync", repository.savedRecurringSchedules.get(0).getTitle());
        assertEquals(draft, repository.savedRecurringDrafts.get(0));
    }

    @Test
    public void saveSchedule_whenRecurringItemChangedToSingle_usesRecurringAwareUpdateBranch() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                11L,
                "周会",
                1713261600000L,
                1713265200000L,
                "中",
                4,
                "会议室 B",
                ""
        ));
        repository.recurrenceDraftByScheduleId.add(11L, new RecurrenceDraft(
                true,
                88L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        viewModel.loadSchedule(11L);
        viewModel.applyRecurrenceDraft(new RecurrenceDraft(
                false,
                88L,
                RecurrenceFrequency.NONE,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        viewModel.saveSchedule("周会", "会议室 B", "");

        assertEquals(0, repository.updatedSchedules.size());
        assertEquals(1, repository.updatedRecurringSchedules.size());
        assertFalse(repository.updatedRecurringDrafts.get(0).isRecurring());
        assertEquals(OccurrenceEditScope.SINGLE, repository.updatedEditScopes.get(0));
    }

    @Test
    public void saveSchedule_whenRecurringAwareUpdateThrows_setsValidationMessageInsteadOfCrashing() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                12L,
                "周会",
                1713261600000L,
                1713265200000L,
                "中",
                4,
                "会议室 B",
                ""
        ));
        repository.recurrenceDraftByScheduleId.add(12L, new RecurrenceDraft(
                true,
                99L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        repository.updateRecurringException =
                new UnsupportedOperationException("编辑已有重复系列规则暂未支持。");
        AddScheduleViewModel viewModel = new AddScheduleViewModel(
                repository,
                1713261600000L,
                1713265200000L
        );

        viewModel.loadSchedule(12L);
        viewModel.saveSchedule("周会", "会议室 B", "");

        assertFalse(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertEquals("当前版本暂不支持直接修改重复日程规则", viewModel.getValidationMessage().getValue());
    }

    private static class FakeScheduleRepository implements ScheduleRepository {
        private final List<Schedule> savedSchedules = new ArrayList<>();
        private final List<Schedule> savedRecurringSchedules = new ArrayList<>();
        private final List<RecurrenceDraft> savedRecurringDrafts = new ArrayList<>();
        private final List<Schedule> updatedSchedules = new ArrayList<>();
        private final List<Schedule> updatedRecurringSchedules = new ArrayList<>();
        private final List<RecurrenceDraft> updatedRecurringDrafts = new ArrayList<>();
        private final List<OccurrenceEditScope> updatedEditScopes = new ArrayList<>();
        private final RecurrenceDraftByScheduleId recurrenceDraftByScheduleId = new RecurrenceDraftByScheduleId();
        private RuntimeException updateRecurringException;

        @Override
        public long addSchedule(Schedule schedule) {
            savedSchedules.add(schedule);
            return savedSchedules.size();
        }

        @Override
        public long addScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft) {
            savedRecurringSchedules.add(schedule);
            savedRecurringDrafts.add(recurrenceDraft);
            return savedRecurringSchedules.size();
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
        public RecurrenceDraft getRecurrenceDraft(long scheduleId) {
            return recurrenceDraftByScheduleId.get(scheduleId);
        }

        @Override
        public void updateSchedule(Schedule schedule) {
            updatedSchedules.add(schedule);
        }

        @Override
        public void updateScheduleWithRecurrence(Schedule schedule, RecurrenceDraft recurrenceDraft,
                                                 OccurrenceEditScope editScope) {
            if (updateRecurringException != null) {
                throw updateRecurringException;
            }
            updatedRecurringSchedules.add(schedule);
            updatedRecurringDrafts.add(recurrenceDraft);
            updatedEditScopes.add(editScope);
        }

        @Override
        public void deleteSchedule(long id) {
        }

        @Override
        public void updateManualOrder(List<Schedule> schedules) {
        }
    }

    private static class RecurrenceDraftByScheduleId {
        private final List<Long> scheduleIds = new ArrayList<>();
        private final List<RecurrenceDraft> drafts = new ArrayList<>();

        void add(long scheduleId, RecurrenceDraft draft) {
            scheduleIds.add(scheduleId);
            drafts.add(draft);
        }

        RecurrenceDraft get(long scheduleId) {
            for (int index = 0; index < scheduleIds.size(); index++) {
                if (scheduleIds.get(index) == scheduleId) {
                    return drafts.get(index);
                }
            }
            return null;
        }
    }
}
