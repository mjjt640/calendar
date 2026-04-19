package com.example.calendar.domain.model;

import java.io.Serializable;

public class RecurrenceDraft implements Serializable {
    public static final String UNIT_DAY = "DAY";
    public static final String UNIT_WEEK = "WEEK";
    public static final String UNIT_MONTH = "MONTH";

    private static final long serialVersionUID = 1L;

    private final boolean recurring;
    private final Long seriesId;
    private final RecurrenceFrequency frequency;
    private final String intervalUnit;
    private final int intervalValue;
    private final RecurrenceDurationType durationType;
    private final Long untilTime;
    private final Integer occurrenceCount;

    public RecurrenceDraft(boolean recurring, Long seriesId, RecurrenceFrequency frequency, String intervalUnit,
                           int intervalValue, RecurrenceDurationType durationType, Long untilTime,
                           Integer occurrenceCount) {
        this.recurring = recurring;
        this.seriesId = seriesId;
        this.frequency = frequency == null ? RecurrenceFrequency.NONE : frequency;
        this.intervalUnit = intervalUnit == null ? UNIT_DAY : intervalUnit;
        this.intervalValue = intervalValue;
        this.durationType = durationType == null ? RecurrenceDurationType.NONE : durationType;
        this.untilTime = untilTime;
        this.occurrenceCount = occurrenceCount;
    }

    public boolean isRecurring() {
        return recurring;
    }

    public Long getSeriesId() {
        return seriesId;
    }

    public RecurrenceFrequency getFrequency() {
        return frequency;
    }

    public String getIntervalUnit() {
        return intervalUnit;
    }

    public int getIntervalValue() {
        return intervalValue;
    }

    public RecurrenceDurationType getDurationType() {
        return durationType;
    }

    public Long getUntilTime() {
        return untilTime;
    }

    public Integer getOccurrenceCount() {
        return occurrenceCount;
    }
}
