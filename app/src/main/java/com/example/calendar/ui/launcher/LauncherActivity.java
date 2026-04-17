package com.example.calendar.ui.launcher;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.calendar.data.repository.LocalAuthRepository;
import com.example.calendar.data.repository.LocalProfileRepository;
import com.example.calendar.ui.MainActivity;
import com.example.calendar.ui.auth.LoginActivity;
import com.example.calendar.ui.profile.ProfileSetupActivity;

public class LauncherActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LauncherRoute route = new LauncherRouteResolver().resolve(
                new LocalAuthRepository(this).getSession(),
                new LocalProfileRepository(this).getProfile()
        );

        Class<?> destination;
        if (route == LauncherRoute.HOME) {
            destination = MainActivity.class;
        } else if (route == LauncherRoute.PROFILE_SETUP) {
            destination = ProfileSetupActivity.class;
        } else {
            destination = LoginActivity.class;
        }

        startActivity(new Intent(this, destination));
        finish();
    }
}
