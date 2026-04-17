package com.example.calendar.ui.home;

import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calendar.R;
import com.example.calendar.databinding.ItemTodayScheduleBinding;
import com.example.calendar.domain.model.Schedule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SchedulePreviewAdapter extends RecyclerView.Adapter<SchedulePreviewAdapter.ViewHolder> {
    public interface Callbacks {
        void onStartDrag(RecyclerView.ViewHolder holder);
        void onLongPress(Schedule schedule, View anchor);
    }

    private static final long SORT_STEP_DELAY_MS = 110L;

    private final List<Schedule> items = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Callbacks callbacks;

    public SchedulePreviewAdapter(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void submitItems(List<Schedule> nextItems) {
        items.clear();
        items.addAll(nextItems);
        notifyDataSetChanged();
    }

    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition == toPosition) {
            return;
        }
        Schedule moved = items.remove(fromPosition);
        items.add(toPosition, moved);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void animateSortTo(List<Schedule> targetItems, Runnable onComplete) {
        animateSortStep(targetItems, 0, onComplete);
    }

    public List<Schedule> getCurrentItems() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTodayScheduleBinding binding = ItemTodayScheduleBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void animateSortStep(List<Schedule> targetItems, int targetIndex, Runnable onComplete) {
        if (targetIndex >= targetItems.size()) {
            onComplete.run();
            return;
        }

        long targetId = targetItems.get(targetIndex).getId();
        int currentIndex = findIndexById(targetId);
        if (currentIndex == -1) {
            animateSortStep(targetItems, targetIndex + 1, onComplete);
            return;
        }

        if (currentIndex != targetIndex) {
            moveItem(currentIndex, targetIndex);
            mainHandler.postDelayed(
                    () -> animateSortStep(targetItems, targetIndex + 1, onComplete),
                    SORT_STEP_DELAY_MS
            );
            return;
        }

        animateSortStep(targetItems, targetIndex + 1, onComplete);
    }

    private int findIndexById(long scheduleId) {
        for (int index = 0; index < items.size(); index++) {
            if (items.get(index).getId() == scheduleId) {
                return index;
            }
        }
        return -1;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTodayScheduleBinding binding;

        ViewHolder(ItemTodayScheduleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Schedule schedule) {
            binding.scheduleTitle.setText(schedule.getTitle());
            binding.scheduleMeta.setText(R.string.home_schedule_meta);
            binding.scheduleTime.setText(timeFormat.format(new Date(schedule.getStartTime())));
            binding.priorityDot.setBackgroundTintList(
                    ColorStateList.valueOf(resolvePriorityColor(schedule.getPriority()))
            );
            binding.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    callbacks.onStartDrag(this);
                    return true;
                }
                return false;
            });
            binding.getRoot().setOnLongClickListener(v -> {
                callbacks.onLongPress(schedule, binding.getRoot());
                return true;
            });
        }

        private int resolvePriorityColor(String priority) {
            if ("高".equals(priority)) {
                return ContextCompat.getColor(binding.getRoot().getContext(), R.color.priority_high);
            }
            if ("低".equals(priority)) {
                return ContextCompat.getColor(binding.getRoot().getContext(), R.color.priority_low);
            }
            return ContextCompat.getColor(binding.getRoot().getContext(), R.color.priority_medium);
        }
    }
}
