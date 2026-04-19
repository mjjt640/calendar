package com.example.calendar.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calendar.R;
import com.example.calendar.databinding.FragmentHomeBinding;
import com.example.calendar.domain.model.Schedule;
import com.example.calendar.ui.schedule.AddScheduleActivity;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private SchedulePreviewAdapter adapter;
    private CalendarMonthAdapter calendarAdapter;
    private HomeViewModel viewModel;
    private ItemTouchHelper itemTouchHelper;
    private boolean dragMoved;
    private boolean sortingInProgress;
    private boolean monthUiInitialized;
    private int pendingMonthAnimationDirection;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this, new HomeViewModelFactory(requireContext()))
                .get(HomeViewModel.class);
        adapter = new SchedulePreviewAdapter(new SchedulePreviewAdapter.Callbacks() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder holder) {
                if (sortingInProgress) {
                    return;
                }
                int position = holder.getBindingAdapterPosition();
                if (adapter.hasRecurringItems() || adapter.isRecurringAt(position)) {
                    showRecurringSortPending();
                    return;
                }
                dragMoved = false;
                itemTouchHelper.startDrag(holder);
            }

            @Override
            public void onLongPress(Schedule schedule, View anchor) {
                if (!sortingInProgress) {
                    showActions(schedule, anchor);
                }
            }
        });
        calendarAdapter = new CalendarMonthAdapter(date -> {
            if (!sortingInProgress) {
                viewModel.selectDate(date);
            }
        });
        itemTouchHelper = new ItemTouchHelper(itemTouchCallback);

        binding.calendarGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        binding.calendarGrid.setAdapter(calendarAdapter);
        binding.scheduleList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.scheduleList.setItemAnimator(new DefaultItemAnimator());
        binding.scheduleList.setAdapter(adapter);
        itemTouchHelper.attachToRecyclerView(binding.scheduleList);

        binding.addScheduleButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddScheduleActivity.class))
        );
        binding.previousMonthButton.setOnClickListener(v -> {
            pendingMonthAnimationDirection = -1;
            viewModel.showPreviousMonth();
        });
        binding.nextMonthButton.setOnClickListener(v -> {
            pendingMonthAnimationDirection = 1;
            viewModel.showNextMonth();
        });
        binding.resetTodayButton.setOnClickListener(v -> viewModel.resetToToday());
        binding.sortByTimeButton.setOnClickListener(v -> animateTimeSort());

        viewModel.getScreenTitleLiveData().observe(getViewLifecycleOwner(), binding.screenTitle::setText);
        viewModel.getMonthState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                return;
            }
            binding.monthTitle.setText(state.getTitle());
            calendarAdapter.submitItems(state.getCells());
            animateMonthChangeIfNeeded();
        });
        viewModel.getSelectedDateLabel().observe(getViewLifecycleOwner(), binding.selectedDateTitle::setText);
        viewModel.getSchedules().observe(getViewLifecycleOwner(), schedules -> {
            if (!sortingInProgress) {
                adapter.submitItems(schedules);
            }
            binding.scheduleEmptyState.setVisibility(schedules == null || schedules.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.ensureSeedData();
        viewModel.loadSchedules();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && !sortingInProgress) {
            viewModel.loadSchedules();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void animateTimeSort() {
        if (sortingInProgress) {
            return;
        }
        if (adapter.hasRecurringItems()) {
            showRecurringSortPending();
            return;
        }
        List<Schedule> targetItems = viewModel.getTimeSortedSchedules();
        if (targetItems.size() <= 1) {
            return;
        }
        sortingInProgress = true;
        binding.sortByTimeButton.setEnabled(false);
        binding.resetTodayButton.setEnabled(false);
        adapter.animateSortTo(targetItems, () -> {
            viewModel.persistManualOrder(adapter.getCurrentItems());
            sortingInProgress = false;
            if (binding != null) {
                binding.sortByTimeButton.setEnabled(true);
                binding.resetTodayButton.setEnabled(true);
            }
        });
    }

    private void showActions(Schedule schedule, View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenu().add(0, 1, 1, getString(R.string.schedule_menu_edit));
        popupMenu.getMenu().add(0, 2, 2, getString(R.string.schedule_menu_delete));
        popupMenu.setOnMenuItemClickListener(item -> handleMenuClick(item, schedule));
        popupMenu.show();
    }

    private void animateMonthChangeIfNeeded() {
        if (binding == null) {
            return;
        }
        if (!monthUiInitialized || pendingMonthAnimationDirection == 0) {
            resetMonthAnimationTargets();
            monthUiInitialized = true;
            pendingMonthAnimationDirection = 0;
            return;
        }

        float offset = pendingMonthAnimationDirection * binding.calendarGrid.getResources().getDisplayMetrics().density * 28f;
        FastOutSlowInInterpolator interpolator = new FastOutSlowInInterpolator();

        animateMonthTarget(binding.monthTitle, offset, interpolator);
        animateMonthTarget(binding.calendarWeekRow, offset, interpolator);
        animateMonthTarget(binding.calendarGrid, offset, interpolator);

        pendingMonthAnimationDirection = 0;
        monthUiInitialized = true;
    }

    private void animateMonthTarget(View target, float offset, FastOutSlowInInterpolator interpolator) {
        target.animate().cancel();
        target.setAlpha(0f);
        target.setTranslationX(offset);
        target.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(220L)
                .setInterpolator(interpolator)
                .start();
    }

    private void resetMonthAnimationTargets() {
        binding.monthTitle.animate().cancel();
        binding.calendarWeekRow.animate().cancel();
        binding.calendarGrid.animate().cancel();

        binding.monthTitle.setAlpha(1f);
        binding.monthTitle.setTranslationX(0f);
        binding.calendarWeekRow.setAlpha(1f);
        binding.calendarWeekRow.setTranslationX(0f);
        binding.calendarGrid.setAlpha(1f);
        binding.calendarGrid.setTranslationX(0f);
    }

    private boolean handleMenuClick(MenuItem item, Schedule schedule) {
        if (schedule.isRecurring()) {
            showRecurringActionPending();
            return true;
        }
        if (item.getItemId() == 1) {
            Intent intent = new Intent(requireContext(), AddScheduleActivity.class);
            intent.putExtra(AddScheduleActivity.EXTRA_SCHEDULE_ID, schedule.getId());
            startActivity(intent);
            return true;
        }
        if (item.getItemId() == 2) {
            viewModel.deleteSchedule(schedule.getId());
            return true;
        }
        return false;
    }

    private void showRecurringActionPending() {
        if (binding == null) {
            return;
        }
        Snackbar.make(binding.getRoot(), R.string.home_recurring_action_pending, Snackbar.LENGTH_SHORT).show();
    }

    private void showRecurringSortPending() {
        if (binding == null) {
            return;
        }
        Snackbar.make(binding.getRoot(), R.string.home_recurring_sort_pending, Snackbar.LENGTH_SHORT).show();
    }

    private final ItemTouchHelper.SimpleCallback itemTouchCallback =
            new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                    0
            ) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                      @NonNull RecyclerView.ViewHolder target) {
                    if (sortingInProgress) {
                        return false;
                    }
                    int from = viewHolder.getBindingAdapterPosition();
                    int to = target.getBindingAdapterPosition();
                    if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                        return false;
                    }
                    dragMoved = true;
                    adapter.moveItem(from, to);
                    return true;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                }

                @Override
                public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                    super.onSelectedChanged(viewHolder, actionState);
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                        View itemView = viewHolder.itemView;
                        ViewCompat.setElevation(itemView, 28f);
                        itemView.animate()
                                .scaleX(1.03f)
                                .scaleY(1.03f)
                                .alpha(0.96f)
                                .setDuration(120L)
                                .start();
                    }
                }

                @Override
                public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    View itemView = viewHolder.itemView;
                    itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .alpha(1f)
                            .setDuration(180L)
                            .start();
                    ViewCompat.setElevation(itemView, 0f);
                    if (dragMoved) {
                        viewModel.persistManualOrder(adapter.getCurrentItems());
                        dragMoved = false;
                    }
                }

                @Override
                public float getMoveThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                    return 0.18f;
                }
            };
}
