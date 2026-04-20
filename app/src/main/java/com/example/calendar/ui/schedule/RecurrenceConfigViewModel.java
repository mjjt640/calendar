package com.example.calendar.ui.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class RecurrenceConfigViewModel extends ViewModel {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(APP_ZONE);

    private static final String SUMMARY_ONE_TIME = "不重复";
    private static final String SUMMARY_DAILY = "每天";
    private static final String SUMMARY_WEEKLY = "每周";
    private static final String SUMMARY_MONTHLY = "每月";
    private static final String UNTIL_DATE_EMPTY = "请选择截止日期";

    private final Long seriesId;
    private final long startTime;
    private final long endTime;
    private final long anchorDayStartTime;
    private final MutableLiveData<RecurrenceFrequency> frequency = new MutableLiveData<>();
    private final MutableLiveData<String> customIntervalUnit = new MutableLiveData<>();
    private final MutableLiveData<Integer> intervalValue = new MutableLiveData<>();
    private final MutableLiveData<RecurrenceDurationType> durationType = new MutableLiveData<>();
    private final MutableLiveData<Long> untilTime = new MutableLiveData<>();
    private final MutableLiveData<Integer> occurrenceCount = new MutableLiveData<>();
    private final MutableLiveData<String> summary = new MutableLiveData<>(SUMMARY_ONE_TIME);
    private final MutableLiveData<Boolean> saveEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> showCustomInterval = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showDurationSection = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showUntilDate = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> showOccurrenceCount = new MutableLiveData<>(false);
    private final MutableLiveData<String> untilDateText = new MutableLiveData<>(UNTIL_DATE_EMPTY);

    public RecurrenceConfigViewModel(RecurrenceDraft initialDraft, long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.anchorDayStartTime = resolveAnchorDayStartTime(startTime);

        RecurrenceDraft draft = normalizeDraft(initialDraft);
        seriesId = draft.getSeriesId();
        frequency.setValue(draft.getFrequency());
        customIntervalUnit.setValue(resolveSafeUnit(draft.getIntervalUnit()));
        intervalValue.setValue(draft.getIntervalValue() > 0 ? draft.getIntervalValue() : 1);
        durationType.setValue(draft.getDurationType());
        untilTime.setValue(draft.getUntilTime());
        occurrenceCount.setValue(draft.getOccurrenceCount());
        refreshDerivedState();
    }

    public LiveData<RecurrenceFrequency> getFrequency() {
        return frequency;
    }

    public LiveData<String> getCustomIntervalUnit() {
        return customIntervalUnit;
    }

    public LiveData<Integer> getIntervalValue() {
        return intervalValue;
    }

    public LiveData<RecurrenceDurationType> getDurationType() {
        return durationType;
    }

    public LiveData<Long> getUntilTime() {
        return untilTime;
    }

    public LiveData<Integer> getOccurrenceCount() {
        return occurrenceCount;
    }

    public LiveData<String> getSummary() {
        return summary;
    }

    public LiveData<Boolean> getSaveEnabled() {
        return saveEnabled;
    }

    public LiveData<Boolean> getShowCustomInterval() {
        return showCustomInterval;
    }

    public LiveData<Boolean> getShowDurationSection() {
        return showDurationSection;
    }

    public LiveData<Boolean> getShowUntilDate() {
        return showUntilDate;
    }

    public LiveData<Boolean> getShowOccurrenceCount() {
        return showOccurrenceCount;
    }

    public LiveData<String> getUntilDateText() {
        return untilDateText;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void updateFrequency(RecurrenceFrequency nextFrequency) {
        frequency.setValue(nextFrequency == null ? RecurrenceFrequency.NONE : nextFrequency);
        refreshDerivedState();
    }

    public void updateCustomIntervalUnit(String nextUnit) {
        customIntervalUnit.setValue(resolveSafeUnit(nextUnit));
        refreshDerivedState();
    }

    public void updateIntervalValue(int nextIntervalValue) {
        intervalValue.setValue(nextIntervalValue);
        refreshDerivedState();
    }

    public void updateDurationType(RecurrenceDurationType nextDurationType) {
        durationType.setValue(nextDurationType == null ? RecurrenceDurationType.NONE : nextDurationType);
        if (valueOrDefault(durationType.getValue(), RecurrenceDurationType.NONE) == RecurrenceDurationType.UNTIL_DATE) {
            untilTime.setValue(normalizeUntilTime(untilTime.getValue()));
        }
        refreshDerivedState();
    }

    public void updateUntilTime(long nextUntilTime) {
        untilTime.setValue(normalizeUntilTime(nextUntilTime > 0L ? nextUntilTime : null));
        refreshDerivedState();
    }

    public void updateOccurrenceCount(int nextOccurrenceCount) {
        occurrenceCount.setValue(nextOccurrenceCount);
        refreshDerivedState();
    }

    public RecurrenceDraft buildResultDraft() {
        RecurrenceFrequency currentFrequency = valueOrDefault(frequency.getValue(), RecurrenceFrequency.NONE);
        if (currentFrequency == RecurrenceFrequency.NONE) {
            return createOneTimeDraft();
        }

        RecurrenceDurationType currentDurationType =
                valueOrDefault(durationType.getValue(), RecurrenceDurationType.NONE);
        Long validatedUntilTime = currentDurationType == RecurrenceDurationType.UNTIL_DATE
                ? normalizeUntilTime(untilTime.getValue())
                : null;
        if (currentDurationType == RecurrenceDurationType.UNTIL_DATE && validatedUntilTime == null) {
            untilTime.setValue(null);
            refreshDerivedState();
            return null;
        }
        if (!Boolean.TRUE.equals(saveEnabled.getValue())) {
            return null;
        }

        String finalUnit = resolveIntervalUnit(currentFrequency);
        int finalIntervalValue = resolveIntervalValue(currentFrequency);
        Integer finalOccurrenceCount = currentDurationType == RecurrenceDurationType.OCCURRENCE_COUNT
                ? occurrenceCount.getValue()
                : null;

        return new RecurrenceDraft(
                true,
                seriesId,
                currentFrequency,
                finalUnit,
                finalIntervalValue,
                currentDurationType,
                validatedUntilTime,
                finalOccurrenceCount
        );
    }

    private void refreshDerivedState() {
        RecurrenceFrequency currentFrequency = valueOrDefault(frequency.getValue(), RecurrenceFrequency.NONE);
        RecurrenceDurationType currentDurationType =
                valueOrDefault(durationType.getValue(), RecurrenceDurationType.NONE);
        Long normalizedUntilTime = currentDurationType == RecurrenceDurationType.UNTIL_DATE
                ? normalizeUntilTime(untilTime.getValue())
                : untilTime.getValue();
        if (currentDurationType == RecurrenceDurationType.UNTIL_DATE
                && !sameLongValue(untilTime.getValue(), normalizedUntilTime)) {
            untilTime.setValue(normalizedUntilTime);
        }
        boolean recurring = currentFrequency != RecurrenceFrequency.NONE;
        boolean custom = recurring && currentFrequency == RecurrenceFrequency.CUSTOM;

        showCustomInterval.setValue(custom);
        showDurationSection.setValue(recurring);
        showUntilDate.setValue(recurring && currentDurationType == RecurrenceDurationType.UNTIL_DATE);
        showOccurrenceCount.setValue(recurring && currentDurationType == RecurrenceDurationType.OCCURRENCE_COUNT);
        untilDateText.setValue(formatUntilDate(normalizedUntilTime));
        saveEnabled.setValue(isSaveEnabled(currentFrequency, currentDurationType, normalizedUntilTime));
        summary.setValue(buildSummary(currentFrequency, currentDurationType, normalizedUntilTime));
    }

    private boolean isSaveEnabled(RecurrenceFrequency currentFrequency,
                                  RecurrenceDurationType currentDurationType,
                                  Long normalizedUntilTime) {
        if (currentFrequency == RecurrenceFrequency.NONE) {
            return true;
        }
        if (currentFrequency == RecurrenceFrequency.CUSTOM && valueOrZero(intervalValue.getValue()) <= 0) {
            return false;
        }
        if (currentDurationType == RecurrenceDurationType.UNTIL_DATE && normalizedUntilTime == null) {
            return false;
        }
        return currentDurationType != RecurrenceDurationType.OCCURRENCE_COUNT
                || valueOrZero(occurrenceCount.getValue()) > 0;
    }

    private String buildSummary(RecurrenceFrequency currentFrequency,
                                RecurrenceDurationType currentDurationType,
                                Long normalizedUntilTime) {
        String baseSummary = buildBaseSummary(currentFrequency);
        if (currentFrequency == RecurrenceFrequency.NONE) {
            return baseSummary;
        }
        if (currentDurationType == RecurrenceDurationType.UNTIL_DATE && normalizedUntilTime != null) {
            return baseSummary + "，截止到 " + formatUntilDate(normalizedUntilTime);
        }
        if (currentDurationType == RecurrenceDurationType.OCCURRENCE_COUNT
                && valueOrZero(occurrenceCount.getValue()) > 0) {
            return baseSummary + "，共 " + occurrenceCount.getValue() + " 次";
        }
        return baseSummary;
    }

    private String buildBaseSummary(RecurrenceFrequency currentFrequency) {
        if (currentFrequency == RecurrenceFrequency.DAILY) {
            return SUMMARY_DAILY;
        }
        if (currentFrequency == RecurrenceFrequency.WEEKLY) {
            return SUMMARY_WEEKLY;
        }
        if (currentFrequency == RecurrenceFrequency.MONTHLY) {
            return SUMMARY_MONTHLY;
        }
        if (currentFrequency == RecurrenceFrequency.CUSTOM) {
            int safeIntervalValue = Math.max(1, valueOrZero(intervalValue.getValue()));
            return "每 " + safeIntervalValue + " " + customUnitLabel(customIntervalUnit.getValue());
        }
        return SUMMARY_ONE_TIME;
    }

    private String resolveIntervalUnit(RecurrenceFrequency currentFrequency) {
        if (currentFrequency == RecurrenceFrequency.DAILY) {
            return RecurrenceDraft.UNIT_DAY;
        }
        if (currentFrequency == RecurrenceFrequency.WEEKLY) {
            return RecurrenceDraft.UNIT_WEEK;
        }
        if (currentFrequency == RecurrenceFrequency.MONTHLY) {
            return RecurrenceDraft.UNIT_MONTH;
        }
        return resolveSafeUnit(customIntervalUnit.getValue());
    }

    private int resolveIntervalValue(RecurrenceFrequency currentFrequency) {
        if (currentFrequency == RecurrenceFrequency.CUSTOM) {
            return Math.max(1, valueOrZero(intervalValue.getValue()));
        }
        return 1;
    }

    private RecurrenceDraft normalizeDraft(RecurrenceDraft draft) {
        if (draft == null || !draft.isRecurring() || draft.getFrequency() == RecurrenceFrequency.NONE) {
            return createOneTimeDraft();
        }

        RecurrenceDurationType normalizedDurationType = draft.getDurationType() == null
                ? RecurrenceDurationType.NONE
                : draft.getDurationType();
        Long normalizedUntilTime = normalizedDurationType == RecurrenceDurationType.UNTIL_DATE
                ? normalizeUntilTime(draft.getUntilTime())
                : null;
        Integer normalizedOccurrenceCount = normalizedDurationType == RecurrenceDurationType.OCCURRENCE_COUNT
                ? draft.getOccurrenceCount()
                : null;

        return new RecurrenceDraft(
                true,
                draft.getSeriesId(),
                draft.getFrequency(),
                resolveSafeUnit(draft.getIntervalUnit()),
                draft.getIntervalValue() > 0 ? draft.getIntervalValue() : 1,
                normalizedDurationType,
                normalizedUntilTime,
                normalizedOccurrenceCount
        );
    }

    private long resolveAnchorDayStartTime(long timeMillis) {
        return Instant.ofEpochMilli(timeMillis)
                .atZone(APP_ZONE)
                .toLocalDate()
                .atStartOfDay(APP_ZONE)
                .toInstant()
                .toEpochMilli();
    }

    private Long normalizeUntilTime(Long candidateUntilTime) {
        if (candidateUntilTime == null || candidateUntilTime < anchorDayStartTime) {
            return null;
        }
        return candidateUntilTime;
    }

    private boolean sameLongValue(Long first, Long second) {
        if (first == null && second == null) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        return first.longValue() == second.longValue();
    }

    private String resolveSafeUnit(String unit) {
        if (RecurrenceDraft.UNIT_WEEK.equals(unit)) {
            return RecurrenceDraft.UNIT_WEEK;
        }
        if (RecurrenceDraft.UNIT_MONTH.equals(unit)) {
            return RecurrenceDraft.UNIT_MONTH;
        }
        return RecurrenceDraft.UNIT_DAY;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private <T> T valueOrDefault(T value, T defaultValue) {
        return value == null ? defaultValue : value;
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

    private String customUnitLabel(String unit) {
        if (RecurrenceDraft.UNIT_WEEK.equals(unit)) {
            return "周";
        }
        if (RecurrenceDraft.UNIT_MONTH.equals(unit)) {
            return "个月";
        }
        return "天";
    }

    private String formatUntilDate(Long timeMillis) {
        if (timeMillis == null) {
            return UNTIL_DATE_EMPTY;
        }
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timeMillis));
    }
}
