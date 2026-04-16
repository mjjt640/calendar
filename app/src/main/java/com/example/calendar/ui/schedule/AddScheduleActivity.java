package com.example.calendar.ui.schedule;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.databinding.ActivityAddScheduleBinding;
import com.google.android.material.snackbar.Snackbar;

public class AddScheduleActivity extends AppCompatActivity {
    private ActivityAddScheduleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AddScheduleViewModel viewModel = new ViewModelProvider(
                this,
                new AddScheduleViewModelFactory(this)
        ).get(AddScheduleViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.saveButton.setOnClickListener(v -> viewModel.saveSchedule(
                binding.titleInput.getText() == null ? "" : binding.titleInput.getText().toString(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 3600000L
        ));

        viewModel.getValidationMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                binding.titleLayout.setError(message);
            } else {
                binding.titleLayout.setError(null);
            }
        });

        viewModel.getSavedState().observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                Snackbar.make(binding.getRoot(), "Schedule saved", Snackbar.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
