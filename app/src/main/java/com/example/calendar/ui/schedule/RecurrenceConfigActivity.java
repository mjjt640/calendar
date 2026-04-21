package com.example.calendar.ui.schedule;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.method.KeyListener;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.R;
import com.example.calendar.databinding.ActivityRecurrenceConfigBinding;
import com.example.calendar.domain.model.RecurrenceDraft;
import com.example.calendar.domain.model.RecurrenceDurationType;
import com.example.calendar.domain.model.RecurrenceFrequency;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class RecurrenceConfigActivity extends AppCompatActivity {
    public static final String EXTRA_INITIAL_DRAFT = "recurrence_config.initial_draft";
    public static final String EXTRA_START_TIME = "recurrence_config.start_time";
    public static final String EXTRA_END_TIME = "recurrence_config.end_time";
    public static final String EXTRA_RESULT_DRAFT = "recurrence_config.result_draft";

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    private ActivityRecurrenceConfigBinding binding;
    private RecurrenceConfigViewModel viewModel;
    private SelectionDropdownAdapter intervalUnitAdapter;
    private SelectionDropdownAdapter durationAdapter;

    public static Intent createIntent(@NonNull Context context, @Nullable RecurrenceDraft initialDraft,
                                      long startTime, long endTime) {
        Intent intent = new Intent(context, RecurrenceConfigActivity.class);
        intent.putExtra(EXTRA_INITIAL_DRAFT, initialDraft);
        intent.putExtra(EXTRA_START_TIME, startTime);
        intent.putExtra(EXTRA_END_TIME, endTime);
        return intent;
    }

    @Nullable
    public static RecurrenceDraft extractResultDraft(@Nullable Intent data) {
        if (data == null) {
            return null;
        }
        return getSerializableExtra(data, EXTRA_RESULT_DRAFT);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRecurrenceConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        RecurrenceDraft initialDraft = getSerializableExtra(getIntent(), EXTRA_INITIAL_DRAFT);
        long startTime = getIntent().getLongExtra(EXTRA_START_TIME, System.currentTimeMillis());
        long endTime = getIntent().getLongExtra(EXTRA_END_TIME, startTime);
        viewModel = new ViewModelProvider(
                this,
                new RecurrenceConfigViewModelFactory(initialDraft, startTime, endTime)
        ).get(RecurrenceConfigViewModel.class);

        setupToolbar();
        setupFrequencySelector();
        setupCustomSection();
        setupDurationSection();
        setupActions();
        bindViewModel();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupFrequencySelector() {
        binding.frequencyGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.frequency_none_option) {
                viewModel.updateFrequency(RecurrenceFrequency.NONE);
            } else if (checkedId == R.id.frequency_daily_option) {
                viewModel.updateFrequency(RecurrenceFrequency.DAILY);
            } else if (checkedId == R.id.frequency_weekly_option) {
                viewModel.updateFrequency(RecurrenceFrequency.WEEKLY);
            } else if (checkedId == R.id.frequency_monthly_option) {
                viewModel.updateFrequency(RecurrenceFrequency.MONTHLY);
            } else if (checkedId == R.id.frequency_custom_option) {
                viewModel.updateFrequency(RecurrenceFrequency.CUSTOM);
            }
        });
    }

    private void setupCustomSection() {
        intervalUnitAdapter = new SelectionDropdownAdapter(
                getResources().getStringArray(R.array.recurrence_config_unit_labels)
        );
        configureReadOnlyDropdown(binding.intervalUnitInput);
        binding.intervalUnitInput.setAdapter(intervalUnitAdapter);
        binding.intervalUnitInput.setOnClickListener(v -> binding.intervalUnitInput.showDropDown());
        binding.intervalUnitInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.intervalUnitInput.showDropDown();
            }
        });
        binding.intervalUnitInput.setOnItemClickListener((parent, view, position, id) -> {
            intervalUnitAdapter.setSelectedPosition(position);
            viewModel.updateCustomIntervalUnit(unitForPosition(position));
        });

        binding.intervalInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.updateIntervalValue(parseInteger(editable));
            }
        });
    }

    private void setupDurationSection() {
        durationAdapter = new SelectionDropdownAdapter(
                getResources().getStringArray(R.array.recurrence_config_duration_labels)
        );
        configureReadOnlyDropdown(binding.durationTypeInput);
        binding.durationTypeInput.setAdapter(durationAdapter);
        binding.durationTypeInput.setOnClickListener(v -> binding.durationTypeInput.showDropDown());
        binding.durationTypeInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.durationTypeInput.showDropDown();
            }
        });
        binding.durationTypeInput.setOnItemClickListener((parent, view, position, id) -> {
            durationAdapter.setSelectedPosition(position);
            viewModel.updateDurationType(durationForPosition(position));
        });

        binding.untilDateRow.setOnClickListener(v -> showUntilDatePicker());
        binding.occurrenceCountInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                viewModel.updateOccurrenceCount(parseInteger(editable));
            }
        });
    }

    private void setupActions() {
        binding.saveButton.setOnClickListener(v -> {
            RecurrenceDraft result = viewModel.buildResultDraft();
            if (result == null) {
                return;
            }
            Intent data = new Intent().putExtra(EXTRA_RESULT_DRAFT, result);
            setResult(RESULT_OK, data);
            finish();
        });
    }

    private void bindViewModel() {
        viewModel.getSummary().observe(this, binding.summaryValue::setText);
        viewModel.getFrequency().observe(this, this::renderFrequencySelection);
        viewModel.getIntervalValue().observe(this,
                value -> updateEditTextIfNeeded(binding.intervalInput, formatPositiveValue(value)));
        viewModel.getCustomIntervalUnit().observe(this, unit -> {
            binding.intervalUnitInput.setText(labelForUnit(unit), false);
            intervalUnitAdapter.setSelectedPosition(positionForUnit(unit));
        });
        viewModel.getDurationType().observe(this, durationType -> {
            binding.durationTypeInput.setText(labelForDuration(durationType), false);
            durationAdapter.setSelectedPosition(positionForDuration(durationType));
        });
        viewModel.getUntilDateText().observe(this, binding.untilDateValue::setText);
        viewModel.getOccurrenceCount().observe(this,
                value -> updateEditTextIfNeeded(binding.occurrenceCountInput, formatPositiveValue(value)));
        viewModel.getShowCustomInterval().observe(this,
                show -> binding.customSection.setVisibility(Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE));
        viewModel.getShowDurationSection().observe(this,
                show -> binding.durationSection.setVisibility(Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE));
        viewModel.getShowUntilDate().observe(this,
                show -> binding.untilDateRow.setVisibility(Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE));
        viewModel.getShowOccurrenceCount().observe(this,
                show -> binding.occurrenceCountLayout.setVisibility(Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE));
        viewModel.getSaveEnabled().observe(this, enabled -> {
            binding.saveButton.setEnabled(Boolean.TRUE.equals(enabled));
            renderValidationState();
        });

        observeForValidation(viewModel.getShowCustomInterval());
        observeForValidation(viewModel.getIntervalValue());
        observeForValidation(viewModel.getShowOccurrenceCount());
        observeForValidation(viewModel.getOccurrenceCount());
        observeForValidation(viewModel.getShowUntilDate());
        observeForValidation(viewModel.getUntilTime());
    }

    private <T> void observeForValidation(androidx.lifecycle.LiveData<T> liveData) {
        liveData.observe(this, unused -> renderValidationState());
    }

    private void renderFrequencySelection(RecurrenceFrequency frequency) {
        int checkedId;
        if (frequency == RecurrenceFrequency.DAILY) {
            checkedId = R.id.frequency_daily_option;
        } else if (frequency == RecurrenceFrequency.WEEKLY) {
            checkedId = R.id.frequency_weekly_option;
        } else if (frequency == RecurrenceFrequency.MONTHLY) {
            checkedId = R.id.frequency_monthly_option;
        } else if (frequency == RecurrenceFrequency.CUSTOM) {
            checkedId = R.id.frequency_custom_option;
        } else {
            checkedId = R.id.frequency_none_option;
        }
        if (binding.frequencyGroup.getCheckedRadioButtonId() != checkedId) {
            binding.frequencyGroup.check(checkedId);
        }
    }

    private void renderValidationState() {
        boolean showCustomInterval = Boolean.TRUE.equals(viewModel.getShowCustomInterval().getValue());
        int intervalValue = safeValue(viewModel.getIntervalValue().getValue());
        binding.intervalInputLayout.setError(showCustomInterval && intervalValue <= 0
                ? getString(R.string.recurrence_config_interval_error)
                : null);

        boolean showOccurrenceCount = Boolean.TRUE.equals(viewModel.getShowOccurrenceCount().getValue());
        int occurrenceCount = safeValue(viewModel.getOccurrenceCount().getValue());
        binding.occurrenceCountLayout.setError(showOccurrenceCount && occurrenceCount <= 0
                ? getString(R.string.recurrence_config_occurrence_error)
                : null);

        boolean showUntilDate = Boolean.TRUE.equals(viewModel.getShowUntilDate().getValue());
        boolean missingUntilDate = showUntilDate && viewModel.getUntilTime().getValue() == null;
        binding.untilDateHelper.setVisibility(missingUntilDate ? View.VISIBLE : View.GONE);
    }

    private void showUntilDatePicker() {
        Long untilTime = viewModel.getUntilTime().getValue();
        LocalDate initialDate = Instant.ofEpochMilli(
                        untilTime != null ? untilTime : viewModel.getStartTime())
                .atZone(APP_ZONE)
                .toLocalDate();
        long minimumDate = toPickerDateMillis(Instant.ofEpochMilli(viewModel.getStartTime())
                .atZone(APP_ZONE)
                .toLocalDate());
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.recurrence_config_pick_date)
                .setSelection(toPickerDateMillis(initialDate))
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setStart(minimumDate)
                        .setOpenAt(Math.max(minimumDate, toPickerDateMillis(initialDate)))
                        .setValidator(DateValidatorPointForward.from(minimumDate))
                        .build())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            LocalDate selectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            long selectedTime = selectedDate.atStartOfDay(APP_ZONE)
                    .toInstant()
                    .toEpochMilli();
            viewModel.updateUntilTime(selectedTime);
        });
        picker.show(getSupportFragmentManager(), "recurrence_until_date_picker");
    }

    private long atStartOfDay(long timeMillis) {
        return Instant.ofEpochMilli(timeMillis)
                .atZone(APP_ZONE)
                .toLocalDate()
                .atStartOfDay(APP_ZONE)
                .toInstant()
                .toEpochMilli();
    }

    private long toPickerDateMillis(@NonNull LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();
    }

    private int parseInteger(@Nullable CharSequence value) {
        if (value == null) {
            return 0;
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private int safeValue(@Nullable Integer value) {
        return value == null ? 0 : value;
    }

    @NonNull
    private String formatPositiveValue(@Nullable Integer value) {
        return value != null && value > 0 ? String.valueOf(value) : "";
    }

    private void updateEditTextIfNeeded(@NonNull android.widget.EditText editText, @NonNull String value) {
        String current = editText.getText() == null ? "" : editText.getText().toString();
        if (!current.equals(value)) {
            editText.setText(value);
            editText.setSelection(value.length());
        }
    }

    private String labelForUnit(@Nullable String unit) {
        if (RecurrenceDraft.UNIT_WEEK.equals(unit)) {
            return getString(R.string.recurrence_config_unit_week);
        }
        if (RecurrenceDraft.UNIT_MONTH.equals(unit)) {
            return getString(R.string.recurrence_config_unit_month);
        }
        return getString(R.string.recurrence_config_unit_day);
    }

    private String labelForDuration(@Nullable RecurrenceDurationType durationType) {
        if (durationType == RecurrenceDurationType.UNTIL_DATE) {
            return getString(R.string.recurrence_config_duration_until_date);
        }
        if (durationType == RecurrenceDurationType.OCCURRENCE_COUNT) {
            return getString(R.string.recurrence_config_duration_occurrence_count);
        }
        return getString(R.string.recurrence_config_duration_none);
    }

    private String unitForPosition(int position) {
        if (position == 1) {
            return RecurrenceDraft.UNIT_WEEK;
        }
        if (position == 2) {
            return RecurrenceDraft.UNIT_MONTH;
        }
        return RecurrenceDraft.UNIT_DAY;
    }

    private RecurrenceDurationType durationForPosition(int position) {
        if (position == 1) {
            return RecurrenceDurationType.UNTIL_DATE;
        }
        if (position == 2) {
            return RecurrenceDurationType.OCCURRENCE_COUNT;
        }
        return RecurrenceDurationType.NONE;
    }

    private int positionForUnit(@Nullable String unit) {
        if (RecurrenceDraft.UNIT_WEEK.equals(unit)) {
            return 1;
        }
        if (RecurrenceDraft.UNIT_MONTH.equals(unit)) {
            return 2;
        }
        return 0;
    }

    private int positionForDuration(@Nullable RecurrenceDurationType durationType) {
        if (durationType == RecurrenceDurationType.UNTIL_DATE) {
            return 1;
        }
        if (durationType == RecurrenceDurationType.OCCURRENCE_COUNT) {
            return 2;
        }
        return 0;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private static RecurrenceDraft getSerializableExtra(@NonNull Intent intent, @NonNull String key) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getSerializableExtra(key, RecurrenceDraft.class);
        }
        Object extra = intent.getSerializableExtra(key);
        return extra instanceof RecurrenceDraft ? (RecurrenceDraft) extra : null;
    }

    private void configureReadOnlyDropdown(
            @NonNull com.google.android.material.textfield.MaterialAutoCompleteTextView input
    ) {
        KeyListener originalKeyListener = input.getKeyListener();
        input.setTag(originalKeyListener);
        input.setKeyListener(null);
        input.setInputType(0);
        input.setCursorVisible(false);
        input.setLongClickable(false);
        input.setTextIsSelectable(false);
        input.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, android.view.MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private class SelectionDropdownAdapter extends ArrayAdapter<String> {
        private int selectedPosition = -1;

        SelectionDropdownAdapter(@NonNull String[] items) {
            super(RecurrenceConfigActivity.this, R.layout.item_recurrence_dropdown, items);
        }

        void setSelectedPosition(int selectedPosition) {
            this.selectedPosition = selectedPosition;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull android.view.ViewGroup parent) {
            return createOrBindView(position, convertView, parent, false);
        }

        @NonNull
        @Override
        public View getDropDownView(int position, @Nullable View convertView,
                                    @NonNull android.view.ViewGroup parent) {
            return createOrBindView(position, convertView, parent, true);
        }

        private View createOrBindView(int position, @Nullable View convertView,
                                      @NonNull android.view.ViewGroup parent, boolean dropdown) {
            View view = convertView == null
                    ? getLayoutInflater().inflate(R.layout.item_recurrence_dropdown, parent, false)
                    : convertView;
            TextView label = view.findViewById(R.id.dropdown_label);
            ImageView check = view.findViewById(R.id.dropdown_check);
            label.setText(getItem(position));

            boolean isSelected = position == selectedPosition;
            if (dropdown) {
                view.setBackgroundResource(isSelected
                        ? R.drawable.bg_priority_dropdown_item_selected
                        : R.drawable.bg_priority_dropdown_item);
                label.setTextColor(ContextCompat.getColor(
                        RecurrenceConfigActivity.this,
                        isSelected ? R.color.calendar_hero : R.color.text_primary
                ));
                check.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            } else {
                view.setBackgroundResource(R.drawable.bg_priority_dropdown_item);
                label.setTextColor(ContextCompat.getColor(
                        RecurrenceConfigActivity.this,
                        R.color.text_primary
                ));
                check.setVisibility(View.GONE);
            }
            return view;
        }
    }
}
