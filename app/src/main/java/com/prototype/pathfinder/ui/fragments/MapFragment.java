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

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String targetRoom;
    private DBManager dbManager;
    private FusedLocationProviderClient fusedLocationClient;

    // CMU Center Coordinates (Admin Building Approx)
    private static final LatLng CMU_CENTER = new LatLng(7.864722, 125.050833);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_map, container, false);

        if (getArguments() != null) {
            targetRoom = getArguments().getString("target_room");
        }

        dbManager = new DBManager(getContext());
        dbManager.open();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        return v;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // 1. Add Destination Markers
        LatLng destLatLng = null;
        for (String room : dbManager.getAllRoomNames()) {
            DBManager.LocationItem loc = dbManager.getLocation(room);
            if (loc != null) {
                LatLng pos = new LatLng(loc.lat, loc.lng);

                // Highlight target room if selected
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

        // 2. Handle User Location & Camera
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            if (destLatLng != null) {
                // If navigating to a specific room
                LatLng finalDest = destLatLng;
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        // Draw Path line
                        mMap.addPolyline(new PolylineOptions()
                                .add(userLatLng, finalDest)
                                .width(12)
                                .color(Color.BLUE)
                                .geodesic(true));

                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16));
                    } else {
                        // Fallback if gps not ready
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(finalDest, 17));
                    }
                });
            } else {
                // Default View: Center on CMU
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CMU_CENTER, 16));
            }
        } else {
            // No Permissions: Just show CMU map
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(CMU_CENTER, 16));
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }
}