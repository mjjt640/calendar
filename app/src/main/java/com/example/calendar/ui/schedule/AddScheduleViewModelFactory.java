package com.example.calendar.ui.schedule;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.data.local.db.DatabaseProvider;
import com.example.calendar.data.repository.LocalScheduleRepository;
import com.example.calendar.domain.usecase.AddScheduleUseCase;

public class AddScheduleViewModelFactory implements ViewModelProvider.Factory {
    private final android.content.Context context;

    public AddScheduleViewModelFactory(android.content.Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        LocalScheduleRepository repository = new LocalScheduleRepository(
                DatabaseProvider.getInstance(context).scheduleDao()
        );
        return (T) new AddScheduleViewModel(new AddScheduleUseCase(repository));
    }
}
