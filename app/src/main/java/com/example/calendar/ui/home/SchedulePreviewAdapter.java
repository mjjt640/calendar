package com.example.calendar.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calendar.databinding.ItemTodayScheduleBinding;

import java.util.ArrayList;
import java.util.List;

public class SchedulePreviewAdapter extends RecyclerView.Adapter<SchedulePreviewAdapter.ViewHolder> {
    private final List<String> items = new ArrayList<>();

    public void submitItems(List<String> nextItems) {
        items.clear();
        items.addAll(nextItems);
        notifyDataSetChanged();
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

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemTodayScheduleBinding binding;

        ViewHolder(ItemTodayScheduleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String title) {
            binding.scheduleTitle.setText(title);
        }
    }
}
