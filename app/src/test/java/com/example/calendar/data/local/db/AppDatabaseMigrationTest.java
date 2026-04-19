package com.example.calendar.data.local.db;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AppDatabaseMigrationTest {

    @Test
    public void migration3To4_createsRecurrenceTablesWithoutRebuildingSchedules() {
        List<String> executedSql = new ArrayList<>();
        SupportSQLiteDatabase database = recordingDatabase(executedSql);

        AppDatabase.MIGRATION_3_4.migrate(database);

        assertTrue(containsSql(executedSql, "CREATE TABLE IF NOT EXISTS `recurrence_series`"));
        assertTrue(containsSql(executedSql, "CREATE UNIQUE INDEX IF NOT EXISTS `index_recurrence_series_scheduleId`"));
        assertTrue(containsSql(executedSql, "CREATE TABLE IF NOT EXISTS `recurrence_exceptions`"));
        assertTrue(containsSql(executedSql, "CREATE UNIQUE INDEX IF NOT EXISTS `index_recurrence_exceptions_seriesId_occurrenceStartTime`"));
        assertFalse(containsSql(executedSql, "DROP TABLE"));
        assertFalse(containsSql(executedSql, "CREATE TABLE IF NOT EXISTS `schedules`"));
    }

    @Test
    public void databaseProvider_registersMigration3To4() {
        Migration[] migrations = DatabaseProvider.getMigrations();

        assertTrue(migrations.length > 0);
        assertSame(AppDatabase.MIGRATION_3_4, migrations[0]);
    }

    private static SupportSQLiteDatabase recordingDatabase(List<String> executedSql) {
        return (SupportSQLiteDatabase) Proxy.newProxyInstance(
                SupportSQLiteDatabase.class.getClassLoader(),
                new Class[]{SupportSQLiteDatabase.class},
                (proxy, method, args) -> {
                    if ("execSQL".equals(method.getName()) && args != null && args.length > 0) {
                        executedSql.add(String.valueOf(args[0]));
                        return null;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == long.class) {
                        return 0L;
                    }
                    if (returnType == float.class) {
                        return 0f;
                    }
                    if (returnType == double.class) {
                        return 0d;
                    }
                    return null;
                }
        );
    }

    private static boolean containsSql(List<String> executedSql, String expectedFragment) {
        for (String sql : executedSql) {
            if (sql.contains(expectedFragment)) {
                return true;
            }
        }
        return false;
    }
}
