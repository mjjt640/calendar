package com.example.calendar.ui.home.calendar;

import java.time.LocalDate;

public class CalendarDayCell {
    private final LocalDate date;
    private final int dayOfMonth;
    private final boolean inCurrentMonth;
    private final boolean today;
    private final boolean selected;
    private final boolean hasSchedule;

    public CalendarDayCell(LocalDate date, boolean inCurrentMonth, boolean today, boolean selected, boolean hasSchedule) {
        this.date = date;
        this.dayOfMonth = date.getDayOfMonth();
        this.inCurrentMonth = inCurrentMonth;
        this.today = today;
        this.selected = selected;
        this.hasSchedule = hasSchedule;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public boolean isInCurrentMonth() {
        return inCurrentMonth;
    }

    public boolean isToday() {
        return today;
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean hasSchedule() {
        return hasSchedule;
    }
}
