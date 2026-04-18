package com.example.calendar.ui.home;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calendar.R;
import com.example.calendar.databinding.ItemCalendarDayBinding;
import com.example.calendar.ui.home.calendar.CalendarDayCell;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarMonthAdapter extends RecyclerView.Adapter<CalendarMonthAdapter.ViewHolder> {
    public interface Callbacks {
        void onDateSelected(LocalDate date);
    }

    private final List<CalendarDayCell> items = new ArrayList<>();
    private final Callbacks callbacks;

    public CalendarMonthAdapter(Callbacks callbacks) {
        this.callbacks = callbacks;
    }

    public void submitItems(List<CalendarDayCell> nextItems) {
        items.clear();
        items.addAll(nextItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarDayBinding binding = ItemCalendarDayBinding.inflate(
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

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCalendarDayBinding binding;

        ViewHolder(ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(CalendarDayCell cell) {
            binding.dayNumber.setText(String.valueOf(cell.getDayOfMonth()));
            binding.markerDot.setVisibility(cell.hasSchedule() && cell.isInCurrentMonth() ? View.VISIBLE : View.INVISIBLE);
            binding.getRoot().setEnabled(cell.isInCurrentMonth());
            binding.getRoot().setAlpha(cell.isInCurrentMonth() ? 1f : 0.52f);
            binding.getRoot().setOnClickListener(cell.isInCurrentMonth()
                    ? v -> callbacks.onDateSelected(cell.getDate())
                    : null);

            if (cell.isSelected()) {
                binding.dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_selected);
                binding.dayNumber.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.calendar_hero_on));
                binding.markerDot.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.calendar_marker_on_selected)
                ));
                return;
            }

            if (cell.isToday()) {
                binding.dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_today);
                binding.dayNumber.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.calendar_hero));
            } else {
                binding.dayContainer.setBackgroundResource(R.drawable.bg_calendar_day_idle);
                binding.dayNumber.setTextColor(ContextCompat.getColor(
                        binding.getRoot().getContext(),
                        cell.isInCurrentMonth() ? R.color.text_primary : R.color.calendar_day_out_month
                ));
            }

            binding.markerDot.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(binding.getRoot().getContext(), R.color.accent_dot)
            ));
        }
    }
}
