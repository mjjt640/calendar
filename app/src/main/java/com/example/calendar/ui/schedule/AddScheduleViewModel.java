package com.example.calendar.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.OccurrenceEditScope;
import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;
import com.example.calendar.domain.model.Schedule;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AddScheduleViewModel extends ViewModel {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm").withZone(APP_ZONE);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(APP_ZONE);
    private static final String PAGE_TITLE_CREATE = "新建日程";
    private static final String PAGE_TITLE_EDIT = "编辑日程";
    private static final String SAVE_BUTTON_CREATE = "保存日程";
    private static final String SAVE_BUTTON_EDIT = "保存修改";
    private static final String ERROR_TITLE_REQUIRED = "请输入日程标题";
    private static final String ERROR_INVALID_TIME = "结束时间不能早于开始时间";
    private static final String ERROR_RECURRENCE_RULE_UNSUPPORTED = "当前版本暂不支持直接修改重复日程规则";
    private static final String ERROR_RECURRENCE_ENV_UNSUPPORTED = "当前环境暂不支持重复日程保存";
    private static final String RECURRENCE_SUMMARY_SINGLE = "单次";

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
    private final MutableLiveData<RecurrenceDraft> recurrenceDraft =
            new MutableLiveData<>(createOneTimeDraft());
    private final MutableLiveData<Boolean> showRecurrenceCard = new MutableLiveData<>(false);
    private final MutableLiveData<String> recurrenceSummary =
            new MutableLiveData<>(RECURRENCE_SUMMARY_SINGLE);
    private final MutableLiveData<Boolean> isRecurringSchedule = new MutableLiveData<>(false);
    private final MutableLiveData<OccurrenceEditScope> occurrenceEditScope =
            new MutableLiveData<>(OccurrenceEditScope.SINGLE);
    private boolean editingRecurringSeries;
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
        refreshRecurrenceCardState();
        refreshRecurrenceState(recurrenceDraft.getValue());
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

    public LiveData<RecurrenceDraft> getRecurrenceDraft() {
        return recurrenceDraft;
    }

    public LiveData<Boolean> getShowRecurrenceCard() {
        return showRecurrenceCard;
    }

    public LiveData<String> getRecurrenceSummary() {
        return recurrenceSummary;
    }

    public LiveData<Boolean> getIsRecurringSchedule() {
        return isRecurringSchedule;
    }

    public LiveData<OccurrenceEditScope> getOccurrenceEditScope() {
        return occurrenceEditScope;
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
        RecurrenceDraft loadedDraft = scheduleRepository.getRecurrenceDraft(id);
        editingRecurringSeries = loadedDraft != null && loadedDraft.isRecurring();
        refreshRecurrenceState(loadedDraft);
        occurrenceEditScope.setValue(OccurrenceEditScope.SINGLE);
        pageTitle.setValue(PAGE_TITLE_EDIT);
        saveButtonText.setValue(SAVE_BUTTON_EDIT);
        showDeleteAction.setValue(true);
        refreshRecurrenceCardState();
    }

    public void updateStartTime(long startTime) {
        this.startTime = startTime;
        startTimeText.setValue(formatTime(startTime));
        refreshRecurrenceCardState();
    }

    public void updateEndTime(long endTime) {
        this.endTime = endTime;
        endTimeText.setValue(formatTime(endTime));
        refreshRecurrenceCardState();
    }

    public void updatePriority(String nextPriority) {
        priority.setValue(nextPriority);
    }

    public void applyRecurrenceDraft(RecurrenceDraft nextDraft) {
        refreshRecurrenceState(nextDraft);
    }

    public void updateOccurrenceEditScope(OccurrenceEditScope nextScope) {
        occurrenceEditScope.setValue(nextScope == null ? OccurrenceEditScope.SINGLE : nextScope);
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
        RecurrenceDraft currentRecurrenceDraft = recurrenceDraft.getValue() == null
                ? createOneTimeDraft()
                : recurrenceDraft.getValue();
        Schedule scheduleToSave = new Schedule(
                scheduleId,
                trimmedTitle,
                startTime,
                endTime,
                finalPriority,
                scheduleId == 0L ? 0 : sortOrder,
                trimmedLocation,
                trimmedNote
        );
        try {
            if (scheduleId == 0L) {
                if (currentRecurrenceDraft.isRecurring()) {
                    scheduleRepository.addScheduleWithRecurrence(scheduleToSave, currentRecurrenceDraft);
                } else {
                    scheduleRepository.addSchedule(scheduleToSave);
                }
            } else {
                if (editingRecurringSeries || currentRecurrenceDraft.isRecurring()) {
                    scheduleRepository.updateScheduleWithRecurrence(
                            scheduleToSave,
                            currentRecurrenceDraft,
                            occurrenceEditScope.getValue() == null
                                    ? OccurrenceEditScope.SINGLE
                                    : occurrenceEditScope.getValue()
                    );
                } else {
                    scheduleRepository.updateSchedule(scheduleToSave);
                }
                editingRecurringSeries = currentRecurrenceDraft.isRecurring();
            }
        } catch (UnsupportedOperationException exception) {
            validationMessage.setValue(ERROR_RECURRENCE_RULE_UNSUPPORTED);
            savedState.setValue(false);
            return;
        } catch (IllegalStateException exception) {
            validationMessage.setValue(ERROR_RECURRENCE_ENV_UNSUPPORTED);
            savedState.setValue(false);
            return;
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

    private void refreshRecurrenceCardState() {
        showRecurrenceCard.setValue(startTime > 0L && endTime >= startTime);
    }

    private void refreshRecurrenceState(RecurrenceDraft nextDraft) {
        RecurrenceDraft finalDraft = nextDraft == null ? createOneTimeDraft() : nextDraft;
        recurrenceDraft.setValue(finalDraft);
        isRecurringSchedule.setValue(finalDraft.isRecurring());
        recurrenceSummary.setValue(buildRecurrenceSummary(finalDraft));
    }

    private String buildRecurrenceSummary(RecurrenceDraft draft) {
        if (draft == null || !draft.isRecurring() || draft.getFrequency() == RecurrenceFrequency.NONE) {
            return RECURRENCE_SUMMARY_SINGLE;
        }

        String baseSummary;
        if (draft.getFrequency() == RecurrenceFrequency.DAILY) {
            baseSummary = "每日";
        } else if (draft.getFrequency() == RecurrenceFrequency.WEEKLY) {
            baseSummary = "每周";
        } else if (draft.getFrequency() == RecurrenceFrequency.MONTHLY) {
            baseSummary = "每月";
        } else {
            baseSummary = "每 " + Math.max(1, draft.getIntervalValue()) + " " + unitLabel(draft.getIntervalUnit());
        }

        if (draft.getDurationType() == RecurrenceDurationType.UNTIL_DATE && draft.getUntilTime() != null) {
            return baseSummary + "，截止到 "
                    + DATE_FORMATTER.format(Instant.ofEpochMilli(draft.getUntilTime()));
        }
        if (draft.getDurationType() == RecurrenceDurationType.OCCURRENCE_COUNT
                && draft.getOccurrenceCount() != null
                && draft.getOccurrenceCount() > 0) {
            return baseSummary + "，共 " + draft.getOccurrenceCount() + " 次";
        }
        return baseSummary;
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

    private String unitLabel(String intervalUnit) {
        if (RecurrenceDraft.UNIT_WEEK.equals(intervalUnit)) {
            return "周";
        }
        if (RecurrenceDraft.UNIT_MONTH.equals(intervalUnit)) {
            return "月";
        }
        return "天";
    }

    private String formatTime(long timeMillis) {
        return DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(timeMillis));
    }
}
