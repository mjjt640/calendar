package com.example.calendar.ui.auth;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
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
        startFloatingAnimation(binding.glowOrbMid, 0f, -14f, 3900L);
        startButtonAnimations();
        bindInputFocusAnimations();
        bindButtonTouchFeedback();

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

    private void startButtonAnimations() {
        ObjectAnimator glowScaleX = ObjectAnimator.ofFloat(binding.loginButtonGlow, View.SCALE_X, 0.96f, 1.04f);
        glowScaleX.setDuration(1800L);
        glowScaleX.setRepeatCount(ValueAnimator.INFINITE);
        glowScaleX.setRepeatMode(ValueAnimator.REVERSE);
        glowScaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        glowScaleX.start();

        ObjectAnimator glowScaleY = ObjectAnimator.ofFloat(binding.loginButtonGlow, View.SCALE_Y, 0.92f, 1.08f);
        glowScaleY.setDuration(1800L);
        glowScaleY.setRepeatCount(ValueAnimator.INFINITE);
        glowScaleY.setRepeatMode(ValueAnimator.REVERSE);
        glowScaleY.setInterpolator(new AccelerateDecelerateInterpolator());
        glowScaleY.start();

        ObjectAnimator shimmer = ObjectAnimator.ofFloat(binding.loginButtonShimmer, View.TRANSLATION_X, 0f, 520f);
        shimmer.setDuration(1850L);
        shimmer.setRepeatCount(ValueAnimator.INFINITE);
        shimmer.setRepeatMode(ValueAnimator.RESTART);
        shimmer.setInterpolator(new DecelerateInterpolator());
        shimmer.start();
    }

    private void bindInputFocusAnimations() {
        binding.accountInput.setOnFocusChangeListener((v, hasFocus) -> animateFieldFocus(binding.accountLayout, hasFocus));
        binding.passwordInput.setOnFocusChangeListener((v, hasFocus) -> animateFieldFocus(binding.passwordLayout, hasFocus));
    }

    private void animateFieldFocus(View target, boolean focused) {
        target.animate()
                .translationY(focused ? -3f : 0f)
                .alpha(focused ? 1f : 0.98f)
                .setDuration(220L)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        ViewCompat.setElevation(target, focused ? 10f : 0f);
    }

    private void bindButtonTouchFeedback() {
        binding.loginButton.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                binding.loginButtonShell.animate()
                        .scaleX(0.985f)
                        .scaleY(0.985f)
                        .setDuration(110L)
                        .start();
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                binding.loginButtonShell.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(160L)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
            return false;
        });
    }

    private String textOf(android.text.Editable editable) {
        return editable == null ? "" : editable.toString();
    }
}
