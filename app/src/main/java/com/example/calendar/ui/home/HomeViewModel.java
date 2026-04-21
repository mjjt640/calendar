package com.example.calendar.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.OccurrenceEditScope;
import com.example.calendar.domain.model.Schedule;
import com.example.calendar.ui.home.calendar.CalendarMonthBuilder;
import com.example.calendar.ui.home.calendar.CalendarMonthState;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeViewModel extends ViewModel {
    private static final DateTimeFormatter SELECTED_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("M月d日安排", Locale.CHINA);

    private final ScheduleRepository scheduleRepository;
    private final Clock clock;
    private final CalendarMonthBuilder monthBuilder;
    private final MutableLiveData<String> screenTitle = new MutableLiveData<>("日程安排");
    private final MutableLiveData<List<Schedule>> schedules = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<CalendarMonthState> monthState = new MutableLiveData<>();
    private final MutableLiveData<String> selectedDateLabel = new MutableLiveData<>();

    private YearMonth visibleMonth;
    private LocalDate selectedDate;

    public HomeViewModel(ScheduleRepository scheduleRepository) {
        this(scheduleRepository, Clock.systemDefaultZone());
    }

    HomeViewModel(ScheduleRepository scheduleRepository, Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.clock = clock;
        this.monthBuilder = new CalendarMonthBuilder();
        resetToToday();
    }

    public String getScreenTitle() {
        return screenTitle.getValue();
    }

    public LiveData<String> getScreenTitleLiveData() {
        return screenTitle;
    }

    public LiveData<List<Schedule>> getSchedules() {
        return schedules;
    }

    public LiveData<CalendarMonthState> getMonthState() {
        return monthState;
    }

    public LiveData<String> getSelectedDateLabel() {
        return selectedDateLabel;
    }

    public void loadSchedules() {
        refreshCalendarState();
    }

    public void showPreviousMonth() {
        visibleMonth = visibleMonth.minusMonths(1);
        if (!visibleMonth.equals(YearMonth.from(selectedDate))) {
            selectedDate = visibleMonth.atDay(1);
        }
        refreshCalendarState();
    }

    public void showNextMonth() {
        visibleMonth = visibleMonth.plusMonths(1);
        if (!visibleMonth.equals(YearMonth.from(selectedDate))) {
            selectedDate = visibleMonth.atDay(1);
        }
        refreshCalendarState();
    }

    public void selectDate(LocalDate date) {
        if (date == null) {
            return;
        }
        visibleMonth = YearMonth.from(date);
        selectedDate = date;
        refreshCalendarState();
    }

    public void resetToToday() {
        LocalDate today = LocalDate.now(clock);
        visibleMonth = YearMonth.from(today);
        selectedDate = today;
        refreshCalendarState();
    }

    public List<Schedule> getTimeSortedSchedules() {
        List<Schedule> result = new ArrayList<>(scheduleRepository.getSchedulesForDay(dayStartMillis(), nextDayStartMillis()));
        result.sort(Comparator.comparingLong(Schedule::getStartTime));
        return result;
    }

    public void persistManualOrder(List<Schedule> reorderedSchedules) {
        scheduleRepository.updateManualOrder(reorderedSchedules);
        schedules.setValue(new ArrayList<>(reorderedSchedules));
        updateSelectedDateLabel();
        rebuildMonthState();
    }

    public void deleteSchedule(long id) {
        scheduleRepository.deleteSchedule(id);
        refreshCalendarState();
    }

    public void deleteSchedule(Schedule schedule, OccurrenceEditScope editScope) {
        if (schedule == null) {
            return;
        }
        if (schedule.isRecurring()) {
            long occurrenceStartTime = schedule.getOccurrenceStartTime() == null
                    ? schedule.getStartTime()
                    : schedule.getOccurrenceStartTime();
            scheduleRepository.deleteScheduleWithRecurrence(
                    schedule.getId(),
                    editScope == null ? OccurrenceEditScope.SINGLE : editScope,
                    occurrenceStartTime
            );
        } else {
            scheduleRepository.deleteSchedule(schedule.getId());
        }
        refreshCalendarState();
    }

    public void ensureSeedData() {
        if (scheduleRepository.getScheduleCount() > 0) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        scheduleRepository.addSchedule(new Schedule(
                "团队晨会",
                toMillis(today.atTime(9, 0)),
                toMillis(today.atTime(9, 30))
        ));
        scheduleRepository.addSchedule(new Schedule(
                "项目复盘",
                toMillis(today.plusDays(2).atTime(14, 0)),
                toMillis(today.plusDays(2).atTime(15, 0))
        ));
        scheduleRepository.addSchedule(new Schedule(
                "下月规划",
                toMillis(today.plusMonths(1).withDayOfMonth(1).atTime(10, 0)),
                toMillis(today.plusMonths(1).withDayOfMonth(1).atTime(11, 0))
        ));
        refreshCalendarState();
    }

    YearMonth getVisibleMonthForTest() {
        return visibleMonth;
    }

    LocalDate getSelectedDateForTest() {
        return selectedDate;
    }

    private void refreshCalendarState() {
        schedules.setValue(scheduleRepository.getSchedulesForDay(dayStartMillis(), nextDayStartMillis()));
        updateSelectedDateLabel();
        rebuildMonthState();
    }

    private void rebuildMonthState() {
        Set<LocalDate> markers = scheduleRepository.getScheduleDayMarkers(monthStartMillis(), nextMonthStartMillis());
        monthState.setValue(monthBuilder.build(
                visibleMonth,
                LocalDate.now(clock),
                selectedDate,
                markers
        ));
    }

    private void updateSelectedDateLabel() {
        selectedDateLabel.setValue(selectedDate.format(SELECTED_DATE_FORMATTER));
    }

    private long dayStartMillis() {
        return toMillis(selectedDate.atStartOfDay());
    }

    private long nextDayStartMillis() {
        return toMillis(selectedDate.plusDays(1).atStartOfDay());
    }

    private long monthStartMillis() {
        return toMillis(visibleMonth.atDay(1).atStartOfDay());
    }

    private long nextMonthStartMillis() {
        return toMillis(visibleMonth.plusMonths(1).atDay(1).atStartOfDay());
    }

    private long toMillis(java.time.LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
