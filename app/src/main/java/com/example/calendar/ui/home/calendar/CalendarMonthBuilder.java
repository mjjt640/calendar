package com.example.calendar.ui.home.calendar;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarMonthBuilder {
    private static final int MIN_GRID_SIZE = 35;
    private static final DateTimeFormatter MONTH_TITLE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINA);

    public CalendarMonthState build(
            YearMonth visibleMonth,
            LocalDate today,
            LocalDate selectedDate,
            Set<LocalDate> markerDates
    ) {
        LocalDate firstDayOfMonth = visibleMonth.atDay(1);
        int offset = toMondayFirstOffset(firstDayOfMonth.getDayOfWeek());
        LocalDate gridStart = firstDayOfMonth.minusDays(offset);
        int daySpan = offset + visibleMonth.lengthOfMonth();
        int gridSize = Math.max(MIN_GRID_SIZE, ((daySpan + 6) / 7) * 7);
        List<CalendarDayCell> cells = new ArrayList<>(gridSize);

        for (int index = 0; index < gridSize; index++) {
            LocalDate cellDate = gridStart.plusDays(index);
            cells.add(new CalendarDayCell(
                    cellDate,
                    visibleMonth.equals(YearMonth.from(cellDate)),
                    cellDate.equals(today),
                    cellDate.equals(selectedDate),
                    markerDates.contains(cellDate)
            ));
        }

        return new CalendarMonthState(visibleMonth.atDay(1).format(MONTH_TITLE_FORMATTER), cells);
    }

    private int toMondayFirstOffset(DayOfWeek dayOfWeek) {
        return dayOfWeek.getValue() - 1;
    }
}
