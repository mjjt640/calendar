package com.example.calendar.data.local.entity;

import com.example.calendar.data.local.dao.RecurrenceDao;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RecurrenceSeriesEntityTest {

    @Test
    public void constructor_keepsCoreRecurrenceFields() {
        RecurrenceSeriesEntity entity = new RecurrenceSeriesEntity(
                7L,
                42L,
                RecurrenceFrequency.WEEKLY,
                "WEEK",
                2,
                1713261600000L,
                1713265200000L,
                RecurrenceDurationType.UNTIL_DATE,
                1714471200000L,
                0
        );

        assertEquals(42L, entity.scheduleId);
        assertEquals(RecurrenceFrequency.WEEKLY, entity.frequency);
        assertEquals("WEEK", entity.intervalUnit);
        assertEquals(2, entity.intervalValue);
        assertEquals(RecurrenceDurationType.UNTIL_DATE, entity.durationType);
        assertEquals(Long.valueOf(1714471200000L), entity.untilTime);
    }

    @Test
    public void recurrenceSeriesSchema_declaresUniqueScheduleConstraint() throws IOException {
        String source = normalizedSource("app/src/main/java/com/example/calendar/data/local/entity/RecurrenceSeriesEntity.java");

        assertTrue(source.contains("indices = {@Index(value = {\"scheduleId\"}, unique = true)}"));
        assertTrue(source.contains("entity = ScheduleEntity.class"));
        assertTrue(source.contains("parentColumns = \"id\""));
        assertTrue(source.contains("childColumns = \"scheduleId\""));
        assertTrue(source.contains("onDelete = ForeignKey.CASCADE"));
    }

    @Test
    public void recurrenceExceptionSchema_usesSeriesScopedOverrideModel() throws Exception {
        Field seriesIdField = RecurrenceExceptionEntity.class.getField("seriesId");
        Field exceptionTypeField = RecurrenceExceptionEntity.class.getField("exceptionType");

        assertNotNull(seriesIdField);
        assertEquals(long.class, seriesIdField.getType());
        assertEquals(String.class, exceptionTypeField.getType());
        assertEquals("DELETE", RecurrenceExceptionEntity.TYPE_DELETE);
        assertEquals("OVERRIDE", RecurrenceExceptionEntity.TYPE_OVERRIDE);

        String source = normalizedSource("app/src/main/java/com/example/calendar/data/local/entity/RecurrenceExceptionEntity.java");
        assertTrue(source.contains("indices = {@Index(value = {\"seriesId\", \"occurrenceStartTime\"}, unique = true)}"));
        assertTrue(source.contains("entity = RecurrenceSeriesEntity.class"));
        assertTrue(source.contains("childColumns = \"seriesId\""));
        assertTrue(source.contains("onDelete = ForeignKey.CASCADE"));
    }

    @Test
    public void recurrenceDao_queriesExceptionsBySeriesWindowAndReplacesDuplicates() throws Exception {
        Method method = RecurrenceDao.class.getMethod(
                "getExceptionsForWindow",
                long.class,
                long.class,
                long.class
        );

        assertEquals(3, method.getParameterCount());

        String source = normalizedSource("app/src/main/java/com/example/calendar/data/local/dao/RecurrenceDao.java");
        assertTrue(source.contains("@Insert(onConflict = OnConflictStrategy.REPLACE)"));
        assertTrue(source.contains("WHERE seriesId = :seriesId"));
        assertTrue(source.contains("occurrenceStartTime >= :windowStartInclusive"));
        assertTrue(source.contains("occurrenceStartTime < :windowEndExclusive"));
    }

    private static String normalizedSource(String relativePath) throws IOException {
        Path base = Paths.get(System.getProperty("user.dir"));
        Path path = Files.exists(base.resolve(relativePath))
                ? base.resolve(relativePath)
                : base.resolve("..").resolve(relativePath).normalize();
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
    }
}
