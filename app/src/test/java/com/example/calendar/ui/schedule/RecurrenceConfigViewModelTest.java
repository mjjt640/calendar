package com.example.calendar.ui.schedule;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;

import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RecurrenceConfigViewModelTest {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
    private static final long START_TIME = 1713261600000L;
    private static final long END_TIME = 1713265200000L;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void initialState_defaultsToOneTimeSummary() {
        RecurrenceConfigViewModel viewModel = createViewModel();

        assertEquals("不重复", viewModel.getSummary().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getSaveEnabled().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getShowCustomInterval().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getShowDurationSection().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getShowUntilDate().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getShowOccurrenceCount().getValue()));
        assertEquals("请选择截止日期", viewModel.getUntilDateText().getValue());
    }

    @Test
    public void summary_forPresetFrequencies_matchesChineseCopy() {
        RecurrenceConfigViewModel viewModel = createViewModel();

        viewModel.updateFrequency(RecurrenceFrequency.DAILY);
        assertEquals("每天", viewModel.getSummary().getValue());
        assertTrue(Boolean.TRUE.equals(viewModel.getShowDurationSection().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getShowCustomInterval().getValue()));

        viewModel.updateFrequency(RecurrenceFrequency.WEEKLY);
        assertEquals("每周", viewModel.getSummary().getValue());

        viewModel.updateFrequency(RecurrenceFrequency.MONTHLY);
        assertEquals("每月", viewModel.getSummary().getValue());
    }

    @Test
    public void summary_forDailyUntilDate_matchesChineseCopy() {
        RecurrenceConfigViewModel viewModel = createViewModel();

        viewModel.updateFrequency(RecurrenceFrequency.DAILY);
        viewModel.updateDurationType(RecurrenceDurationType.UNTIL_DATE);
        viewModel.updateUntilTime(LocalDate.of(2026, 5, 31)
                .atStartOfDay(APP_ZONE)
                .toInstant()
                .toEpochMilli());

        assertTrue(Boolean.TRUE.equals(viewModel.getShowUntilDate().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getShowOccurrenceCount().getValue()));
        assertEquals("2026-05-31", viewModel.getUntilDateText().getValue());
        assertEquals("每天，截止到 2026-05-31", viewModel.getSummary().getValue());
    }

    @Test
    public void save_disabledWhenCustomIntervalIsInvalid() {
        RecurrenceConfigViewModel viewModel = createViewModel();

        viewModel.updateFrequency(RecurrenceFrequency.CUSTOM);
        viewModel.updateCustomIntervalUnit(RecurrenceDraft.UNIT_WEEK);
        viewModel.updateIntervalValue(0);

        assertFalse(Boolean.TRUE.equals(viewModel.getSaveEnabled().getValue()));
        assertNull(viewModel.buildResultDraft());
    }

    @Test
    public void summary_forOccurrenceCount_matchesChineseCopy() {
        RecurrenceConfigViewModel viewModel = createViewModel();

        viewModel.updateFrequency(RecurrenceFrequency.CUSTOM);
        viewModel.updateCustomIntervalUnit(RecurrenceDraft.UNIT_MONTH);
        viewModel.updateIntervalValue(3);
        viewModel.updateDurationType(RecurrenceDurationType.OCCURRENCE_COUNT);
        viewModel.updateOccurrenceCount(5);

        assertTrue(Boolean.TRUE.equals(viewModel.getShowCustomInterval().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getShowUntilDate().getValue()));
        assertTrue(Boolean.TRUE.equals(viewModel.getShowOccurrenceCount().getValue()));
        assertEquals("每 3 个月，共 5 次", viewModel.getSummary().getValue());
    }

    @Test
    public void buildResultDraft_forCustomRecurring_containsResolvedValues() {
        RecurrenceConfigViewModel viewModel = createViewModel();

        viewModel.updateFrequency(RecurrenceFrequency.CUSTOM);
        viewModel.updateCustomIntervalUnit(RecurrenceDraft.UNIT_WEEK);
        viewModel.updateIntervalValue(2);

        RecurrenceDraft result = viewModel.buildResultDraft();

        assertNotNull(result);
        assertTrue(result.isRecurring());
        assertEquals(RecurrenceFrequency.CUSTOM, result.getFrequency());
        assertEquals(RecurrenceDraft.UNIT_WEEK, result.getIntervalUnit());
        assertEquals(2, result.getIntervalValue());
    }

    @Test
    public void durationVisibility_switchesBetweenUntilDateAndOccurrenceCount() {
        RecurrenceConfigViewModel viewModel = createViewModel();

        viewModel.updateFrequency(RecurrenceFrequency.WEEKLY);
        viewModel.updateDurationType(RecurrenceDurationType.UNTIL_DATE);
        assertTrue(Boolean.TRUE.equals(viewModel.getShowUntilDate().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getShowOccurrenceCount().getValue()));

        viewModel.updateDurationType(RecurrenceDurationType.OCCURRENCE_COUNT);
        assertFalse(Boolean.TRUE.equals(viewModel.getShowUntilDate().getValue()));
        assertTrue(Boolean.TRUE.equals(viewModel.getShowOccurrenceCount().getValue()));

        viewModel.updateDurationType(RecurrenceDurationType.NONE);
        assertFalse(Boolean.TRUE.equals(viewModel.getShowUntilDate().getValue()));
        assertFalse(Boolean.TRUE.equals(viewModel.getShowOccurrenceCount().getValue()));
    }

    @Test
    public void initialDraft_untilDateBeforeAnchorDay_isNormalizedToEmptyAndCannotSave() {
        RecurrenceDraft invalidDraft = new RecurrenceDraft(
                true,
                9L,
                RecurrenceFrequency.DAILY,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.UNTIL_DATE,
                LocalDate.of(2024, 4, 15).atStartOfDay(APP_ZONE).toInstant().toEpochMilli(),
                null
        );

        RecurrenceConfigViewModel viewModel = new RecurrenceConfigViewModel(invalidDraft, START_TIME, END_TIME);

        assertEquals("请选择截止日期", viewModel.getUntilDateText().getValue());
        assertNull(viewModel.getUntilTime().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSaveEnabled().getValue()));
        assertNull(viewModel.buildResultDraft());
    }

    @Test
    public void save_disabledWhenUntilDateBeforeAnchorDay() {
        RecurrenceConfigViewModel viewModel = createViewModel();

        viewModel.updateFrequency(RecurrenceFrequency.DAILY);
        viewModel.updateDurationType(RecurrenceDurationType.UNTIL_DATE);
        viewModel.updateUntilTime(LocalDate.of(2024, 4, 15)
                .atStartOfDay(APP_ZONE)
                .toInstant()
                .toEpochMilli());

        assertEquals("请选择截止日期", viewModel.getUntilDateText().getValue());
        assertNull(viewModel.getUntilTime().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSaveEnabled().getValue()));
        assertNull(viewModel.buildResultDraft());
    }

    private RecurrenceConfigViewModel createViewModel() {
        return new RecurrenceConfigViewModel(createOneTimeDraft(), START_TIME, END_TIME);
    }

    private RecurrenceDraft createOneTimeDraft() {
        return new RecurrenceDraft(
                false,
                null,
                RecurrenceFrequency.NONE,
                RecurrenceDraft.UNIT_DAY,
                1,
                RecurrenceDurationType.NONE,
                null,
                null
        );
    }
}
