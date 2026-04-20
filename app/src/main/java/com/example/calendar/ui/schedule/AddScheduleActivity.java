package com.example.calendar.ui.schedule;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

public class AddScheduleActivity extends AppCompatActivity {
    public static final String EXTRA_SCHEDULE_ID = "schedule_id";

    private ActivityAddScheduleBinding binding;
    private AddScheduleViewModel viewModel;
    private PriorityDropdownAdapter priorityAdapter;
    private ActivityResultLauncher<Intent> recurrenceConfigLauncher;
    private boolean hasAnimatedRecurrenceCard;
    private String lastRecurrenceSummary;
    @Nullable
    private OccurrenceEditScope pendingRecurrenceScope;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this, new AddScheduleViewModelFactory(this))
                .get(AddScheduleViewModel.class);
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
        binding.recurrenceCard.setOnClickListener(v -> openRecurrenceConfigFlow());
        binding.saveButton.setOnClickListener(v -> viewModel.saveSchedule(
                textOf(binding.titleInput.getText()),
                textOf(binding.locationInput.getText()),
                textOf(binding.noteInput.getText())
        ));
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
                show -> binding.deleteButton.setVisibility(Boolean.TRUE.equals(show) ? View.VISIBLE : View.GONE));
        viewModel.getPriority().observe(this, value -> {
            priorityAdapter.setSelectedPriority(value);
            binding.priorityInput.setText(value, false);
            updatePriorityFieldAppearance(value);
        });
        viewModel.getShowRecurrenceCard().observe(this, this::renderRecurrenceCardVisibility);
        viewModel.getRecurrenceSummary().observe(this, this::renderRecurrenceSummary);
        viewModel.getTitleText().observe(this, value -> updateInputIfNeeded(binding.titleInput, value));
        viewModel.getLocationText().observe(this, value -> updateInputIfNeeded(binding.locationInput, value));
        viewModel.getNoteText().observe(this, value -> updateInputIfNeeded(binding.noteInput, value));
        viewModel.getSavedState().observe(this, saved -> {
            if (Boolean.TRUE.equals(saved)) {
                Snackbar.make(binding.getRoot(), getString(R.string.schedule_saved), Snackbar.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void openRecurrenceConfigFlow() {
        if (!Boolean.TRUE.equals(viewModel.getShowRecurrenceCard().getValue())) {
            return;
        }
        if (Boolean.TRUE.equals(viewModel.getShouldConfirmRecurrenceScope().getValue())) {
            showOccurrenceScopeDialog();
            return;
        }
        launchRecurrenceConfig(null);
    }

    private void showOccurrenceScopeDialog() {
        String[] items = new String[]{
                getString(R.string.recurrence_scope_single),
                getString(R.string.recurrence_scope_this_and_future)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.recurrence_scope_dialog_title)
                .setItems(items, (dialog, which) -> {
                    OccurrenceEditScope scope = which == 1
                            ? OccurrenceEditScope.THIS_AND_FUTURE
                            : OccurrenceEditScope.SINGLE;
                    launchRecurrenceConfig(scope);
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
