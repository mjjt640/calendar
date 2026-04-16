package com.example.calendar.domain.usecase;

import com.example.calendar.data.repository.ScheduleRepository;
import com.example.calendar.domain.model.Schedule;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AddScheduleUseCaseTest {

    @Test
    public void invoke_addsScheduleThroughRepository() {
        FakeScheduleRepository repository = new FakeScheduleRepository();
        AddScheduleUseCase useCase = new AddScheduleUseCase(repository);

        long insertedId = useCase.invoke(new Schedule("Design review", 1713261600000L, 1713265200000L));

        assertEquals(1L, insertedId);
        assertEquals(1, repository.savedSchedules.size());
        assertEquals("Design review", repository.savedSchedules.get(0).getTitle());
    }

    private static class FakeScheduleRepository implements ScheduleRepository {
        private final List<Schedule> savedSchedules = new ArrayList<>();

        @Override
        public long addSchedule(Schedule schedule) {
            savedSchedules.add(schedule);
            return savedSchedules.size();
        }

        @Override
        public List<Schedule> getOpenSchedules() {
            return new ArrayList<>(savedSchedules);
        }

        @Override
        public List<String> getTodaySchedulePreview() {
            List<String> preview = new ArrayList<>();
            for (Schedule schedule : savedSchedules) {
                preview.add(schedule.getTitle());
            }
            return preview;
        }
    }
}
