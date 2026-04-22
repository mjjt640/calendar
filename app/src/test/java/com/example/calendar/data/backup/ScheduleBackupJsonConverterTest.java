package com.example.calendar.data.backup;

import com.example.calendar.data.local.entity.RecurrenceExceptionEntity;
import com.example.calendar.data.local.entity.RecurrenceSeriesEntity;
import com.example.calendar.data.local.entity.ScheduleEntity;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;
import com.example.calendar.domain.model.Schedule;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScheduleBackupJsonConverterTest {
    @Test
    public void toJsonAndFromJson_keepScheduleAndRecurrenceData() {
        ScheduleBackupPayload payload = new ScheduleBackupPayload();
        payload.exportedAt = 1710000000000L;
        payload.schedules.add(new ScheduleEntity(
                1L,
                "团队晨会",
                1710000000000L,
                1710003600000L,
                Schedule.PRIORITY_HIGH,
                1,
                false,
                "会议室 A",
                "同步本周进度",
                15
        ));
        payload.recurrenceSeries.add(new RecurrenceSeriesEntity(
                11L,
                1L,
                RecurrenceFrequency.WEEKLY,
                "week",
                1,
                1710000000000L,
                1710003600000L,
                RecurrenceDurationType.NONE,
                null,
                null
        ));
        payload.recurrenceExceptions.add(new RecurrenceExceptionEntity(
                21L,
                11L,
                1710604800000L,
                RecurrenceExceptionEntity.TYPE_OVERRIDE,
                "改到周二晨会",
                1710691200000L,
                1710694800000L,
                Schedule.PRIORITY_MEDIUM,
                "线上会议",
                "临时调整"
        ));

        ScheduleBackupJsonConverter converter = new ScheduleBackupJsonConverter();

        String json = converter.toJson(payload);
        ScheduleBackupPayload restored = converter.fromJson(json);

        assertTrue(json.contains("\"schemaVersion\""));
        assertEquals(1, restored.schedules.size());
        assertEquals(1, restored.recurrenceSeries.size());
        assertEquals(1, restored.recurrenceExceptions.size());
        assertEquals("团队晨会", restored.schedules.get(0).title);
        assertEquals(Long.valueOf(1L), Long.valueOf(restored.recurrenceSeries.get(0).scheduleId));
        assertEquals(Long.valueOf(11L), Long.valueOf(restored.recurrenceExceptions.get(0).seriesId));
    }
}
