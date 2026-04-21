package com.example.calendar.reminder;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.calendar.R;
import com.example.calendar.data.repository.LocalReminderSettingsRepository;
import com.example.calendar.domain.model.ReminderSettings;
import com.example.calendar.domain.model.Schedule;
import com.example.calendar.ui.schedule.AddScheduleActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ScheduleReminderWorker extends Worker {
    public static final String CHANNEL_ID_POPUP_SOUND = "calendar_schedule_reminder_popup_sound";
    public static final String CHANNEL_ID_POPUP_SILENT = "calendar_schedule_reminder_popup_silent";
    public static final String CHANNEL_ID_NORMAL_SOUND = "calendar_schedule_reminder_normal_sound";
    public static final String CHANNEL_ID_NORMAL_SILENT = "calendar_schedule_reminder_normal_silent";
    public static final String KEY_SCHEDULE_ID = "schedule_id";
    public static final String KEY_OCCURRENCE_START_TIME = "occurrence_start_time";
    public static final String KEY_DISPLAY_START_TIME = "display_start_time";
    public static final String KEY_DISPLAY_END_TIME = "display_end_time";
    public static final String KEY_TITLE = "title";
    public static final String KEY_PRIORITY = "priority";
    public static final String KEY_LOCATION = "location";

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);

    public ScheduleReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long scheduleId = getInputData().getLong(KEY_SCHEDULE_ID, 0L);
        long occurrenceStartTime = getInputData().getLong(KEY_OCCURRENCE_START_TIME, 0L);
        long displayStartTime = getInputData().getLong(KEY_DISPLAY_START_TIME, 0L);
        long displayEndTime = getInputData().getLong(KEY_DISPLAY_END_TIME, 0L);
        String title = getInputData().getString(KEY_TITLE);
        String priority = getInputData().getString(KEY_PRIORITY);
        String location = getInputData().getString(KEY_LOCATION);

        if (scheduleId == 0L || displayStartTime == 0L || displayEndTime == 0L) {
            return Result.success();
        }
        if (displayEndTime <= System.currentTimeMillis()) {
            new WorkManagerScheduleReminderCoordinator(getApplicationContext())
                    .syncScheduleReminderAfterOccurrence(scheduleId, occurrenceStartTime);
            return Result.success();
        }

        ReminderSettings settings = new LocalReminderSettingsRepository(getApplicationContext()).getSettings();
        if (!settings.isRemindersEnabled()) {
            return Result.success();
        }
        if (settings.isReminderBlockedAt(currentMinutesOfDay(), Schedule.PRIORITY_HIGH.equals(priority))) {
            new WorkManagerScheduleReminderCoordinator(getApplicationContext())
                    .syncScheduleReminderAfterOccurrence(scheduleId, occurrenceStartTime);
            return Result.success();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(
                getApplicationContext(),
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(getApplicationContext()).notify(
                    (int) scheduleId,
                    new NotificationCompat.Builder(getApplicationContext(), resolveChannelId(settings))
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle(TextUtils.isEmpty(title)
                                    ? getApplicationContext().getString(R.string.schedule_notification_fallback_title)
                                    : title)
                            .setContentText(buildContentText(displayStartTime, location))
                            .setPriority(settings.isPopupEnabled()
                                    ? NotificationCompat.PRIORITY_HIGH
                                    : NotificationCompat.PRIORITY_DEFAULT)
                            .setSilent(!settings.isSoundEnabled())
                            .setAutoCancel(true)
                            .setContentIntent(createContentIntent(
                                    scheduleId,
                                    occurrenceStartTime,
                                    displayStartTime,
                                    displayEndTime
                            ))
                            .build()
            );
        }
        new WorkManagerScheduleReminderCoordinator(getApplicationContext())
                .syncScheduleReminderAfterOccurrence(scheduleId, occurrenceStartTime);
        return Result.success();
    }

    @NonNull
    private PendingIntent createContentIntent(long scheduleId, long occurrenceStartTime,
                                              long displayStartTime, long displayEndTime) {
        Intent intent = new Intent(getApplicationContext(), AddScheduleActivity.class);
        intent.putExtra(AddScheduleActivity.EXTRA_SCHEDULE_ID, scheduleId);
        intent.putExtra(AddScheduleActivity.EXTRA_OCCURRENCE_START_TIME, occurrenceStartTime);
        intent.putExtra(AddScheduleActivity.EXTRA_DISPLAY_START_TIME, displayStartTime);
        intent.putExtra(AddScheduleActivity.EXTRA_DISPLAY_END_TIME, displayEndTime);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(
                getApplicationContext(),
                (int) scheduleId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    @NonNull
    private String buildContentText(long displayStartTime, String location) {
        String timeText = getApplicationContext().getString(
                R.string.schedule_notification_time_prefix,
                timeFormat.format(new Date(displayStartTime))
        );
        if (TextUtils.isEmpty(location)) {
            return timeText;
        }
        return timeText + " · " + location;
    }

    private int currentMinutesOfDay() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
    }

    @NonNull
    private String resolveChannelId(@NonNull ReminderSettings settings) {
        if (settings.isPopupEnabled() && settings.isSoundEnabled()) {
            return CHANNEL_ID_POPUP_SOUND;
        }
        if (settings.isPopupEnabled()) {
            return CHANNEL_ID_POPUP_SILENT;
        }
        if (settings.isSoundEnabled()) {
            return CHANNEL_ID_NORMAL_SOUND;
        }
        return CHANNEL_ID_NORMAL_SILENT;
    }
}
