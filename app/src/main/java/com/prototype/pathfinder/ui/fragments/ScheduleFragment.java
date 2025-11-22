package com.prototype.pathfinder.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.prototype.pathfinder.R;
import com.prototype.pathfinder.data.DBManager;
import com.prototype.pathfinder.ui.DashboardActivity;
import java.io.IOException;
import java.util.List;

public class ScheduleFragment extends Fragment {

    private RecyclerView rvSchedule;
    private DBManager dbManager;
    private String userEmail;
    private ActivityResultLauncher<String> imagePicker;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_schedule, container, false);

        rvSchedule = v.findViewById(R.id.rvSchedule);
        rvSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        Button btnUpload = v.findViewById(R.id.btnUploadCOR);

        dbManager = new DBManager(getContext());
        dbManager.open();
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", "test@user.com");

        refreshList();

        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) processCOR(uri);
        });

        btnUpload.setOnClickListener(view -> imagePicker.launch("image/*"));

        return v;
    }

    private void processCOR(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(getContext(), uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image).addOnSuccessListener(visionText -> {
                String rawText = visionText.getText().toUpperCase();
                boolean found = false;

                // UPDATED MAPPING FOR CMU BUILDINGS
                if (rawText.contains("MATH") || rawText.contains("STAT")) {
                    dbManager.addSchedule(userEmail, "Mathematics", "CAS Building", "Mon/Wed", "9:00 AM");
                    found = true;
                }
                if (rawText.contains("IT") || rawText.contains("COMP") || rawText.contains("PROG")) {
                    dbManager.addSchedule(userEmail, "Intro to Computing", "ICS Building", "Tue/Thu", "1:00 PM");
                    found = true;
                }
                if (rawText.contains("PE") || rawText.contains("GYM")) {
                    dbManager.addSchedule(userEmail, "Physical Education", "University Gym", "Fri", "8:00 AM");
                    found = true;
                }
                if (rawText.contains("HIST") || rawText.contains("ENG")) {
                    dbManager.addSchedule(userEmail, "Gen. Education", "Admin Building", "Wed", "10:00 AM");
                    found = true;
                }

                if (found) {
                    Toast.makeText(getContext(), "Schedule Updated from COR!", Toast.LENGTH_SHORT).show();
                    refreshList();
                } else {
                    Toast.makeText(getContext(), "No recognizable subjects found in image.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to read image: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error loading image.", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshList() {
        List<DBManager.ScheduleItem> items = dbManager.getUserSchedule(userEmail);
        rvSchedule.setAdapter(new ScheduleAdapter(items));
    }

    // ... (Adapter Inner Class remains the same as previous step) ...
    class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {
        List<DBManager.ScheduleItem> list;
        public ScheduleAdapter(List<DBManager.ScheduleItem> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DBManager.ScheduleItem item = list.get(position);
            holder.tvSubject.setText(item.subject + " (" + item.day + " " + item.time + ")");
            holder.tvLocation.setText("Location: " + item.room + " (Tap to Navigate)");

            holder.itemView.setOnClickListener(v -> {
                if (getActivity() instanceof DashboardActivity) {
                    ((DashboardActivity) getActivity()).switchToMap(item.room);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                showEditDialog(item);
                return true;
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSubject, tvLocation;
            public ViewHolder(View v) {
                super(v);
                tvSubject = v.findViewById(R.id.tvSubject);
                tvLocation = v.findViewById(R.id.tvLocation);
            }
        }
    }

    private void showEditDialog(DBManager.ScheduleItem item) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_schedule, null);
        Spinner spinnerRooms = v.findViewById(R.id.spinnerRooms);

        List<String> rooms = dbManager.getAllRoomNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, rooms);
        spinnerRooms.setAdapter(adapter);

        int position = adapter.getPosition(item.room);
        if (position >= 0) spinnerRooms.setSelection(position);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Room for " + item.subject)
                .setView(v)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newRoom = spinnerRooms.getSelectedItem().toString();
                    Toast.makeText(getContext(), "Room updated to " + newRoom, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}