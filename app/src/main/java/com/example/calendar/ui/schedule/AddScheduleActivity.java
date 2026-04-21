package com.example.calendar.ui.schedule;

import android.Manifest;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.R;
import com.example.calendar.databinding.ActivityAddScheduleBinding;
import com.example.calendar.domain.model.OccurrenceEditScope;
import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.Schedule;
import com.example.calendar.reminder.ReminderFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class AddScheduleActivity extends AppCompatActivity {
    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    public static final String EXTRA_OCCURRENCE_START_TIME = "occurrence_start_time";
    public static final String EXTRA_DISPLAY_START_TIME = "display_start_time";
    public static final String EXTRA_DISPLAY_END_TIME = "display_end_time";
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    private ActivityAddScheduleBinding binding;
    private AddScheduleViewModel viewModel;
    private PriorityDropdownAdapter priorityAdapter;
    private ActivityResultLauncher<Intent> recurrenceConfigLauncher;
    private boolean hasAnimatedRecurrenceCard;
    private String lastRecurrenceSummary;
    @Nullable
    private OccurrenceEditScope pendingRecurrenceScope;
    private int pendingSuccessMessageRes = R.string.schedule_saved;
    private boolean pendingSaveAfterPermission;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    public static Intent createEditIntent(android.content.Context context, Schedule schedule) {
        Intent intent = new Intent(context, AddScheduleActivity.class);
        intent.putExtra(EXTRA_SCHEDULE_ID, schedule.getId());
        if (schedule.isRecurring() && schedule.getOccurrenceStartTime() != null) {
            intent.putExtra(EXTRA_OCCURRENCE_START_TIME, schedule.getOccurrenceStartTime());
            intent.putExtra(EXTRA_DISPLAY_START_TIME, schedule.getStartTime());
            intent.putExtra(EXTRA_DISPLAY_END_TIME, schedule.getEndTime());
        }
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new AddScheduleViewModelFactory(this))
                .get(AddScheduleViewModel.class);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!pendingSaveAfterPermission) {
                        return;
                    }
                    pendingSaveAfterPermission = false;
                    if (!granted) {
                        Snackbar.make(
                                binding.getRoot(),
                                R.string.notification_permission_hint,
                                Snackbar.LENGTH_SHORT
                        ).show();
                    }
                    performSave();
                }
        );
        recurrenceConfigLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK) {
                        pendingRecurrenceScope = null;
                        return;
                    }
                    RecurrenceDraft resultDraft = RecurrenceConfigActivity.extractResultDraft(result.getData());
                    if (resultDraft != null) {
                        if (pendingRecurrenceScope != null) {
                            viewModel.confirmRecurrenceScope(pendingRecurrenceScope);
                        }
                        viewModel.applyRecurrenceDraft(resultDraft);
                    }
                    pendingRecurrenceScope = null;
                }
        );

        String[] priorityItems = new String[]{
                getString(R.string.schedule_priority_high),
                getString(R.string.schedule_priority_medium),
                getString(R.string.schedule_priority_low)
        };
        priorityAdapter = new PriorityDropdownAdapter(priorityItems);
        binding.priorityInput.setAdapter(priorityAdapter);
        binding.priorityInput.setText(getString(R.string.schedule_priority_medium), false);
        binding.priorityInput.setOnClickListener(v -> binding.priorityInput.showDropDown());
        binding.priorityInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.priorityInput.showDropDown();
            }
        });
        binding.priorityInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPriority = (String) parent.getItemAtPosition(position);
            priorityAdapter.setSelectedPriority(selectedPriority);
            viewModel.updatePriority(selectedPriority);
        });

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.startTimeRow.setOnClickListener(v -> pickDateTime(true, viewModel.getStartTime()));
        binding.endTimeRow.setOnClickListener(v -> pickDateTime(false, viewModel.getEndTime()));
        binding.reminderRow.setOnClickListener(v -> showReminderOptionsDialog());
        binding.recurrenceCard.setOnClickListener(v -> openRecurrenceConfigFlow());
        binding.saveButton.setOnClickListener(v -> {
            pendingSuccessMessageRes = R.string.schedule_saved;
            maybeRequestNotificationPermissionThenSave();
        });
        binding.deleteButton.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.getShouldConfirmDeleteScope().getValue())) {
                showDeleteScopeDialog();
            } else {
                pendingSuccessMessageRes = R.string.schedule_deleted;
                viewModel.deleteSchedule();
            }
        });

        long scheduleId = getIntent().getLongExtra(EXTRA_SCHEDULE_ID, 0L);
        if (scheduleId != 0L) {
            Long occurrenceStartTime = getIntent().hasExtra(EXTRA_OCCURRENCE_START_TIME)
                    ? getIntent().getLongExtra(EXTRA_OCCURRENCE_START_TIME, 0L)
                    : null;
            Long displayStartTime = getIntent().hasExtra(EXTRA_DISPLAY_START_TIME)
                    ? getIntent().getLongExtra(EXTRA_DISPLAY_START_TIME, 0L)
                    : null;
            Long displayEndTime = getIntent().hasExtra(EXTRA_DISPLAY_END_TIME)
                    ? getIntent().getLongExtra(EXTRA_DISPLAY_END_TIME, 0L)
                    : null;
            viewModel.loadSchedule(scheduleId, occurrenceStartTime, displayStartTime, displayEndTime);
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
                show -> binding.deleteButton.setVisibility(Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE));
        viewModel.getPriority().observe(this, value -> {
            priorityAdapter.setSelectedPriority(value);
            binding.priorityInput.setText(value, false);
            updatePriorityFieldAppearance(value);
        });
        viewModel.getShowRecurrenceCard().observe(this, this::renderRecurrenceCardVisibility);
        viewModel.getRecurrenceSummary().observe(this, this::renderRecurrenceSummary);
        viewModel.getReminderSummary().observe(this, binding.reminderSummaryValue::setText);
        viewModel.getTitleText().observe(this, value -> updateInputIfNeeded(binding.titleInput, value));
        viewModel.getLocationText().observe(this, value -> updateInputIfNeeded(binding.locationInput, value));
        viewModel.getNoteText().observe(this, value -> updateInputIfNeeded(binding.noteInput, value));
        viewModel.getSavedState().observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                Snackbar.make(binding.getRoot(), getString(pendingSuccessMessageRes), Snackbar.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void maybeRequestNotificationPermissionThenSave() {
        Integer reminderMinutes = viewModel.getReminderMinutesBefore().getValue();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && reminderMinutes != null
                && reminderMinutes > Schedule.REMINDER_NONE
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pendingSaveAfterPermission = true;
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        performSave();
    }

    private void performSave() {
        viewModel.saveSchedule(
                textOf(binding.titleInput.getText()),
                textOf(binding.locationInput.getText()),
                textOf(binding.noteInput.getText())
        );
    }

    private void openRecurrenceConfigFlow() {
        if (!Boolean.TRUE.equals(viewModel.getShowRecurrenceCard().getValue())) {
            return;
        }
        if (Boolean.TRUE.equals(viewModel.getShouldConfirmRecurrenceScope().getValue())) {
            showEditScopeDialog();
            return;
        }
        launchRecurrenceConfig(null);
    }

    private void showEditScopeDialog() {
        String[] items = new String[]{
                getString(R.string.recurrence_edit_scope_single),
                getString(R.string.recurrence_edit_scope_this_and_future),
                getString(R.string.recurrence_edit_scope_entire_series)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recurrence_edit_scope_dialog_title)
                .setItems(items, (dialog, which) -> {
                    OccurrenceEditScope scope = which == 2
                            ? OccurrenceEditScope.ENTIRE_SERIES
                            : (which == 1 ? OccurrenceEditScope.THIS_AND_FUTURE : OccurrenceEditScope.SINGLE);
                    launchRecurrenceConfig(scope);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showDeleteScopeDialog() {
        String[] items = new String[]{
                getString(R.string.recurrence_delete_scope_single),
                getString(R.string.recurrence_delete_scope_this_and_future),
                getString(R.string.recurrence_delete_scope_entire_series)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recurrence_delete_scope_dialog_title)
                .setItems(items, (dialog, which) -> {
                    OccurrenceEditScope scope = which == 2
                            ? OccurrenceEditScope.ENTIRE_SERIES
                            : (which == 1 ? OccurrenceEditScope.THIS_AND_FUTURE : OccurrenceEditScope.SINGLE);
                    pendingSuccessMessageRes = R.string.schedule_deleted;
                    viewModel.confirmDeleteScope(scope);
                    viewModel.deleteSchedule();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void launchRecurrenceConfig(@Nullable OccurrenceEditScope scope) {
        pendingRecurrenceScope = scope;
        recurrenceConfigLauncher.launch(RecurrenceConfigActivity.createIntent(
                this,
                viewModel.getRecurrenceDraft().getValue(),
                viewModel.getStartTime(),
                viewModel.getEndTime()
        ));
    }

    private void renderRecurrenceCardVisibility(Boolean show) {
        boolean shouldShow = Boolean.TRUE.equals(show);
        if (!shouldShow) {
            binding.recurrenceCard.animate().cancel();
            binding.recurrenceCard.setVisibility(View.GONE);
            binding.recurrenceCard.setAlpha(1f);
            binding.recurrenceCard.setTranslationY(0f);
            return;
        }
        if (binding.recurrenceCard.getVisibility() == View.VISIBLE) {
            return;
        }
        binding.recurrenceCard.setVisibility(View.VISIBLE);
        if (!hasAnimatedRecurrenceCard) {
            hasAnimatedRecurrenceCard = true;
            binding.recurrenceCard.setAlpha(0f);
            binding.recurrenceCard.setTranslationY(dpToPx(14));
            binding.recurrenceCard.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            binding.recurrenceCard.setAlpha(1f);
            binding.recurrenceCard.setTranslationY(0f);
        }
    }

    private void renderRecurrenceSummary(String summary) {
        String safeSummary = summary == null ? "" : summary;
        binding.recurrenceSummaryChip.setText(safeSummary);
        binding.recurrenceSummaryValue.setText(safeSummary);
        if (lastRecurrenceSummary != null && !lastRecurrenceSummary.equals(safeSummary)) {
            binding.recurrenceSummaryChip.animate().cancel();
            binding.recurrenceSummaryChip.setAlpha(0.78f);
            binding.recurrenceSummaryChip.setScaleX(0.96f);
            binding.recurrenceSummaryChip.setScaleY(0.96f);
            binding.recurrenceSummaryChip.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
        lastRecurrenceSummary = safeSummary;
    }

    private void showReminderOptionsDialog() {
        String[] items = new String[]{
                getString(R.string.add_schedule_reminder_none),
                getString(R.string.add_schedule_reminder_five),
                getString(R.string.add_schedule_reminder_fifteen),
                getString(R.string.add_schedule_reminder_thirty),
                getString(R.string.add_schedule_reminder_sixty),
                getString(R.string.add_schedule_reminder_custom)
        };
        int checkedIndex = resolveReminderCheckedIndex();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_schedule_reminder_title)
                .setSingleChoiceItems(items, checkedIndex, (dialog, which) -> {
                    if (which == items.length - 1) {
                        dialog.dismiss();
                        showCustomReminderDialog();
                        return;
                    }
                    viewModel.updateReminderMinutesBefore(reminderMinutesForIndex(which));
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showCustomReminderDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.add_schedule_reminder_custom_hint);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(resolveCustomReminderText());
        input.setSelection(input.getText().length());

        LinearLayout container = new LinearLayout(this);
        int padding = dpToPx(24);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, dpToPx(12), padding, 0);
        container.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_schedule_reminder_custom_title)
                .setView(container)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                    Integer customMinutes = parseReminderMinutes(input.getText() == null
                            ? ""
                            : input.getText().toString());
                    if (customMinutes == null) {
                        Snackbar.make(
                                binding.getRoot(),
                                R.string.add_schedule_reminder_custom_error,
                                Snackbar.LENGTH_SHORT
                        ).show();
                        return;
                    }
                    viewModel.updateReminderMinutesBefore(customMinutes);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private int resolveReminderCheckedIndex() {
        Integer reminderMinutes = viewModel.getReminderMinutesBefore().getValue();
        int reminderValue = reminderMinutes == null ? Schedule.REMINDER_NONE : reminderMinutes;
        if (reminderValue == 5) {
            return 1;
        }
        if (reminderValue == 15) {
            return 2;
        }
        if (reminderValue == 30) {
            return 3;
        }
        if (reminderValue == 60) {
            return 4;
        }
        if (ReminderFormatter.isPreset(reminderValue)) {
            return 0;
        }
        return 5;
    }

    private int reminderMinutesForIndex(int index) {
        if (index == 1) {
            return 5;
        }
        if (index == 2) {
            return 15;
        }
        if (index == 3) {
            return 30;
        }
        if (index == 4) {
            return 60;
        }
        return Schedule.REMINDER_NONE;
    }

    @NonNull
    private String resolveCustomReminderText() {
        Integer reminderMinutes = viewModel.getReminderMinutesBefore().getValue();
        if (reminderMinutes == null || reminderMinutes <= Schedule.REMINDER_NONE
                || ReminderFormatter.isPreset(reminderMinutes)) {
            return "";
        }
        return String.valueOf(reminderMinutes);
    }

    @Nullable
    private Integer parseReminderMinutes(@NonNull String text) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 1 || value > 10080) {
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void pickDateTime(boolean isStartTime, long currentTimeMillis) {
        LocalDate currentDate = Instant.ofEpochMilli(currentTimeMillis)
                .atZone(APP_ZONE)
                .toLocalDate();
        LocalTime currentTime = Instant.ofEpochMilli(currentTimeMillis)
                .atZone(APP_ZONE)
                .toLocalTime()
                .withSecond(0)
                .withNano(0);

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStartTime ? R.string.add_schedule_start_label : R.string.add_schedule_end_label)
                .setSelection(toPickerDateMillis(currentDate))
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            LocalDate selectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            showTimePicker(isStartTime, selectedDate, currentTime);
        });
        datePicker.show(
                getSupportFragmentManager(),
                isStartTime ? "schedule_start_date_picker" : "schedule_end_date_picker"
        );
    }

    private void showTimePicker(boolean isStartTime, @NonNull LocalDate selectedDate,
                                @NonNull LocalTime initialTime) {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(initialTime.getHour())
                .setMinute(initialTime.getMinute())
                .setTitleText(isStartTime ? R.string.add_schedule_start_label : R.string.add_schedule_end_label)
                .build();
        timePicker.addOnPositiveButtonClickListener(v -> {
            LocalDateTime selectedDateTime = LocalDateTime.of(
                    selectedDate,
                    LocalTime.of(timePicker.getHour(), timePicker.getMinute())
            );
            long timeInMillis = selectedDateTime.atZone(APP_ZONE)
                    .toInstant()
                    .toEpochMilli();
            if (isStartTime) {
                viewModel.updateStartTime(timeInMillis);
            } else {
                viewModel.updateEndTime(timeInMillis);
            }
        });
        timePicker.show(
                getSupportFragmentManager(),
                isStartTime ? "schedule_start_time_picker" : "schedule_end_time_picker"
        );
    }

    private long toPickerDateMillis(@NonNull LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
    }

    private void updateInputIfNeeded(com.google.android.material.textfield.TextInputEditText input, String value) {
        String safeValue = value == null ? "" : value;
        if (!safeValue.equals(textOf(input.getText()))) {
            input.setText(safeValue);
            input.setSelection(safeValue.length());
        }
    }

    private String textOf(android.text.Editable editable) {
        return editable == null ? "" : editable.toString();
    }

    private void updatePriorityFieldAppearance(String priority) {
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setSize(dpToPx(10), dpToPx(10));
        dot.setColor(resolvePriorityColor(priority));
        binding.priorityInput.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
        binding.priorityInput.setCompoundDrawablePadding(dpToPx(10));
    }

    private int resolvePriorityColor(String priority) {
        if (Schedule.PRIORITY_HIGH.equals(priority)) {
            return ContextCompat.getColor(this, R.color.priority_high);
        }
        if (Schedule.PRIORITY_LOW.equals(priority)) {
            return ContextCompat.getColor(this, R.color.priority_low);
        }
        return ContextCompat.getColor(this, R.color.priority_medium);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private class PriorityDropdownAdapter extends ArrayAdapter<String> {
        private final LayoutInflater inflater;
        private String selectedPriority;

        PriorityDropdownAdapter(String[] items) {
            super(AddScheduleActivity.this, R.layout.item_priority_dropdown, items);
            inflater = LayoutInflater.from(AddScheduleActivity.this);
            selectedPriority = getString(R.string.schedule_priority_medium);
        }

        void setSelectedPriority(String selectedPriority) {
            this.selectedPriority = selectedPriority;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createOrBindView(position, convertView, parent, false);
        }

        @NonNull
        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return createOrBindView(position, convertView, parent, true);
        }

        private View createOrBindView(int position, @Nullable View convertView, @NonNull ViewGroup parent,
                                      boolean dropdown) {
            View view = convertView == null
                    ? inflater.inflate(R.layout.item_priority_dropdown, parent, false)
                    : convertView;
            TextView label = view.findViewById(R.id.priority_label);
            View dot = view.findViewById(R.id.priority_dot);
            ImageView check = view.findViewById(R.id.priority_check);

            String item = getItem(position);
            label.setText(item);
            dot.setBackgroundTintList(ColorStateList.valueOf(resolvePriorityColor(item)));

            boolean isSelected = item != null && item.equals(selectedPriority);
            if (dropdown) {
                view.setBackgroundResource(isSelected
                        ? R.drawable.bg_priority_dropdown_item_selected
                        : R.drawable.bg_priority_dropdown_item);
                check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                label.setTextColor(ContextCompat.getColor(
                        AddScheduleActivity.this,
                        isSelected ? R.color.calendar_hero : R.color.text_primary
                ));
            } else {
                view.setBackgroundResource(R.drawable.bg_priority_dropdown_item);
                check.setVisibility(View.GONE);
                label.setTextColor(ContextCompat.getColor(AddScheduleActivity.this, R.color.text_primary));
            }
            return view;
        }

        private int resolvePriorityColor(String priority) {
            return AddScheduleActivity.this.resolvePriorityColor(priority);
        }
    }
}
