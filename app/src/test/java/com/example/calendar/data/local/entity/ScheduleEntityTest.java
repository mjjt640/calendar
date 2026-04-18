package com.example.calendar.data.local.entity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ScheduleEntityTest {

    @Test
    public void createDraft_keepsTitleAndDefaultState() {
        ScheduleEntity entity = ScheduleEntity.createDraft("Project review", 1713261600000L, 1713265200000L);

        assertEquals("Project review", entity.title);
        assertEquals("中", entity.priority);
        assertEquals("", entity.location);
        assertEquals("", entity.note);
        assertFalse(entity.completed);
    }
}
