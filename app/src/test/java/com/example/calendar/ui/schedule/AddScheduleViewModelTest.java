package com.example.calendar.ui.schedule;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.calendar.domain.model.Schedule;
import com.example.calendar.domain.usecase.AddScheduleUseCase;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AddScheduleViewModelTest {
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void saveSchedule_withValidInput_marksSuccess() {
        FakeAddScheduleUseCase useCase = new FakeAddScheduleUseCase();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(useCase);

        viewModel.saveSchedule("Team sync", 1713261600000L, 1713265200000L);

        assertTrue(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertNull(viewModel.getValidationMessage().getValue());
        assertEquals(1, useCase.savedSchedules.size());
        assertEquals("Team sync", useCase.savedSchedules.get(0).getTitle());
    }

    @Test
    public void saveSchedule_withBlankTitle_setsValidationError() {
        FakeAddScheduleUseCase useCase = new FakeAddScheduleUseCase();
        AddScheduleViewModel viewModel = new AddScheduleViewModel(useCase);

        viewModel.saveSchedule("   ", 1713261600000L, 1713265200000L);

        assertEquals("Title is required", viewModel.getValidationMessage().getValue());
        assertFalse(Boolean.TRUE.equals(viewModel.getSavedState().getValue()));
        assertEquals(0, useCase.savedSchedules.size());
    }

    private static class FakeAddScheduleUseCase extends AddScheduleUseCase {
        private final List<Schedule> savedSchedules = new ArrayList<>();

        FakeAddScheduleUseCase() {
            super(null);
        }

        @Override
        public long invoke(Schedule schedule) {
            savedSchedules.add(schedule);
            return savedSchedules.size();
        }
    }
}
