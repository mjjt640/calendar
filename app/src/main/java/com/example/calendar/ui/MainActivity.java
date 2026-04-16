package com.example.calendar.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calendar.R;
import com.example.calendar.common.constants.AppConstants;
import com.example.calendar.databinding.ActivityMainBinding;
import com.example.calendar.ui.home.HomeFragment;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment(), AppConstants.HOME_TAG)
                    .commit();
        }
    }
}
