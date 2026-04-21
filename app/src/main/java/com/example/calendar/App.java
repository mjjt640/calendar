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
        NotificationChannel channel = new NotificationChannel(
                ScheduleReminderWorker.CHANNEL_ID,
                "日程提醒",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("用于推送即将开始的日程提醒");
        notificationManager.createNotificationChannel(channel);
    }
}
