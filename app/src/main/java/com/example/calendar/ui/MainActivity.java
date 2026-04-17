package com.example.calendar.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.calendar.R;
import com.example.calendar.common.constants.AppConstants;
import com.example.calendar.databinding.ActivityMainBinding;
import com.example.calendar.ui.home.HomeFragment;
import com.example.calendar.ui.user.UserFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                switchFragment(new HomeFragment(), AppConstants.HOME_TAG);
                return true;
            }
            if (item.getItemId() == R.id.navigation_user) {
                switchFragment(new UserFragment(), AppConstants.USER_TAG);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            binding.bottomNavigation.setSelectedItemId(R.id.navigation_home);
        }
    }

    private void switchFragment(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }
}
