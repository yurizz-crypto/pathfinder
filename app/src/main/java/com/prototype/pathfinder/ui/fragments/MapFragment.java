package com.prototype.pathfinder.ui.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import com.prototype.pathfinder.R;
import com.prototype.pathfinder.data.DBManager;

/**
 * MapFragment
 * <p>
 * Displays an interactive Google Map.
 * Capabilities:
 * 1. Plots campus locations stored in the local SQLite database.
 * 2. Uses FusedLocationProviderClient to get the user's real-time location.
 * 3. Draws a navigation line (Polyline) if a specific target room is passed via arguments.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String targetRoom; // The room name passed from ScheduleFragment (optional)
    private DBManager dbManager;
    private FusedLocationProviderClient fusedLocationClient;

    // CMU Center Coordinates (Admin Building Approx) - Default fallback location
    private static final LatLng CMU_CENTER = new LatLng(7.864722, 125.050833);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);

        // Retrieve arguments (if navigating from Schedule)
        if (getArguments() != null) {
            targetRoom = getArguments().getString("target_room");
        }

        dbManager = new DBManager(getContext());
        dbManager.open();

        // Initialize Location Services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize Map Fragment asynchronously
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        return v;
    }

    /**
     * Triggered when the Google Map is ready for interaction.
     * Handles logic for markers, permissions, and camera movement.
     *
     * @param googleMap The instance of the map.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // 1. Plot All Campus Locations from Database
        LatLng destLatLng = null;
        for (String room : dbManager.getAllRoomNames()) {
            DBManager.LocationItem loc = dbManager.getLocation(room);
            if (loc != null) {
                LatLng pos = new LatLng(loc.lat, loc.lng);

                // Highlight target room with a RED marker, others with AZURE
                if (targetRoom != null && targetRoom.equals(loc.name)) {
                    mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(loc.name)
                            .snippet(loc.desc)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    destLatLng = pos;
                } else {
                    mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(loc.name)
                            .snippet(loc.desc)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                }
            }
        }

        // 2. Handle User Location & Camera Positioning
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true); // Shows the blue dot

            if (destLatLng != null) {
                // Scenario A: Navigating to a specific room
                LatLng finalDest = destLatLng;
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        // Draw simple straight line (geodesic) path from User -> Destination
                        mMap.addPolyline(new PolylineOptions()
                                .add(userLatLng, finalDest)
                                .width(12)
                                .color(Color.BLUE)
                                .geodesic(true));

                        // Center camera on user to start navigation
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16));
                    } else {
                        // Fallback: GPS signal not found, just show destination
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(finalDest, 17));
                    }
                });
            } else {
                // Scenario B: Just exploring map (Center on default Campus location)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CMU_CENTER, 16));
            }
        } else {
            // Permission Denied: Just show CMU map without user location
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CMU_CENTER, 16));
            // Request permission (Request code 1001)
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }
}