package com.example.calendar.ui.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.databinding.FragmentUserBinding;
import com.example.calendar.domain.model.UserProfile;
import com.example.calendar.ui.auth.LoginActivity;

public class UserFragment extends Fragment {
    private FragmentUserBinding binding;
    private UserViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUserBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this, new UserViewModelFactory(requireContext()))
                .get(UserViewModel.class);

        binding.logoutButton.setOnClickListener(v -> viewModel.logout());

        viewModel.getProfileLiveData().observe(getViewLifecycleOwner(), this::bindProfile);
        viewModel.getLogoutState().observe(getViewLifecycleOwner(), logout -> {
            if (Boolean.TRUE.equals(logout)) {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        viewModel.loadProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadProfile();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void bindProfile(UserProfile profile) {
        if (profile == null) {
            return;
        }
        String nickname = emptyToFallback(profile.getNickname(), "用户");
        String account = emptyToFallback(profile.getAccount(), "未设置账号");
        String signature = emptyToFallback(profile.getSignature(), "暂未留下签名");
        binding.userName.setText(nickname);
        binding.userAccount.setText(account);
        binding.userSignature.setText(signature);
    }

    private String emptyToFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
