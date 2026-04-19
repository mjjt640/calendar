package com.example.calendar.data.local.db;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;

public final class DatabaseProvider {
    private static final String DATABASE_NAME = "calendar.db";

    private static volatile AppDatabase instance;

    private DatabaseProvider() {
    }

    static Migration[] getMigrations() {
        return new Migration[]{AppDatabase.MIGRATION_3_4};
    }

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (DatabaseProvider.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).addMigrations(getMigrations())
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
