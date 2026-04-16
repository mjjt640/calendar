package com.example.calendar.ui.home;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HomeViewModelTest {

    @Test
    public void defaultTitle_isTodaySchedule() {
        HomeViewModel viewModel = new HomeViewModel();

        assertEquals("Today Schedule", viewModel.getScreenTitle());
    }
}
