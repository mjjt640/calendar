package com.example.calendar;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import com.example.calendar.reminder.ScheduleReminderWorker;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        createReminderChannel();
    }

    private void createReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }
        notificationManager.createNotificationChannel(createChannel(
                ScheduleReminderWorker.CHANNEL_ID_POPUP_SOUND,
                getString(R.string.schedule_reminder_channel_name_popup_sound),
                NotificationManager.IMPORTANCE_HIGH,
                false
        ));
        notificationManager.createNotificationChannel(createChannel(
                ScheduleReminderWorker.CHANNEL_ID_POPUP_SILENT,
                getString(R.string.schedule_reminder_channel_name_popup_silent),
                NotificationManager.IMPORTANCE_HIGH,
                true
        ));
        notificationManager.createNotificationChannel(createChannel(
                ScheduleReminderWorker.CHANNEL_ID_NORMAL_SOUND,
                getString(R.string.schedule_reminder_channel_name_normal_sound),
                NotificationManager.IMPORTANCE_DEFAULT,
                false
        ));
        notificationManager.createNotificationChannel(createChannel(
                ScheduleReminderWorker.CHANNEL_ID_NORMAL_SILENT,
                getString(R.string.schedule_reminder_channel_name_normal_silent),
                NotificationManager.IMPORTANCE_DEFAULT,
                true
        ));
    }

    private NotificationChannel createChannel(String channelId, String channelName,
                                              int importance, boolean silent) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.setDescription(getString(R.string.schedule_reminder_channel_desc));
        if (silent) {
            channel.setSound(null, null);
            channel.enableVibration(false);
        }
        return channel;
    }
}
