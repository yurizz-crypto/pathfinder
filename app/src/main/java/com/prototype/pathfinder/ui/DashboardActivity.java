package com.prototype.pathfinder.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.ui.fragments.HomeFragment;
import com.prototype.pathfinder.ui.fragments.MapFragment;
import com.prototype.pathfinder.ui.fragments.ScheduleFragment;

public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bottomNav = findViewById(R.id.bottom_nav);

        // Load Default Fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_schedule) {
                selectedFragment = new ScheduleFragment();
            } else if (id == R.id.nav_map) {
                selectedFragment = new MapFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
    }

    // Public method to allow Fragments to switch tabs (e.g., Schedule -> Map)
    public void switchToMap(String roomName) {
        bottomNav.setSelectedItemId(R.id.nav_map);

        MapFragment mapFrag = new MapFragment();
        Bundle args = new Bundle();
        args.putString("target_room", roomName);
        mapFrag.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mapFrag)
                .commit();
    }
}