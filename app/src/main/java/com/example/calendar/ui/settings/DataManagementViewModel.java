package com.example.calendar.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.calendar.domain.model.ScheduleBackupOverview;
import com.example.calendar.data.repository.ScheduleBackupRepository;

public class DataManagementViewModel extends ViewModel {
    private final ScheduleBackupRepository repository;
    private final MutableLiveData<ScheduleBackupOverview> overviewLiveData = new MutableLiveData<>();

    public DataManagementViewModel(ScheduleBackupRepository repository) {
        this.repository = repository;
        refreshOverview();
    }

    public LiveData<ScheduleBackupOverview> getOverviewLiveData() {
        return overviewLiveData;
    }

    public void refreshOverview() {
        overviewLiveData.setValue(repository.getOverview());
    }

    public String buildExportJson() {
        return repository.exportBackupJson();
    }

    public ScheduleBackupOverview importBackupJson(String json) {
        ScheduleBackupOverview overview = repository.importBackupJson(json);
        overviewLiveData.setValue(overview);
        return overview;
    }
}
