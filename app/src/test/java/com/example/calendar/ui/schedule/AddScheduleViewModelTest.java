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
    private static final long START_TIME = 1713261600000L;
    private static final long END_TIME = 1713265200000L;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void saveSchedule_withValidInput_marksSuccess() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.saveSchedule("Team sync", "Room A", "Bring weekly report");

        assertTrue(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertNull(viewModel.getValidationMessage().getValue());
        assertEquals(1, repository.savedSchedules.size());
        assertEquals("Team sync", repository.savedSchedules.get(0).getTitle());
        assertEquals("Room A", repository.savedSchedules.get(0).getLocation());
        assertEquals("Bring weekly report", repository.savedSchedules.get(0).getNote());
    }

    @Test
    public void saveSchedule_withBlankTitle_setsValidationError() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.saveSchedule("   ", "Room A", "Bring weekly report");

        assertEquals("\u8bf7\u8f93\u5165\u65e5\u7a0b\u6807\u9898", viewModel.getValidationMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertEquals(0, repository.savedSchedules.size());
    }

    @Test
    public void init_formatsChineseTimeLabels() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        assertEquals("2024\u5e744\u670816\u65e5 18:00", viewModel.getStartTimeText().getValue());
        assertEquals("2024\u5e744\u670816\u65e5 19:00", viewModel.getEndTimeText().getValue());
    }

    @Test
    public void init_withValidTimeRange_showsRecurrenceCardAndDefaultsToSingleSummary() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        assertTrue(Boolean.TRUE.equals(viewModel.getShowRecurrenceCard().getValue()));
        assertEquals("\u5355\u6b21", viewModel.getRecurrenceSummary().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getIsRecurringSchedule().getValue()));
        assertEquals(OccurrenceEditScope.SINGLE, viewModel.getOccurrenceEditScope().getValue());
    }

    @Test
    public void updateStartTime_refreshesDisplayedLabel() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.updateStartTime(1713349800000L);

        assertEquals("2024\u5e744\u670817\u65e5 18:30", viewModel.getStartTimeText().getValue());
    }

    @Test
    public void applyRecurrenceDraft_refreshesSummary() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

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

        assertEquals("\u6bcf\u65e5", viewModel.getRecurrenceSummary().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getIsRecurringSchedule().getValue()));
    }

    @Test
    public void applyRecurrenceDraft_withUntilDate_refreshesSummaryWithDuration() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.applyRecurrenceDraft(new RecurrenceDraft(
                true,
                null,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.UNTIL_DATE,
                1714492800000L,
                null
        ));

        assertEquals("\u6bcf\u5468\uff0c\u622a\u6b62\u5230 2024-05-01", viewModel.getRecurrenceSummary().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getIsRecurringSchedule().getValue()));
    }

    @Test
    public void saveSchedule_withEndBeforeStart_setsValidationError() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, END_TIME, START_TIME);

        viewModel.saveSchedule("Team sync", "Room A", "Bring weekly report");

        assertEquals("\u7ed3\u675f\u65f6\u95f4\u4e0d\u80fd\u65e9\u4e8e\u5f00\u59cb\u65f6\u95f4",
                viewModel.getValidationMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertEquals(0, repository.savedSchedules.size());
    }

    @Test
    public void loadSchedule_withLocationAndNote_populatesFormState() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                9L,
                "Client review",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_HIGH,
                3,
                "Online meeting",
                "Confirm agenda"
        ));
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.loadSchedule(9L);

        assertEquals("Client review", viewModel.getTitleText().getValue());
        assertEquals("Online meeting", viewModel.getLocationText().getValue());
        assertEquals("Confirm agenda", viewModel.getNoteText().getValue());
        assertEquals(Schedule.PRIORITY_HIGH, viewModel.getPriority().getValue());
    }

    @Test
    public void loadSchedule_withRecurringDraft_restoresRecurrenceState() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                10L,
                "Standup",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_MEDIUM,
                3,
                "Online",
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
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.loadSchedule(10L);

        assertEquals(recurringDraft, viewModel.getRecurrenceDraft().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getIsRecurringSchedule().getValue()));
        assertEquals("\u6bcf\u5468", viewModel.getRecurrenceSummary().getValue());
    }

    @Test
    public void loadSchedule_withRecurringDraft_marksOccurrenceScopeSelectionRequired() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                14L,
                "Standup",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_MEDIUM,
                3,
                "Online",
                ""
        ));
        repository.recurrenceDraftByScheduleId.add(14L, new RecurrenceDraft(
                true,
                101L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.loadSchedule(14L);

        assertTrue(Boolean.TRUE.equals(viewModel.getIsRecurringSchedule().getValue()));
        assertEquals(OccurrenceEditScope.SINGLE, viewModel.getOccurrenceEditScope().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getShouldConfirmRecurrenceScope().getValue()));
        assertTrue(Boolean.TRUE.equals(viewModel.getShouldConfirmDeleteScope().getValue()));

        viewModel.applyRecurrenceDraft(new RecurrenceDraft(
                false,
                null,
                RecurrenceFrequency.NONE,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));

        assertTrue(Boolean.TRUE.equals(viewModel.getShouldConfirmRecurrenceScope().getValue()));
    }

    @Test
    public void saveSchedule_withRecurringDraft_usesRecurringSaveBranch() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);
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
        viewModel.saveSchedule("Team sync", "Room A", "Bring weekly report");

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
                "Weekly sync",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_MEDIUM,
                4,
                "Room B",
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
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

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
        viewModel.saveSchedule("Weekly sync", "Room B", "");

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
                "Weekly sync",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_MEDIUM,
                4,
                "Room B",
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
                new UnsupportedOperationException("unsupported recurring update");
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.loadSchedule(12L);
        viewModel.saveSchedule("Weekly sync", "Room B", "");

        assertFalse(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertEquals("\u5f53\u524d\u7248\u672c\u6682\u4e0d\u652f\u6301\u76f4\u63a5\u4fee\u6539\u91cd\u590d\u65e5\u7a0b\u89c4\u5219",
                viewModel.getValidationMessage().getValue());
    }

    @Test
    public void saveSchedule_whenScopeUpdated_usesSelectedOccurrenceEditScope() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                13L,
                "Weekly sync",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_MEDIUM,
                4,
                "Room B",
                ""
        ));
        repository.recurrenceDraftByScheduleId.add(13L, new RecurrenceDraft(
                true,
                100L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.loadSchedule(13L);
        viewModel.confirmRecurrenceScope(OccurrenceEditScope.THIS_AND_FUTURE);
        viewModel.saveSchedule("Weekly sync", "Room B", "");

        assertEquals(1, repository.updatedRecurringSchedules.size());
        assertEquals(OccurrenceEditScope.THIS_AND_FUTURE, repository.updatedEditScopes.get(0));
        assertEquals(Long.valueOf(START_TIME), repository.updatedOccurrenceStartTimes.get(0));
    }

    @Test
    public void saveSchedule_whenEntireSeriesScopeSelected_usesEntireSeriesScope() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                16L,
                "Weekly sync",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_MEDIUM,
                4,
                "Room B",
                ""
        ));
        repository.recurrenceDraftByScheduleId.add(16L, new RecurrenceDraft(
                true,
                103L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.loadSchedule(16L);
        viewModel.confirmRecurrenceScope(OccurrenceEditScope.ENTIRE_SERIES);
        viewModel.saveSchedule("Weekly sync", "Room B", "");

        assertEquals(1, repository.updatedRecurringSchedules.size());
        assertEquals(OccurrenceEditScope.ENTIRE_SERIES, repository.updatedEditScopes.get(0));
    }

    @Test
    public void updateReminderMinutesBefore_refreshesReminderSummary() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.updateReminderMinutesBefore(15);

        assertEquals(Integer.valueOf(15), viewModel.getReminderMinutesBefore().getValue());
        assertEquals("开始前 15 分钟", viewModel.getReminderSummary().getValue());
    }

    @Test
    public void saveSchedule_withReminder_persistsReminderMinutes() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);
        viewModel.updateReminderMinutesBefore(30);

        viewModel.saveSchedule("Team sync", "Room A", "Bring weekly report");

        assertEquals(1, repository.savedSchedules.size());
        assertEquals(30, repository.savedSchedules.get(0).getReminderMinutesBefore());
    }

    @Test
    public void deleteSchedule_whenRecurringScopeConfirmed_usesRecurringDeleteBranch() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                17L,
                "Weekly sync",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_MEDIUM,
                4,
                "Room B",
                ""
        ));
        repository.recurrenceDraftByScheduleId.add(17L, new RecurrenceDraft(
                true,
                104L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.loadSchedule(17L);
        viewModel.confirmDeleteScope(OccurrenceEditScope.THIS_AND_FUTURE);
        viewModel.deleteSchedule();

        assertTrue(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertEquals(1, repository.deletedScheduleIds.size());
        assertEquals(Long.valueOf(17L), repository.deletedScheduleIds.get(0));
        assertEquals(OccurrenceEditScope.THIS_AND_FUTURE, repository.deletedEditScopes.get(0));
        assertEquals(Long.valueOf(START_TIME), repository.deletedOccurrenceStartTimes.get(0));
    }

    @Test
    public void confirmDeleteScope_clearsFurtherDeleteConfirmationRequirement() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                18L,
                "Weekly sync",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_MEDIUM,
                4,
                "Room B",
                ""
        ));
        repository.recurrenceDraftByScheduleId.add(18L, new RecurrenceDraft(
                true,
                105L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.loadSchedule(18L);
        viewModel.confirmDeleteScope(OccurrenceEditScope.SINGLE);

        assertFalse(Boolean.TRUE.equals(viewModel.getShouldConfirmDeleteScope().getValue()));
    }

    @Test
    public void confirmRecurrenceScope_clearsFurtherConfirmationRequirement() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        repository.savedSchedules.add(new Schedule(
                15L,
                "Weekly sync",
                START_TIME,
                END_TIME,
                Schedule.PRIORITY_MEDIUM,
                4,
                "Room B",
                ""
        ));
        repository.recurrenceDraftByScheduleId.add(15L, new RecurrenceDraft(
                true,
                102L,
                RecurrenceFrequency.WEEKLY,
                RecurrenceDraft.UNIT_WEEK,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        AddScheduleViewModel viewModel = new AddScheduleViewModel(repository, START_TIME, END_TIME);

        viewModel.loadSchedule(15L);
        viewModel.confirmRecurrenceScope(OccurrenceEditScope.SINGLE);

        assertFalse(Boolean.TRUE.equals(viewModel.getShouldConfirmRecurrenceScope().getValue()));
        assertEquals(OccurrenceEditScope.SINGLE, viewModel.getOccurrenceEditScope().getValue());
    }

    private static class FakeScheduleRepository implements ScheduleRepository {
        private final List<Schedule> savedSchedules = new ArrayList<>();
        private final List<Schedule> savedRecurringSchedules = new ArrayList<>();
        private final List<RecurrenceDraft> savedRecurringDrafts = new ArrayList<>();
        private final List<Schedule> updatedSchedules = new ArrayList<>();
        private final List<Schedule> updatedRecurringSchedules = new ArrayList<>();
        private final List<RecurrenceDraft> updatedRecurringDrafts = new ArrayList<>();
        private final List<OccurrenceEditScope> updatedEditScopes = new ArrayList<>();
        private final List<Long> updatedOccurrenceStartTimes = new ArrayList<>();
        private final List<Long> deletedScheduleIds = new ArrayList<>();
        private final List<OccurrenceEditScope> deletedEditScopes = new ArrayList<>();
        private final List<Long> deletedOccurrenceStartTimes = new ArrayList<>();
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
                                                 OccurrenceEditScope editScope,
                                                 long occurrenceStartTime) {
            if (updateRecurringException != null) {
                throw updateRecurringException;
            }
            updatedRecurringSchedules.add(schedule);
            updatedRecurringDrafts.add(recurrenceDraft);
            updatedEditScopes.add(editScope);
            updatedOccurrenceStartTimes.add(occurrenceStartTime);
        }

        @Override
        public void deleteSchedule(long id) {
            deletedScheduleIds.add(id);
        }

        @Override
        public void deleteScheduleWithRecurrence(long scheduleId, OccurrenceEditScope editScope,
                                                 long occurrenceStartTime) {
            deletedScheduleIds.add(scheduleId);
            deletedEditScopes.add(editScope);
            deletedOccurrenceStartTimes.add(occurrenceStartTime);
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
