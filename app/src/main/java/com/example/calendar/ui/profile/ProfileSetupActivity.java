package com.example.calendar.ui.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.databinding.ActivityProfileSetupBinding;
import com.example.calendar.ui.MainActivity;

public class ProfileSetupActivity extends AppCompatActivity {
    private ActivityProfileSetupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ProfileSetupViewModel viewModel = new ViewModelProvider(this, new ProfileSetupViewModelFactory(this))
                .get(ProfileSetupViewModel.class);

        binding.completeButton.setOnClickListener(v -> viewModel.saveProfile(
                textOf(binding.nicknameInput.getText()),
                textOf(binding.genderInput.getText()),
                textOf(binding.birthdayInput.getText()),
                textOf(binding.cityInput.getText()),
                textOf(binding.signatureInput.getText())
        ));
        binding.skipButton.setOnClickListener(v -> viewModel.skipProfile());

        viewModel.getNicknameValue().observe(this, value -> {
            if (value != null && !value.equals(textOf(binding.nicknameInput.getText()))) {
                binding.nicknameInput.setText(value);
            }
        });
        viewModel.getFinished().observe(this, done -> {
            if (Boolean.TRUE.equals(done)) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }

    private String textOf(android.text.Editable editable) {
        return editable == null ? "" : editable.toString();
    }
}
