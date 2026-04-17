package com.example.calendar.ui.auth;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.calendar.databinding.ActivityLoginBinding;
import com.example.calendar.ui.MainActivity;
import com.example.calendar.ui.profile.ProfileSetupActivity;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        LoginViewModel viewModel = new ViewModelProvider(this, new LoginViewModelFactory(this))
                .get(LoginViewModel.class);

        runEntranceAnimation();
        startFloatingAnimation(binding.glowOrbLarge, 0f, -24f, 4200L);
        startFloatingAnimation(binding.glowOrbSmall, 0f, 18f, 3600L);

        binding.loginButton.setOnClickListener(v -> viewModel.login(
                textOf(binding.accountInput.getText()),
                textOf(binding.passwordInput.getText())
        ));

        viewModel.getAccountError().observe(this, binding.accountLayout::setError);
        viewModel.getPasswordError().observe(this, binding.passwordLayout::setError);
        viewModel.getDestination().observe(this, destination -> {
            if (destination == null) {
                return;
            }
            Intent intent = new Intent(
                    this,
                    destination == LoginViewModel.LoginDestination.HOME
                            ? MainActivity.class
                            : ProfileSetupActivity.class
            );
            startActivity(intent);
            finish();
        });

        binding.forgotAction.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), "忘记密码功能后续接入", Snackbar.LENGTH_SHORT).show()
        );
        binding.registerAction.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), "注册账号功能后续接入", Snackbar.LENGTH_SHORT).show()
        );
    }

    private void runEntranceAnimation() {
        View[] views = new View[]{
                binding.brandChip,
                binding.loginTitle,
                binding.loginSubtitle,
                binding.loginCard,
                binding.actionRow
        };
        long delay = 0L;
        for (View view : views) {
            view.setAlpha(0f);
            view.setTranslationY(48f);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(delay)
                    .setDuration(420L)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
            delay += 90L;
        }
    }

    private void startFloatingAnimation(View view, float from, float to, long duration) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, from, to);
        animator.setDuration(duration);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private String textOf(android.text.Editable editable) {
        return editable == null ? "" : editable.toString();
    }
}
