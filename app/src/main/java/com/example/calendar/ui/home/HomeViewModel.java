package com.example.calendar.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.Schedule;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {
    private final ScheduleRepository scheduleRepository;
    private final MutableLiveData<String> screenTitle = new MutableLiveData<>("日程安排");
    private final MutableLiveData<List<Schedule>> schedules = new MutableLiveData<>(new ArrayList<>());

    public HomeViewModel(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public String getScreenTitle() {
        return screenTitle.getValue();
    }

    public LiveData<String> getScreenTitleLiveData() {
        return screenTitle;
    }

    public LiveData<List<Schedule>> getSchedules() {
        return schedules;
    }

    public void loadSchedules() {
        schedules.setValue(scheduleRepository.getOpenSchedules());
    }

    public List<Schedule> getTimeSortedSchedules() {
        return scheduleRepository.getSchedulesOrderedByTime();
    }

    public void persistManualOrder(List<Schedule> reorderedSchedules) {
        scheduleRepository.updateManualOrder(reorderedSchedules);
        schedules.setValue(new ArrayList<>(reorderedSchedules));
    }

    public void deleteSchedule(long id) {
        scheduleRepository.deleteSchedule(id);
        loadSchedules();
    }

    public void ensureSeedData() {
        if (scheduleRepository.getScheduleCount() > 0) {
            return;
        }
        scheduleRepository.addSchedule(new Schedule("团队晨会", 1713498000000L, 1713501600000L));
        scheduleRepository.addSchedule(new Schedule("项目复盘", 1713516000000L, 1713519600000L));
        scheduleRepository.addSchedule(new Schedule("健身提醒", 1713533400000L, 1713537000000L));
    }
}
