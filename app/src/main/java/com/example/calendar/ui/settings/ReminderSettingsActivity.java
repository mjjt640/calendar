package com.example.calendar.ui.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.R;
import com.example.calendar.databinding.ActivityReminderSettingsBinding;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

public class ReminderSettingsActivity extends AppCompatActivity {
    private ActivityReminderSettingsBinding binding;
    private ReminderSettingsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReminderSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new ReminderSettingsViewModelFactory(this))
                .get(ReminderSettingsViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        bindObservers();
        bindActions();
        runEntranceAnimation();
    }

    private void bindObservers() {
        viewModel.getRemindersEnabled().observe(this, value -> {
            updateSwitch(binding.masterReminderSwitch, Boolean.TRUE.equals(value),
                    viewModel::updateRemindersEnabled);
            refreshSectionState();
        });
        viewModel.getSoundEnabled().observe(this, value -> updateSwitch(
                binding.soundSwitch,
                Boolean.TRUE.equals(value),
                viewModel::updateSoundEnabled
        ));
        viewModel.getPopupEnabled().observe(this, value -> updateSwitch(
                binding.popupSwitch,
                Boolean.TRUE.equals(value),
                viewModel::updatePopupEnabled
        ));
        viewModel.getDndEnabled().observe(this, value -> {
            updateSwitch(binding.dndSwitch, Boolean.TRUE.equals(value), viewModel::updateDndEnabled);
            refreshSectionState();
        });
        viewModel.getHighPriorityBypass().observe(this, value -> updateSwitch(
                binding.highPrioritySwitch,
                Boolean.TRUE.equals(value),
                viewModel::updateHighPriorityBypass
        ));
        viewModel.getDndStartText().observe(this, binding.dndStartValue::setText);
        viewModel.getDndEndText().observe(this, binding.dndEndValue::setText);
        viewModel.getSavedMessage().observe(this, message -> {
            if (message == null || message.trim().isEmpty()) {
                return;
            }
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            viewModel.consumeSavedMessage();
        });
    }

    private void bindActions() {
        binding.dndStartRow.setOnClickListener(v -> showTimePicker(
                viewModel.getCurrentDndStartMinutes(),
                viewModel::updateDndStartMinutes
        ));
        binding.dndEndRow.setOnClickListener(v -> showTimePicker(
                viewModel.getCurrentDndEndMinutes(),
                viewModel::updateDndEndMinutes
        ));
        binding.saveButton.setOnClickListener(v -> viewModel.saveSettings());
    }

    private void showTimePicker(int selectedMinutes, @NonNull TimeSelectedListener listener) {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedMinutes / 60)
                .setMinute(selectedMinutes % 60)
                .setTitleText(R.string.reminder_settings_time_picker_title)
                .build();
        picker.addOnPositiveButtonClickListener(v ->
                listener.onSelected(picker.getHour() * 60 + picker.getMinute())
        );
        picker.show(getSupportFragmentManager(), "reminder_time_picker");
    }

    private void refreshSectionState() {
        boolean remindersEnabled = Boolean.TRUE.equals(viewModel.getRemindersEnabled().getValue());
        boolean dndEnabled = Boolean.TRUE.equals(viewModel.getDndEnabled().getValue());
        applySectionState(binding.modeCard, remindersEnabled);
        applySectionState(binding.dndCard, remindersEnabled);
        binding.soundSwitch.setEnabled(remindersEnabled);
        binding.popupSwitch.setEnabled(remindersEnabled);
        binding.dndSwitch.setEnabled(remindersEnabled);
        binding.dndStartRow.setEnabled(remindersEnabled && dndEnabled);
        binding.dndEndRow.setEnabled(remindersEnabled && dndEnabled);
        binding.highPrioritySwitch.setEnabled(remindersEnabled && dndEnabled);
        binding.dndTimeContainer.setAlpha(remindersEnabled && dndEnabled ? 1f : 0.45f);
        binding.highPriorityRow.setAlpha(remindersEnabled && dndEnabled ? 1f : 0.45f);
    }

    private void applySectionState(View target, boolean enabled) {
        target.setAlpha(enabled ? 1f : 0.52f);
    }

    private void updateSwitch(@NonNull SwitchMaterial switchView, boolean checked,
                              @NonNull SwitchChangedListener listener) {
        switchView.setOnCheckedChangeListener(null);
        switchView.setChecked(checked);
        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> listener.onChanged(isChecked));
    }

    private void runEntranceAnimation() {
        View[] targets = new View[]{
                binding.headerCard,
                binding.masterCard,
                binding.modeCard,
                binding.dndCard,
                binding.saveButton
        };
        long startDelay = 0L;
        for (View target : targets) {
            target.setAlpha(0f);
            target.setTranslationY(32f);
            target.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(280L)
                    .setStartDelay(startDelay)
                    .start();
            startDelay += 60L;
        }
    }

    private interface SwitchChangedListener {
        void onChanged(boolean checked);
    }

    private interface TimeSelectedListener {
        void onSelected(int minutes);
    }
}
