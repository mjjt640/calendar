package com.example.calendar.ui.home.calendar;

import org.junit.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CalendarMonthBuilderTest {

    @Test
    public void build_usesCompactFiveWeekGridWhenMonthFits() {
        CalendarMonthState state = new CalendarMonthBuilder().build(
                YearMonth.of(2026, 4),
                LocalDate.of(2026, 4, 18),
                LocalDate.of(2026, 4, 18),
                new HashSet<>()
        );

        assertEquals(35, state.getCells().size());
        assertEquals(LocalDate.of(2026, 3, 30), state.getCells().get(0).getDate());
        assertEquals(LocalDate.of(2026, 5, 3), state.getCells().get(34).getDate());
    }

    @Test
    public void build_usesSixWeekGridWhenMonthSpillsIntoSixthRow() {
        CalendarMonthState state = new CalendarMonthBuilder().build(
                YearMonth.of(2026, 8),
                LocalDate.of(2026, 8, 18),
                LocalDate.of(2026, 8, 18),
                new HashSet<>()
        );

        assertEquals(42, state.getCells().size());
        assertEquals(LocalDate.of(2026, 7, 27), state.getCells().get(0).getDate());
        assertEquals(LocalDate.of(2026, 9, 6), state.getCells().get(41).getDate());
    }

    @Test
    public void build_marksFillerDaysAsOutOfMonth() {
        CalendarMonthState state = new CalendarMonthBuilder().build(
                YearMonth.of(2026, 4),
                LocalDate.of(2026, 4, 18),
                LocalDate.of(2026, 4, 18),
                new HashSet<>()
        );

        CalendarDayCell leadingCell = state.getCells().get(0);
        CalendarDayCell firstMonthCell = findCell(state, LocalDate.of(2026, 4, 1));
        CalendarDayCell trailingCell = state.getCells().get(34);

        assertFalse(leadingCell.isInCurrentMonth());
        assertTrue(firstMonthCell.isInCurrentMonth());
        assertFalse(trailingCell.isInCurrentMonth());
    }

    @Test
    public void build_marksTodaySelectedAndScheduleDots() {
        Set<LocalDate> markers = new HashSet<>();
        markers.add(LocalDate.of(2026, 4, 10));
        markers.add(LocalDate.of(2026, 4, 18));

        CalendarMonthState state = new CalendarMonthBuilder().build(
                YearMonth.of(2026, 4),
                LocalDate.of(2026, 4, 18),
                LocalDate.of(2026, 4, 10),
                markers
        );

        CalendarDayCell selectedCell = findCell(state, LocalDate.of(2026, 4, 10));
        CalendarDayCell todayCell = findCell(state, LocalDate.of(2026, 4, 18));
        CalendarDayCell plainCell = findCell(state, LocalDate.of(2026, 4, 11));

        assertTrue(selectedCell.isSelected());
        assertTrue(selectedCell.hasSchedule());
        assertFalse(selectedCell.isToday());

        assertTrue(todayCell.isToday());
        assertTrue(todayCell.hasSchedule());
        assertFalse(todayCell.isSelected());

        assertFalse(plainCell.isSelected());
        assertFalse(plainCell.isToday());
        assertFalse(plainCell.hasSchedule());
    }

    private CalendarDayCell findCell(CalendarMonthState state, LocalDate date) {
        for (CalendarDayCell cell : state.getCells()) {
            if (date.equals(cell.getDate())) {
                return cell;
            }
        }
        assertNotNull("Expected calendar cell for " + date, null);
        return null;
    }
}
