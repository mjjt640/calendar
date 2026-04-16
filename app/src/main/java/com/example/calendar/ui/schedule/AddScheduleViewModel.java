package com.example.calendar.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.domain.model.Schedule;
import com.example.calendar.domain.usecase.AddScheduleUseCase;

public class AddScheduleViewModel extends ViewModel {
    private final AddScheduleUseCase addScheduleUseCase;
    private final MutableLiveData<String> validationMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> savedState = new MutableLiveData<>(false);

    public AddScheduleViewModel(AddScheduleUseCase addScheduleUseCase) {
        this.addScheduleUseCase = addScheduleUseCase;
    }

    public LiveData<String> getValidationMessage() {
        return validationMessage;
    }

    public LiveData<Boolean> getSavedState() {
        return savedState;
    }

    public void saveSchedule(String title, long startTime, long endTime) {
        if (title == null || title.trim().isEmpty()) {
            validationMessage.setValue("Title is required");
            savedState.setValue(false);
            return;
        }

        validationMessage.setValue(null);
        addScheduleUseCase.invoke(new Schedule(title.trim(), startTime, endTime));
        savedState.setValue(true);
    }
}
