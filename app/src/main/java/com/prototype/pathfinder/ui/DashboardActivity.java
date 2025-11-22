package com.prototype.pathfinder.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.prototype.pathfinder.R;
import com.prototype.pathfinder.ui.fragments.HomeFragment;
import com.prototype.pathfinder.ui.fragments.MapFragment;
import com.prototype.pathfinder.ui.fragments.ScheduleFragment;

/**
 * DashboardActivity
 * <p>
 * The primary container for the application's main screens.
 * It hosts the BottomNavigationView and manages the transactions between:
 * 1. HomeFragment (Dashboard)
 * 2. ScheduleFragment (Class Schedule)
 * 3. MapFragment (Campus Map)
 */
public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bottomNav = findViewById(R.id.bottom_nav);

        // Load Default Fragment (Home) only if not restoring from a previous state
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Navigation Item Listener
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

    /**
     * Public method to allow Fragments (specifically ScheduleFragment) to programmatically
     * switch the active tab to the Map and pass a specific location to focus on.
     *
     * @param roomName The name of the room to highlight on the map.
     */
    public void switchToMap(String roomName) {
        // 1. Update the Bottom Navigation UI state
        bottomNav.setSelectedItemId(R.id.nav_map);

        // 2. Create MapFragment with arguments
        MapFragment mapFrag = new MapFragment();
        Bundle args = new Bundle();
        args.putString("target_room", roomName);
        mapFrag.setArguments(args);

        // 3. Execute Transaction
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mapFrag)
                .commit();
    }
}