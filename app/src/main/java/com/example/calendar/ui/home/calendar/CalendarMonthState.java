package com.example.calendar.ui.home.calendar;

import java.util.List;

public class CalendarMonthState {
    private final String title;
    private final List<CalendarDayCell> cells;

    public CalendarMonthState(String title, List<CalendarDayCell> cells) {
        this.title = title;
        this.cells = cells;
    }

    public String getTitle() {
        return title;
    }

    public List<CalendarDayCell> getCells() {
        return cells;
    }
}
