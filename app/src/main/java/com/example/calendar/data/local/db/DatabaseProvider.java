package com.example.calendar.data.local.db;

import android.content.Context;

import androidx.room.Room;

public final class DatabaseProvider {
    private static volatile AppDatabase instance;

    private DatabaseProvider() {
    }

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseProvider.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "calendar.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}
