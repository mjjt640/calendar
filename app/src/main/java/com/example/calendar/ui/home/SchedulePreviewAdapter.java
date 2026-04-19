package com.example.calendar.ui.home;

import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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
    private static final long DRAG_START_DELAY_MS = 200L;
    private static final long SORT_STEP_DELAY_MS = 110L;

    public interface Callbacks {
        void onStartDrag(RecyclerView.ViewHolder holder);
        void onLongPress(Schedule schedule, View anchor);
    }

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

    public boolean hasRecurringItems() {
        for (Schedule item : items) {
            if (item.isRecurring()) {
                return true;
            }
        }
        return false;
    }

    public boolean isRecurringAt(int position) {
        if (position < 0 || position >= items.size()) {
            return false;
        }
        return items.get(position).isRecurring();
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

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.cancelPendingDrag();
        super.onViewRecycled(holder);
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
        private final Runnable delayedDragStarter;
        private float downX;
        private float downY;
        private boolean dragScheduled;

        ViewHolder(ItemTodayScheduleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.delayedDragStarter = () -> {
                dragScheduled = false;
                this.binding.dragHandle.setPressed(false);
                callbacks.onStartDrag(this);
            };
        }

        void bind(Schedule schedule) {
            cancelPendingDrag();
            binding.scheduleTitle.setText(schedule.getTitle());
            binding.scheduleMeta.setText(resolveMetaText(schedule));
            binding.scheduleTime.setText(timeFormat.format(new Date(schedule.getStartTime())));
            binding.priorityDot.setBackgroundTintList(
                    ColorStateList.valueOf(resolvePriorityColor(schedule.getPriority()))
            );
            binding.dragHandle.setOnTouchListener((v, event) -> handleDragHandleTouch(event));
            binding.getRoot().setOnLongClickListener(v -> {
                callbacks.onLongPress(schedule, binding.getRoot());
                return true;
            });
        }

        private CharSequence resolveMetaText(Schedule schedule) {
            String location = schedule.getLocation();
            if (location == null || location.trim().isEmpty()) {
                return binding.getRoot().getContext().getString(R.string.home_schedule_location_empty);
            }
            return location;
        }

        private boolean handleDragHandleTouch(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    dragScheduled = true;
                    binding.dragHandle.setPressed(true);
                    mainHandler.removeCallbacks(delayedDragStarter);
                    mainHandler.postDelayed(delayedDragStarter, DRAG_START_DELAY_MS);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (dragScheduled && movedBeyondTouchSlop(event)) {
                        cancelPendingDrag();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    cancelPendingDrag();
                    return true;
                default:
                    return false;
            }
        }

        private boolean movedBeyondTouchSlop(MotionEvent event) {
            int touchSlop = ViewConfiguration.get(binding.getRoot().getContext()).getScaledTouchSlop();
            return Math.abs(event.getRawX() - downX) > touchSlop
                    || Math.abs(event.getRawY() - downY) > touchSlop;
        }

        void cancelPendingDrag() {
            dragScheduled = false;
            binding.dragHandle.setPressed(false);
            mainHandler.removeCallbacks(delayedDragStarter);
        }

        private int resolvePriorityColor(String priority) {
            if (Schedule.PRIORITY_HIGH.equals(priority)) {
                return ContextCompat.getColor(binding.getRoot().getContext(), R.color.priority_high);
            }
            if (Schedule.PRIORITY_LOW.equals(priority)) {
                return ContextCompat.getColor(binding.getRoot().getContext(), R.color.priority_low);
            }
            return ContextCompat.getColor(binding.getRoot().getContext(), R.color.priority_medium);
        }
    }
}
