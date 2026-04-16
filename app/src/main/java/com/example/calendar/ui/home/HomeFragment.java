package com.example.calendar.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.calendar.databinding.FragmentHomeBinding;
import com.example.calendar.ui.schedule.AddScheduleActivity;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private SchedulePreviewAdapter adapter;

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

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        adapter = new SchedulePreviewAdapter();

        binding.scheduleList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.scheduleList.setAdapter(adapter);
        binding.addScheduleButton.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddScheduleActivity.class))
        );

        viewModel.getScreenTitleLiveData().observe(getViewLifecycleOwner(), binding.screenTitle::setText);
        viewModel.getSampleSchedules().observe(getViewLifecycleOwner(), adapter::submitItems);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
