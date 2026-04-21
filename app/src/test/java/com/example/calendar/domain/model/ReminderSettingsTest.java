package com.example.calendar.domain.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReminderSettingsTest {
    @Test
    public void isInDndWindow_withOvernightRange_coversBothSidesOfMidnight() {
        ReminderSettings settings = new ReminderSettings(true, true, true, true, 22 * 60, 8 * 60, true);

        assertTrue(settings.isInDndWindow(23 * 60 + 30));
        assertTrue(settings.isInDndWindow(7 * 60 + 45));
        assertFalse(settings.isInDndWindow(14 * 60));
    }

    @Test
    public void isReminderBlockedAt_withHighPriorityBypass_allowsHighPriorityOnly() {
        ReminderSettings settings = new ReminderSettings(true, true, true, true, 22 * 60, 8 * 60, true);

        assertFalse(settings.isReminderBlockedAt(23 * 60, true));
        assertTrue(settings.isReminderBlockedAt(23 * 60, false));
    }
}
