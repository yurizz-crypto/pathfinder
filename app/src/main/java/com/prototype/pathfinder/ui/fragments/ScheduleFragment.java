package com.prototype.pathfinder.ui.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

                // Simple keyword matching for demo purposes
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
                    Toast.makeText(getContext(), "No recognizable subjects found.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Core logic: Fetches raw data, splits multi-day entries, sorts them,
     * and groups them by Day for the Recycler View.
     */
    private void refreshList() {
        List<DBManager.ScheduleItem> rawItems = dbManager.getUserSchedule(userEmail);

        // 1. Split "Mon/Wed" items into separate entries for sorting
        List<DisplayItem> displayList = new ArrayList<>();
        for (DBManager.ScheduleItem item : rawItems) {
            // Split days by slash, comma, or space (e.g. "Mon/Wed" -> "Mon", "Wed")
            String[] days = item.day.split("[/, ]+");
            for (String day : days) {
                if(day.trim().isEmpty()) continue;
                displayList.add(new DisplayItem(day.trim(), item));
            }
        }

        // 2. Sort the display list by Day then Time
        Collections.sort(displayList, new ScheduleComparator());

        // 3. Group by Day for the Adapter
        List<DayGroup> groupedList = new ArrayList<>();
        DayGroup currentGroup = null;

        for (DisplayItem item : displayList) {
            // Normalize day string (e.g. "Mon" vs "Monday")
            String dayKey = normalizeDay(item.splitDay);

            // If new group needed
            if (currentGroup == null || !currentGroup.dayName.equals(dayKey)) {
                currentGroup = new DayGroup(dayKey);
                groupedList.add(currentGroup);
            }
            currentGroup.items.add(item.originalItem);
        }

        // 4. Set Adapter
        rvSchedule.setAdapter(new GroupedScheduleAdapter(groupedList));
    }

    private String normalizeDay(String day) {
        String d = day.toUpperCase();
        if(d.startsWith("M")) return "MONDAY";
        if(d.startsWith("TU")) return "TUESDAY";
        if(d.startsWith("W")) return "WEDNESDAY";
        if(d.startsWith("TH")) return "THURSDAY";
        if(d.startsWith("F")) return "FRIDAY";
        if(d.startsWith("SA")) return "SATURDAY";
        if(d.startsWith("SU")) return "SUNDAY";
        return d;
    }

    // --- HELPER CLASSES FOR SORTING ---

    class DisplayItem {
        String splitDay;
        DBManager.ScheduleItem originalItem;
        public DisplayItem(String d, DBManager.ScheduleItem i) { splitDay = d; originalItem = i; }
    }

    class DayGroup {
        String dayName;
        List<DBManager.ScheduleItem> items = new ArrayList<>();
        public DayGroup(String n) { dayName = n; }
    }

    class ScheduleComparator implements Comparator<DisplayItem> {
        Map<String, Integer> dayOrder = new HashMap<>();
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);

        public ScheduleComparator() {
            dayOrder.put("MON", 1); dayOrder.put("MONDAY", 1);
            dayOrder.put("TUE", 2); dayOrder.put("TUESDAY", 2);
            dayOrder.put("WED", 3); dayOrder.put("WEDNESDAY", 3);
            dayOrder.put("THU", 4); dayOrder.put("THURSDAY", 4);
            dayOrder.put("FRI", 5); dayOrder.put("FRIDAY", 5);
            dayOrder.put("SAT", 6); dayOrder.put("SATURDAY", 6);
            dayOrder.put("SUN", 7); dayOrder.put("SUNDAY", 7);
        }

        @Override
        public int compare(DisplayItem o1, DisplayItem o2) {
            // 1. Compare Days
            String d1 = normalizeDay(o1.splitDay);
            String d2 = normalizeDay(o2.splitDay);

            int order1 = dayOrder.containsKey(d1) ? dayOrder.get(d1) : 99;
            int order2 = dayOrder.containsKey(d2) ? dayOrder.get(d2) : 99;

            if (order1 != order2) return Integer.compare(order1, order2);

            // 2. Compare Times (if days are equal)
            try {
                Date t1 = timeFormat.parse(o1.originalItem.time);
                Date t2 = timeFormat.parse(o2.originalItem.time);
                if (t1 != null && t2 != null) {
                    return t1.compareTo(t2);
                }
            } catch (ParseException e) {
                // Ignore parse errors, treat as equal
            }
            return 0;
        }
    }

    // --- RECYCLER ADAPTER ---

    class GroupedScheduleAdapter extends RecyclerView.Adapter<GroupedScheduleAdapter.ViewHolder> {
        List<DayGroup> groups;

        public GroupedScheduleAdapter(List<DayGroup> groups) { this.groups = groups; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Inflate the Container Card (item_schedule.xml)
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DayGroup group = groups.get(position);

            // Set Day Title (Stacked vertically: M\nO\nN)
            String shortDay = group.dayName.length() >= 3 ? group.dayName.substring(0, 3) : group.dayName;
            StringBuilder vertBuilder = new StringBuilder();
            for(int i=0; i<shortDay.length(); i++) {
                vertBuilder.append(shortDay.charAt(i));
                if(i < shortDay.length()-1) vertBuilder.append("\n");
            }
            holder.tvDayTitle.setText(vertBuilder.toString());

            // Clear previous items from the container to prevent duplicates
            holder.containerSubjects.removeAllViews();

            // Inflate and add rows for each subject
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

            for (DBManager.ScheduleItem item : group.items) {
                // Inflate the Row (item_schedule_row.xml)
                View row = inflater.inflate(R.layout.item_schedule_row, holder.containerSubjects, false);

                TextView tvTime = row.findViewById(R.id.tvTime);
                TextView tvSubject = row.findViewById(R.id.tvSubject);
                TextView tvRoom = row.findViewById(R.id.tvRoom);
                View btnNavigate = row.findViewById(R.id.btnNavigate);

                tvTime.setText(item.time);
                tvSubject.setText(item.subject);
                tvRoom.setText(item.room);

                // Navigate Click
                btnNavigate.setOnClickListener(v -> {
                    if (getActivity() instanceof DashboardActivity) {
                        ((DashboardActivity) getActivity()).switchToMap(item.room);
                    }
                });

                // Row Long Click (Edit)
                row.setOnLongClickListener(v -> {
                    showEditDialog(item);
                    return true;
                });

                holder.containerSubjects.addView(row);
            }
        }

        @Override
        public int getItemCount() { return groups.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDayTitle;
            LinearLayout containerSubjects;

            public ViewHolder(View v) {
                super(v);
                tvDayTitle = v.findViewById(R.id.tvDayTitle);
                containerSubjects = v.findViewById(R.id.containerSubjects);
            }
        }
    }

    // --- EDIT DIALOG ---

    private void showEditDialog(DBManager.ScheduleItem item) {
        // Inflate custom dialog layout
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_schedule, null);
        Spinner spinnerRooms = v.findViewById(R.id.spinnerRooms);
        Button btnSave = v.findViewById(R.id.btnSave);
        TextView btnCancel = v.findViewById(R.id.btnCancel);
        TextView tvTitle = v.findViewById(R.id.tvDialogTitle);

        tvTitle.setText("Edit " + item.subject);

        // Setup Spinner
        List<String> rooms = dbManager.getAllRoomNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, rooms);
        spinnerRooms.setAdapter(adapter);
        int position = adapter.getPosition(item.room);
        if (position >= 0) spinnerRooms.setSelection(position);

        // Build Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(v);
        AlertDialog dialog = builder.create();

        // Transparent background for rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Save Action
        btnSave.setOnClickListener(view -> {
            String newRoom = spinnerRooms.getSelectedItem().toString();
            boolean success = dbManager.updateScheduleRoom(item.id, newRoom);

            if (success) {
                Toast.makeText(getContext(), "Room updated to " + newRoom, Toast.LENGTH_SHORT).show();
                refreshList();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to update room.", Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel Action
        btnCancel.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }
}