package com.example.calendar.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.calendar.data.local.entity.RecurrenceExceptionEntity;
import com.example.calendar.data.local.entity.RecurrenceSeriesEntity;

import java.util.List;

@Dao
public interface RecurrenceDao {
    @Insert
    long insertSeries(RecurrenceSeriesEntity recurrenceSeriesEntity);

    @Update
    void updateSeries(RecurrenceSeriesEntity recurrenceSeriesEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertException(RecurrenceExceptionEntity recurrenceExceptionEntity);

    @Query("SELECT * FROM recurrence_series WHERE scheduleId = :scheduleId LIMIT 1")
    RecurrenceSeriesEntity getSeriesByScheduleId(long scheduleId);

    @Query("SELECT * FROM recurrence_series ORDER BY scheduleId ASC, id ASC")
    List<RecurrenceSeriesEntity> getAllSeries();

    @Query("DELETE FROM recurrence_series WHERE scheduleId = :scheduleId")
    void deleteSeriesByScheduleId(long scheduleId);

    @Query("SELECT * FROM recurrence_exceptions WHERE seriesId = :seriesId ORDER BY occurrenceStartTime ASC, id ASC")
    List<RecurrenceExceptionEntity> getExceptionsForSeries(long seriesId);

    @Query("DELETE FROM recurrence_exceptions WHERE seriesId = :seriesId")
    void deleteExceptionsBySeriesId(long seriesId);

    @Query("SELECT * FROM recurrence_exceptions WHERE seriesId = :seriesId AND occurrenceStartTime >= :windowStartInclusive AND occurrenceStartTime < :windowEndExclusive ORDER BY occurrenceStartTime ASC, id ASC")
    List<RecurrenceExceptionEntity> getExceptionsForWindow(long seriesId, long windowStartInclusive, long windowEndExclusive);

    @Query("SELECT * FROM recurrence_exceptions "
            + "WHERE seriesId = :seriesId "
            + "AND exceptionType = 'OVERRIDE' "
            + "AND overrideStartTime IS NOT NULL "
            + "AND overrideStartTime < :windowEndExclusive "
            + "AND (overrideEndTime IS NULL OR overrideEndTime > :windowStartInclusive) "
            + "ORDER BY overrideStartTime ASC, id ASC")
    List<RecurrenceExceptionEntity> getOverrideExceptionsOverlappingWindow(long seriesId,
                                                                           long windowStartInclusive,
                                                                           long windowEndExclusive);
}
