package com.example.calendar.ui.schedule;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.domain.model.RecurrenceDraft;

public class RecurrenceConfigViewModelFactory implements ViewModelProvider.Factory {
    private final RecurrenceDraft initialDraft;
    private final long startTime;
    private final long endTime;

    public RecurrenceConfigViewModelFactory(RecurrenceDraft initialDraft, long startTime, long endTime) {
        this.initialDraft = initialDraft;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (!modelClass.isAssignableFrom(RecurrenceConfigViewModel.class)) {
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
        return (T) new RecurrenceConfigViewModel(initialDraft, startTime, endTime);
    }
}
