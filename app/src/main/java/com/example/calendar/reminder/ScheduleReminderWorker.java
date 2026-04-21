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
import com.example.calendar.ui.schedule.AddScheduleActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScheduleReminderWorker extends Worker {
    public static final String CHANNEL_ID = "calendar_schedule_reminder";
    public static final String KEY_SCHEDULE_ID = "schedule_id";
    public static final String KEY_OCCURRENCE_START_TIME = "occurrence_start_time";
    public static final String KEY_DISPLAY_START_TIME = "display_start_time";
    public static final String KEY_DISPLAY_END_TIME = "display_end_time";
    public static final String KEY_TITLE = "title";
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
        String location = getInputData().getString(KEY_LOCATION);

        if (scheduleId == 0L || displayStartTime == 0L || displayEndTime == 0L) {
            return Result.success();
        }
        if (displayEndTime <= System.currentTimeMillis()) {
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
                    new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle(TextUtils.isEmpty(title) ? "日程提醒" : title)
                            .setContentText(buildContentText(displayStartTime, location))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
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
        String timeText = "开始时间 " + timeFormat.format(new Date(displayStartTime));
        if (TextUtils.isEmpty(location)) {
            return timeText;
        }
        return timeText + " · " + location;
    }
}
