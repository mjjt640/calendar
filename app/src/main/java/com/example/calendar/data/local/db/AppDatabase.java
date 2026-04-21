package com.example.calendar.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.calendar.data.local.dao.RecurrenceDao;
import com.example.calendar.data.local.dao.ScheduleDao;
import com.example.calendar.data.local.entity.RecurrenceExceptionEntity;
import com.example.calendar.data.local.entity.RecurrenceSeriesEntity;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;

@Database(
        entities = {
                ScheduleEntity.class,
                RecurrenceSeriesEntity.class,
                RecurrenceExceptionEntity.class
        },
        version = 5,
        exportSchema = false
)
@TypeConverters(AppDatabase.Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@androidx.annotation.NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurrence_series` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`scheduleId` INTEGER NOT NULL, "
                            + "`frequency` TEXT NOT NULL, "
                            + "`intervalUnit` TEXT NOT NULL, "
                            + "`intervalValue` INTEGER NOT NULL, "
                            + "`anchorStartTime` INTEGER NOT NULL, "
                            + "`anchorEndTime` INTEGER NOT NULL, "
                            + "`durationType` TEXT NOT NULL, "
                            + "`untilTime` INTEGER, "
                            + "`occurrenceCount` INTEGER, "
                            + "FOREIGN KEY(`scheduleId`) REFERENCES `schedules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
            database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_recurrence_series_scheduleId` "
                            + "ON `recurrence_series` (`scheduleId`)"
            );
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurrence_exceptions` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`seriesId` INTEGER NOT NULL, "
                            + "`occurrenceStartTime` INTEGER NOT NULL, "
                            + "`exceptionType` TEXT, "
                            + "`overrideTitle` TEXT, "
                            + "`overrideStartTime` INTEGER, "
                            + "`overrideEndTime` INTEGER, "
                            + "`overridePriority` TEXT, "
                            + "`overrideLocation` TEXT, "
                            + "`overrideNote` TEXT, "
                            + "FOREIGN KEY(`seriesId`) REFERENCES `recurrence_series`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
            database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_recurrence_exceptions_seriesId_occurrenceStartTime` "
                            + "ON `recurrence_exceptions` (`seriesId`, `occurrenceStartTime`)"
            );
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@androidx.annotation.NonNull SupportSQLiteDatabase database) {
            database.execSQL(
                    "ALTER TABLE `schedules` ADD COLUMN `reminderMinutesBefore` INTEGER NOT NULL DEFAULT -1"
            );
        }
    };

    public abstract ScheduleDao scheduleDao();

    public abstract RecurrenceDao recurrenceDao();

    public static class Converters {
        @TypeConverter
        public static String fromRecurrenceFrequency(RecurrenceFrequency value) {
            return value == null ? null : value.name();
        }

        @TypeConverter
        public static RecurrenceFrequency toRecurrenceFrequency(String value) {
            return value == null ? null : RecurrenceFrequency.valueOf(value);
        }

        @TypeConverter
        public static String fromRecurrenceDurationType(RecurrenceDurationType value) {
            return value == null ? null : value.name();
        }

        @TypeConverter
        public static RecurrenceDurationType toRecurrenceDurationType(String value) {
            return value == null ? null : RecurrenceDurationType.valueOf(value);
        }
    }
}
