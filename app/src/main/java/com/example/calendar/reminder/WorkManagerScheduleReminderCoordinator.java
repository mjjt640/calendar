package com.example.calendar.reminder;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.calendar.data.local.dao.RecurrenceDao;
import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.db.AppDatabase;
import com.example.calendar.data.local.db.DatabaseProvider;
import com.example.calendar.data.local.entity.RecurrenceExceptionEntity;
import com.example.calendar.data.local.entity.RecurrenceSeriesEntity;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.Schedule;
import com.example.calendar.domain.usecase.ResolveScheduleOccurrencesUseCase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WorkManagerScheduleReminderCoordinator implements ScheduleReminderCoordinator {
    private static final long SEARCH_WINDOW_DAYS = 45L;
    private static final int SEARCH_WINDOW_LIMIT = 12;

    private final Context context;
    private final ScheduleDao scheduleDao;
    private final RecurrenceDao recurrenceDao;
    private final ResolveScheduleOccurrencesUseCase resolveScheduleOccurrencesUseCase;

    public WorkManagerScheduleReminderCoordinator(@NonNull Context context) {
        this.context = context.getApplicationContext();
        AppDatabase database = DatabaseProvider.getInstance(this.context);
        this.scheduleDao = database.scheduleDao();
        this.recurrenceDao = database.recurrenceDao();
        this.resolveScheduleOccurrencesUseCase = new ResolveScheduleOccurrencesUseCase();
    }

    @Override
    public void syncScheduleReminder(long scheduleId) {
        cancelScheduleReminder(scheduleId);
        ReminderCandidate candidate = findNextReminderCandidate(scheduleId, null);
        if (candidate == null) {
            return;
        }
        enqueueReminderWork(candidate);
    }

    @Override
    public void syncScheduleReminderAfterOccurrence(long scheduleId, long occurrenceStartTime) {
        cancelScheduleReminder(scheduleId);
        ReminderCandidate candidate = findNextReminderCandidate(scheduleId, occurrenceStartTime);
        if (candidate == null) {
            return;
        }
        enqueueReminderWork(candidate);
    }

    @Override
    public void cancelScheduleReminder(long scheduleId) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName(scheduleId));
    }

    @Nullable
    private ReminderCandidate findNextReminderCandidate(long scheduleId, @Nullable Long afterOccurrenceStartTime) {
        ScheduleEntity anchor = scheduleDao.getById(scheduleId);
        if (anchor == null || anchor.completed || anchor.reminderMinutesBefore <= Schedule.REMINDER_NONE) {
            return null;
        }
        long now = System.currentTimeMillis();
        RecurrenceSeriesEntity series = recurrenceDao == null ? null : recurrenceDao.getSeriesByScheduleId(scheduleId);
        if (series == null) {
            if (anchor.startTime <= now) {
                return null;
            }
            Schedule schedule = mapOneTimeSchedule(anchor);
            return buildCandidate(schedule, anchor.reminderMinutesBefore);
        }

        Schedule occurrence = findNextRecurringOccurrence(anchor, series, now, afterOccurrenceStartTime);
        if (occurrence == null) {
            return null;
        }
        return buildCandidate(occurrence, anchor.reminderMinutesBefore);
    }

    @Nullable
    private Schedule findNextRecurringOccurrence(@NonNull ScheduleEntity anchor,
                                                 @NonNull RecurrenceSeriesEntity series,
                                                 long now,
                                                 @Nullable Long afterOccurrenceStartTime) {
        long originalDuration = Math.max(0L, resolveOriginalEndTime(anchor, series)
                - resolveOriginalStartTime(anchor, series));
        long windowStart = now;
        for (int index = 0; index < SEARCH_WINDOW_LIMIT; index++) {
            long windowEnd = toMillis(toZonedDateTime(windowStart).plusDays(SEARCH_WINDOW_DAYS));
            long sourceWindowStart = Math.max(0L, windowStart - originalDuration);
            List<RecurrenceExceptionEntity> exceptions =
                    recurrenceDao.getExceptionsForWindow(series.id, sourceWindowStart, windowEnd);
            List<Schedule> occurrences = new ArrayList<>(resolveScheduleOccurrencesUseCase.resolve(
                    anchor,
                    series,
                    exceptions,
                    windowStart,
                    windowEnd
            ));
            addMovedInOverrides(occurrences, anchor, series.id, windowStart, windowEnd);
            occurrences.sort(Comparator.comparingLong(Schedule::getStartTime));
            for (Schedule occurrence : occurrences) {
                if (occurrence.getStartTime() <= now) {
                    continue;
                }
                if (afterOccurrenceStartTime != null
                        && occurrence.getOccurrenceStartTime() != null
                        && occurrence.getOccurrenceStartTime() <= afterOccurrenceStartTime) {
                    continue;
                }
                return occurrence;
            }
            windowStart = windowEnd;
        }
        return null;
    }

    private void addMovedInOverrides(@NonNull List<Schedule> occurrences,
                                     @NonNull ScheduleEntity anchor,
                                     long seriesId,
                                     long windowStart,
                                     long windowEnd) {
        List<RecurrenceExceptionEntity> overlappingOverrides =
                recurrenceDao.getOverrideExceptionsOverlappingWindow(seriesId, windowStart, windowEnd);
        Set<String> keys = new HashSet<>();
        for (Schedule occurrence : occurrences) {
            keys.add(keyOf(occurrence.getRecurrenceSeriesId(), occurrence.getOccurrenceStartTime()));
        }
        for (RecurrenceExceptionEntity exception : overlappingOverrides) {
            String key = keyOf(seriesId, exception.occurrenceStartTime);
            if (!keys.add(key)) {
                continue;
            }
            Schedule overrideOccurrence =
                    resolveScheduleOccurrencesUseCase.createOverrideOccurrence(anchor, seriesId, exception);
            if (overrideOccurrence != null && overrideOccurrence.getStartTime() < windowEnd
                    && overrideOccurrence.getEndTime() > windowStart) {
                occurrences.add(overrideOccurrence);
            }
        }
    }

    @NonNull
    private Schedule mapOneTimeSchedule(@NonNull ScheduleEntity entity) {
        return new Schedule(
                entity.id,
                entity.title,
                entity.startTime,
                entity.endTime,
                entity.priority,
                entity.sortOrder,
                entity.location,
                entity.note,
                false,
                null,
                null,
                entity.reminderMinutesBefore
        );
    }

    @Nullable
    private ReminderCandidate buildCandidate(@NonNull Schedule schedule, int reminderMinutesBefore) {
        long triggerAtMillis = schedule.getStartTime() - TimeUnit.MINUTES.toMillis(reminderMinutesBefore);
        long now = System.currentTimeMillis();
        if (schedule.getStartTime() <= now) {
            return null;
        }
        return new ReminderCandidate(
                schedule.getId(),
                schedule.getOccurrenceStartTime() == null ? schedule.getStartTime() : schedule.getOccurrenceStartTime(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getTitle(),
                schedule.getLocation(),
                Math.max(0L, triggerAtMillis - now)
        );
    }

    private void enqueueReminderWork(@NonNull ReminderCandidate candidate) {
        Data inputData = new Data.Builder()
                .putLong(ScheduleReminderWorker.KEY_SCHEDULE_ID, candidate.scheduleId)
                .putLong(ScheduleReminderWorker.KEY_OCCURRENCE_START_TIME, candidate.occurrenceStartTime)
                .putLong(ScheduleReminderWorker.KEY_DISPLAY_START_TIME, candidate.displayStartTime)
                .putLong(ScheduleReminderWorker.KEY_DISPLAY_END_TIME, candidate.displayEndTime)
                .putString(ScheduleReminderWorker.KEY_TITLE, candidate.title)
                .putString(ScheduleReminderWorker.KEY_LOCATION, candidate.location)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ScheduleReminderWorker.class)
                .setInitialDelay(candidate.delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(uniqueWorkName(candidate.scheduleId))
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueWorkName(candidate.scheduleId),
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    @NonNull
    public static String uniqueWorkName(long scheduleId) {
        return "schedule-reminder-" + scheduleId;
    }

    private long resolveOriginalStartTime(@NonNull ScheduleEntity entity, @NonNull RecurrenceSeriesEntity series) {
        return series.anchorStartTime > 0 ? series.anchorStartTime : entity.startTime;
    }

    private long resolveOriginalEndTime(@NonNull ScheduleEntity entity, @NonNull RecurrenceSeriesEntity series) {
        return series.anchorEndTime > 0 ? series.anchorEndTime : entity.endTime;
    }

    @NonNull
    private String keyOf(@Nullable Long seriesId, @Nullable Long occurrenceStartTime) {
        return String.valueOf(seriesId) + "#" + String.valueOf(occurrenceStartTime);
    }

    @NonNull
    private ZonedDateTime toZonedDateTime(long timeMillis) {
        return Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault());
    }

    private long toMillis(@NonNull ZonedDateTime dateTime) {
        return dateTime.toInstant().toEpochMilli();
    }

    private static final class ReminderCandidate {
        private final long scheduleId;
        private final long occurrenceStartTime;
        private final long displayStartTime;
        private final long displayEndTime;
        private final String title;
        private final String location;
        private final long delayMillis;

        private ReminderCandidate(long scheduleId, long occurrenceStartTime, long displayStartTime,
                                  long displayEndTime, @NonNull String title, @Nullable String location,
                                  long delayMillis) {
            this.scheduleId = scheduleId;
            this.occurrenceStartTime = occurrenceStartTime;
            this.displayStartTime = displayStartTime;
            this.displayEndTime = displayEndTime;
            this.title = title;
            this.location = location == null ? "" : location;
            this.delayMillis = delayMillis;
        }
    }
}
