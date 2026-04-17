package com.example.calendar.ui.schedule;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.R;
import com.example.calendar.databinding.ActivityAddScheduleBinding;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

public class AddScheduleActivity extends AppCompatActivity {
    public static final String EXTRA_SCHEDULE_ID = "schedule_id";

    private ActivityAddScheduleBinding binding;
    private AddScheduleViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new AddScheduleViewModelFactory(this))
                .get(AddScheduleViewModel.class);

        String[] priorityItems = new String[]{
                getString(R.string.schedule_priority_high),
                getString(R.string.schedule_priority_medium),
                getString(R.string.schedule_priority_low)
        };
        android.widget.ArrayAdapter<String> priorityAdapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                priorityItems
        );
        binding.priorityInput.setAdapter(priorityAdapter);
        binding.priorityInput.setText(getString(R.string.schedule_priority_medium), false);
        binding.priorityInput.setOnItemClickListener((parent, view, position, id) ->
                viewModel.updatePriority((String) parent.getItemAtPosition(position))
        );

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.startTimeRow.setOnClickListener(v -> pickDateTime(true, viewModel.getStartTime()));
        binding.endTimeRow.setOnClickListener(v -> pickDateTime(false, viewModel.getEndTime()));
        binding.saveButton.setOnClickListener(v -> viewModel.saveSchedule(textOf(binding.titleInput.getText())));
        binding.deleteButton.setOnClickListener(v -> viewModel.deleteSchedule());

        long scheduleId = getIntent().getLongExtra(EXTRA_SCHEDULE_ID, 0L);
        if (scheduleId != 0L) {
            viewModel.loadSchedule(scheduleId);
        }

        viewModel.getValidationMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                binding.titleLayout.setError(message);
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            } else {
                binding.titleLayout.setError(null);
            }
        });
        viewModel.getStartTimeText().observe(this, binding.startTimeValue::setText);
        viewModel.getEndTimeText().observe(this, binding.endTimeValue::setText);
        viewModel.getPageTitle().observe(this, binding.toolbar::setTitle);
        viewModel.getSaveButtonText().observe(this, binding.saveButton::setText);
        viewModel.getShowDeleteAction().observe(this,
                show -> binding.deleteButton.setVisibility(Boolean.TRUE.equals(show) ? android.view.View.VISIBLE : android.view.View.GONE));
        viewModel.getPriority().observe(this, value -> binding.priorityInput.setText(value, false));
        viewModel.getTitleText().observe(this, value -> {
            if (value != null && !value.equals(textOf(binding.titleInput.getText()))) {
                binding.titleInput.setText(value);
                binding.titleInput.setSelection(value.length());
            }
        });
        viewModel.getSavedState().observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                Snackbar.make(binding.getRoot(), getString(R.string.schedule_saved), Snackbar.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void pickDateTime(boolean isStartTime, long currentTimeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentTimeMillis);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.setTimeInMillis(currentTimeMillis);
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            this,
                            (timePicker, hourOfDay, minute) -> {
                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);
                                selected.set(Calendar.SECOND, 0);
                                selected.set(Calendar.MILLISECOND, 0);
                                if (isStartTime) {
                                    viewModel.updateStartTime(selected.getTimeInMillis());
                                } else {
                                    viewModel.updateEndTime(selected.getTimeInMillis());
                                }
                            },
                            selected.get(Calendar.HOUR_OF_DAY),
                            selected.get(Calendar.MINUTE),
                            true
                    );
                    localizeDialogButtons(timePickerDialog);
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        localizeDialogButtons(datePickerDialog);
        datePickerDialog.show();
    }

    private void localizeDialogButtons(Dialog dialog) {
        if (dialog instanceof DatePickerDialog) {
            ((DatePickerDialog) dialog).setButton(
                    DatePickerDialog.BUTTON_POSITIVE,
                    getString(R.string.dialog_confirm),
                    (DatePickerDialog) dialog
            );
            ((DatePickerDialog) dialog).setButton(
                    DatePickerDialog.BUTTON_NEGATIVE,
                    getString(R.string.dialog_cancel),
                    (DatePickerDialog) dialog
            );
        } else if (dialog instanceof TimePickerDialog) {
            ((TimePickerDialog) dialog).setButton(
                    TimePickerDialog.BUTTON_POSITIVE,
                    getString(R.string.dialog_confirm),
                    (TimePickerDialog) dialog
            );
            ((TimePickerDialog) dialog).setButton(
                    TimePickerDialog.BUTTON_NEGATIVE,
                    getString(R.string.dialog_cancel),
                    (TimePickerDialog) dialog
            );
        }
    }

    private String textOf(android.text.Editable editable) {
        return editable == null ? "" : editable.toString();
    }
}
