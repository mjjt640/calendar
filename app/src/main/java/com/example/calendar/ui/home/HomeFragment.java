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
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calendar.R;
import com.example.calendar.databinding.FragmentHomeBinding;
import com.example.calendar.domain.model.Schedule;
import com.example.calendar.ui.schedule.AddScheduleActivity;

import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private SchedulePreviewAdapter adapter;
    private HomeViewModel viewModel;
    private ItemTouchHelper itemTouchHelper;
    private boolean dragMoved;
    private boolean sortingInProgress;

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
        itemTouchHelper = new ItemTouchHelper(itemTouchCallback);

        binding.scheduleList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.scheduleList.setItemAnimator(new DefaultItemAnimator());
        binding.scheduleList.setAdapter(adapter);
        itemTouchHelper.attachToRecyclerView(binding.scheduleList);

        binding.addScheduleButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddScheduleActivity.class))
        );
        binding.sortByTimeButton.setOnClickListener(v -> animateTimeSort());

        viewModel.getScreenTitleLiveData().observe(getViewLifecycleOwner(), binding.screenTitle::setText);
        viewModel.getSchedules().observe(getViewLifecycleOwner(), schedules -> {
            if (!sortingInProgress) {
                adapter.submitItems(schedules);
            }
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
        List<Schedule> targetItems = viewModel.getTimeSortedSchedules();
        if (targetItems.size() <= 1) {
            return;
        }
        sortingInProgress = true;
        binding.sortByTimeButton.setEnabled(false);
        adapter.animateSortTo(targetItems, () -> {
            viewModel.persistManualOrder(adapter.getCurrentItems());
            sortingInProgress = false;
            if (binding != null) {
                binding.sortByTimeButton.setEnabled(true);
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

    private boolean handleMenuClick(MenuItem item, Schedule schedule) {
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
