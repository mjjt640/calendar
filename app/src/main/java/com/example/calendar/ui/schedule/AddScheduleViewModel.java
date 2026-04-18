package com.example.calendar.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.Schedule;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AddScheduleViewModel extends ViewModel {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm").withZone(APP_ZONE);
    private static final String PAGE_TITLE_CREATE = "新建日程";
    private static final String PAGE_TITLE_EDIT = "编辑日程";
    private static final String SAVE_BUTTON_CREATE = "保存日程";
    private static final String SAVE_BUTTON_EDIT = "保存修改";
    private static final String ERROR_TITLE_REQUIRED = "请输入日程标题";
    private static final String ERROR_INVALID_TIME = "结束时间不能早于开始时间";

    private final ScheduleRepository scheduleRepository;
    private final MutableLiveData<String> validationMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> savedState = new MutableLiveData<>(false);
    private final MutableLiveData<String> startTimeText = new MutableLiveData<>();
    private final MutableLiveData<String> endTimeText = new MutableLiveData<>();
    private final MutableLiveData<String> pageTitle = new MutableLiveData<>(PAGE_TITLE_CREATE);
    private final MutableLiveData<String> saveButtonText = new MutableLiveData<>(SAVE_BUTTON_CREATE);
    private final MutableLiveData<Boolean> showDeleteAction = new MutableLiveData<>(false);
    private final MutableLiveData<String> priority = new MutableLiveData<>(Schedule.PRIORITY_MEDIUM);
    private final MutableLiveData<String> titleText = new MutableLiveData<>("");
    private final MutableLiveData<String> locationText = new MutableLiveData<>("");
    private final MutableLiveData<String> noteText = new MutableLiveData<>("");
    private long scheduleId;
    private long startTime;
    private long endTime;
    private int sortOrder;

    public AddScheduleViewModel(ScheduleRepository scheduleRepository) {
        this(scheduleRepository, System.currentTimeMillis(), System.currentTimeMillis() + 3600000L);
    }

    public AddScheduleViewModel(ScheduleRepository scheduleRepository, long startTime, long endTime) {
        this.scheduleRepository = scheduleRepository;
        this.startTime = startTime;
        this.endTime = endTime;
        startTimeText.setValue(formatTime(startTime));
        endTimeText.setValue(formatTime(endTime));
    }

    public LiveData<String> getValidationMessage() {
        return validationMessage;
    }

    public LiveData<Boolean> getSavedState() {
        return savedState;
    }

    public LiveData<String> getStartTimeText() {
        return startTimeText;
    }

    public LiveData<String> getEndTimeText() {
        return endTimeText;
    }

    public LiveData<String> getPageTitle() {
        return pageTitle;
    }

    public LiveData<String> getSaveButtonText() {
        return saveButtonText;
    }

    public LiveData<Boolean> getShowDeleteAction() {
        return showDeleteAction;
    }

    public LiveData<String> getPriority() {
        return priority;
    }

    public LiveData<String> getTitleText() {
        return titleText;
    }

    public LiveData<String> getLocationText() {
        return locationText;
    }

    public LiveData<String> getNoteText() {
        return noteText;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void loadSchedule(long id) {
        Schedule schedule = scheduleRepository.getScheduleById(id);
        if (schedule == null) {
            return;
        }
        scheduleId = schedule.getId();
        startTime = schedule.getStartTime();
        endTime = schedule.getEndTime();
        sortOrder = schedule.getSortOrder();
        titleText.setValue(schedule.getTitle());
        locationText.setValue(schedule.getLocation());
        noteText.setValue(schedule.getNote());
        priority.setValue(schedule.getPriority());
        startTimeText.setValue(formatTime(startTime));
        endTimeText.setValue(formatTime(endTime));
        pageTitle.setValue(PAGE_TITLE_EDIT);
        saveButtonText.setValue(SAVE_BUTTON_EDIT);
        showDeleteAction.setValue(true);
    }

    public void updateStartTime(long startTime) {
        this.startTime = startTime;
        startTimeText.setValue(formatTime(startTime));
    }

    public void updateEndTime(long endTime) {
        this.endTime = endTime;
        endTimeText.setValue(formatTime(endTime));
    }

    public void updatePriority(String nextPriority) {
        priority.setValue(nextPriority);
    }

    public void saveSchedule(String title) {
        saveSchedule(title, "", "");
    }

    public void saveSchedule(String title, String location, String note) {
        if (title == null || title.trim().isEmpty()) {
            validationMessage.setValue(ERROR_TITLE_REQUIRED);
            savedState.setValue(false);
            return;
        }
        if (endTime < startTime) {
            validationMessage.setValue(ERROR_INVALID_TIME);
            savedState.setValue(false);
            return;
        }

        String trimmedTitle = title.trim();
        String trimmedLocation = sanitizeOptionalText(location);
        String trimmedNote = sanitizeOptionalText(note);
        String finalPriority = priority.getValue() == null ? Schedule.PRIORITY_MEDIUM : priority.getValue();
        if (scheduleId == 0L) {
            scheduleRepository.addSchedule(new Schedule(
                    0L,
                    trimmedTitle,
                    startTime,
                    endTime,
                    finalPriority,
                    0,
                    trimmedLocation,
                    trimmedNote
            ));
        } else {
            scheduleRepository.updateSchedule(new Schedule(
                    scheduleId,
                    trimmedTitle,
                    startTime,
                    endTime,
                    finalPriority,
                    sortOrder,
                    trimmedLocation,
                    trimmedNote
            ));
        }
        titleText.setValue(trimmedTitle);
        locationText.setValue(trimmedLocation);
        noteText.setValue(trimmedNote);
        validationMessage.setValue(null);
        savedState.setValue(true);
    }

    public void deleteSchedule() {
        if (scheduleId != 0L) {
            scheduleRepository.deleteSchedule(scheduleId);
            savedState.setValue(true);
        }
    }

    private String sanitizeOptionalText(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatTime(long timeMillis) {
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(timeMillis));
    }
}
