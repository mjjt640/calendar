package com.example.calendar.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<String> screenTitle = new MutableLiveData<>("Today Schedule");
    private final MutableLiveData<List<String>> sampleSchedules = new MutableLiveData<>(buildSampleSchedules());

    public String getScreenTitle() {
        return screenTitle.getValue();
    }

    public LiveData<String> getScreenTitleLiveData() {
        return screenTitle;
    }

    public LiveData<List<String>> getSampleSchedules() {
        return sampleSchedules;
    }

    private List<String> buildSampleSchedules() {
        List<String> items = new ArrayList<>();
        items.add("09:00 Product sync");
        items.add("14:00 Study sprint");
        items.add("19:30 Gym reminder");
        return Collections.unmodifiableList(items);
    }
}
