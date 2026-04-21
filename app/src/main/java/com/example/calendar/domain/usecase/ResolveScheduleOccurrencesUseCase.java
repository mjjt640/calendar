package com.example.calendar.domain.usecase;

import com.example.calendar.data.local.entity.RecurrenceExceptionEntity;
import com.example.calendar.data.local.entity.RecurrenceSeriesEntity;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;
import com.example.calendar.domain.model.Schedule;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResolveScheduleOccurrencesUseCase {
    public List<Schedule> resolve(ScheduleEntity anchor, RecurrenceSeriesEntity series,
                                  List<RecurrenceExceptionEntity> exceptions,
                                  long windowStart, long windowEnd) {
        List<Schedule> results = new ArrayList<>();
        if (anchor == null || series == null || windowEnd <= windowStart) {
            return results;
        }

        Map<Long, RecurrenceExceptionEntity> exceptionByOccurrence = indexExceptions(exceptions);
        long occurrenceStart = series.anchorStartTime > 0 ? series.anchorStartTime : anchor.startTime;
        long occurrenceEnd = series.anchorEndTime > 0 ? series.anchorEndTime : anchor.endTime;
        long originalDuration = Math.max(0L, occurrenceEnd - occurrenceStart);
        int occurrenceIndex = 0;

        while (shouldContinue(series, occurrenceStart, occurrenceIndex) && occurrenceStart < windowEnd) {
            Schedule resolvedOccurrence = resolveOccurrence(
                    anchor,
                    series.id,
                    occurrenceStart,
                    occurrenceEnd,
                    originalDuration,
                    exceptionByOccurrence.get(occurrenceStart)
            );
            if (resolvedOccurrence != null && overlapsWindow(
                    resolvedOccurrence.getStartTime(),
                    resolvedOccurrence.getEndTime(),
                    windowStart,
                    windowEnd
            )) {
                results.add(resolvedOccurrence);
            }

            long[] nextOccurrence = advance(series, occurrenceStart, occurrenceEnd);
            occurrenceStart = nextOccurrence[0];
            occurrenceEnd = nextOccurrence[1];
            occurrenceIndex++;
        }

        return results;
    }

    private Map<Long, RecurrenceExceptionEntity> indexExceptions(List<RecurrenceExceptionEntity> exceptions) {
        Map<Long, RecurrenceExceptionEntity> result = new HashMap<>();
        if (exceptions == null) {
            return result;
        }
        for (RecurrenceExceptionEntity exception : exceptions) {
            result.put(exception.occurrenceStartTime, exception);
        }
        return result;
    }

    private boolean shouldContinue(RecurrenceSeriesEntity series, long occurrenceStart, int occurrenceIndex) {
        if (series.durationType == RecurrenceDurationType.OCCURRENCE_COUNT
                && series.occurrenceCount != null
                && occurrenceIndex >= series.occurrenceCount) {
            return false;
        }
        if (series.durationType == RecurrenceDurationType.UNTIL_DATE
                && series.untilTime != null
                && toDate(occurrenceStart).isAfter(toDate(series.untilTime))) {
            return false;
        }
        return true;
    }

    private long[] advance(RecurrenceSeriesEntity series, long occurrenceStart, long occurrenceEnd) {
        ZonedDateTime start = toZonedDateTime(occurrenceStart);
        ZonedDateTime end = toZonedDateTime(occurrenceEnd);
        int step = Math.max(1, series.intervalValue);

        if (series.frequency == RecurrenceFrequency.WEEKLY) {
            return new long[]{toMillis(start.plusWeeks(step)), toMillis(end.plusWeeks(step))};
        }
        if (series.frequency == RecurrenceFrequency.MONTHLY) {
            return new long[]{toMillis(start.plusMonths(step)), toMillis(end.plusMonths(step))};
        }
        if (series.frequency == RecurrenceFrequency.CUSTOM) {
            return advanceCustom(start, end, step, series.intervalUnit);
        }
        return new long[]{toMillis(start.plusDays(step)), toMillis(end.plusDays(step))};
    }

    private long[] advanceCustom(ZonedDateTime start, ZonedDateTime end, int step, String intervalUnit) {
        if (RecurrenceDraft.UNIT_WEEK.equals(intervalUnit)) {
            return new long[]{toMillis(start.plusWeeks(step)), toMillis(end.plusWeeks(step))};
        }
        if (RecurrenceDraft.UNIT_MONTH.equals(intervalUnit)) {
            return new long[]{toMillis(start.plusMonths(step)), toMillis(end.plusMonths(step))};
        }
        return new long[]{toMillis(start.plusDays(step)), toMillis(end.plusDays(step))};
    }

    private Schedule toOccurrence(ScheduleEntity anchor, long seriesId, long occurrenceStart, long occurrenceEnd) {
        return new Schedule(
                anchor.id,
                anchor.title,
                occurrenceStart,
                occurrenceEnd,
                anchor.priority,
                anchor.sortOrder,
                anchor.location,
                anchor.note,
                true,
                seriesId,
                occurrenceStart,
                anchor.reminderMinutesBefore
        );
    }

    private Schedule resolveOccurrence(ScheduleEntity anchor, long seriesId, long occurrenceStart,
                                       long occurrenceEnd, long originalDuration,
                                       RecurrenceExceptionEntity exception) {
        if (exception == null) {
            return toOccurrence(anchor, seriesId, occurrenceStart, occurrenceEnd);
        }
        if (RecurrenceExceptionEntity.TYPE_DELETE.equals(exception.exceptionType)) {
            return null;
        }
        return toOverrideOccurrence(anchor, seriesId, occurrenceStart, occurrenceEnd, originalDuration, exception);
    }

    public Schedule createOverrideOccurrence(ScheduleEntity anchor, long seriesId,
                                             RecurrenceExceptionEntity exception) {
        if (anchor == null || exception == null
                || !RecurrenceExceptionEntity.TYPE_OVERRIDE.equals(exception.exceptionType)) {
            return null;
        }
        long originalDuration = Math.max(0L, anchor.endTime - anchor.startTime);
        long originalOccurrenceStart = exception.occurrenceStartTime;
        long originalOccurrenceEnd = originalOccurrenceStart + originalDuration;
        return toOverrideOccurrence(
                anchor,
                seriesId,
                originalOccurrenceStart,
                originalOccurrenceEnd,
                originalDuration,
                exception
        );
    }

    private Schedule toOverrideOccurrence(ScheduleEntity anchor, long seriesId, long occurrenceStart,
                                          long occurrenceEnd, long originalDuration,
                                          RecurrenceExceptionEntity exception) {
        long resolvedStart = exception.overrideStartTime != null
                ? exception.overrideStartTime
                : occurrenceStart;
        long resolvedEnd;
        if (exception.overrideEndTime != null) {
            resolvedEnd = exception.overrideEndTime;
        } else if (exception.overrideStartTime != null) {
            resolvedEnd = resolvedStart + originalDuration;
        } else {
            resolvedEnd = occurrenceEnd;
        }

        return new Schedule(
                anchor.id,
                exception.overrideTitle != null ? exception.overrideTitle : anchor.title,
                resolvedStart,
                resolvedEnd,
                exception.overridePriority != null ? exception.overridePriority : anchor.priority,
                anchor.sortOrder,
                exception.overrideLocation != null ? exception.overrideLocation : anchor.location,
                exception.overrideNote != null ? exception.overrideNote : anchor.note,
                true,
                seriesId,
                occurrenceStart,
                anchor.reminderMinutesBefore
        );
    }

    public boolean overlapsWindow(long occurrenceStart, long occurrenceEnd, long windowStart, long windowEnd) {
        return occurrenceEnd > windowStart && occurrenceStart < windowEnd;
    }

    private ZonedDateTime toZonedDateTime(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault());
    }

    private java.time.LocalDate toDate(long epochMillis) {
        return toZonedDateTime(epochMillis).toLocalDate();
    }

    private long toMillis(ZonedDateTime dateTime) {
        return dateTime.toInstant().toEpochMilli();
    }
}
