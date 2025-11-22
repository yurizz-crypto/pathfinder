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

/**
 * ScheduleFragment
 * <p>
 * This fragment displays the user's weekly class schedule.
 * Key Features:
 * 1. Displays classes grouped by Day of the Week.
 * 2. Allows manual editing of class details (Time, Room, Day).
 * 3. Integrates Google ML Kit to scan a "Certificate of Registration" (COR) image and auto-populate the schedule.
 * 4. Provides navigation links to the MapFragment for specific rooms.
 */
public class ScheduleFragment extends Fragment {

    private RecyclerView rvSchedule;
    private DBManager dbManager;
    private String userEmail;
    private ActivityResultLauncher<String> imagePicker;

    /**
     * Initializes the view, sets up the RecyclerView, and registers the Image Picker for OCR.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_schedule, container, false);

        rvSchedule = v.findViewById(R.id.rvSchedule);
        rvSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
        Button btnUpload = v.findViewById(R.id.btnUploadCOR);

        // Database Initialization
        dbManager = new DBManager(getContext());
        dbManager.open();

        // Retrieve logged-in user email
        SharedPreferences prefs = getActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("user_email", "test@user.com");

        refreshList();

        // Register Activity Result for Image Picking (Gallery)
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) processCOR(uri);
        });

        btnUpload.setOnClickListener(view -> imagePicker.launch("image/*"));

        return v;
    }

    /**
     * Processes the selected image URI using Google ML Kit Text Recognition.
     * Scans for keywords (e.g., "MATH", "IT") to automatically add schedule entries.
     *
     * @param uri The URI of the selected image.
     */
    private void processCOR(Uri uri) {
        try {
            InputImage image = InputImage.fromFilePath(getContext(), uri);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            recognizer.process(image).addOnSuccessListener(visionText -> {
                String rawText = visionText.getText().toUpperCase();
                boolean found = false;

                // Simple keyword matching logic to simulate parsing a complex document
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
     * Fetches data from DB, processes "Split Days" (e.g., "Mon/Wed"), sorts them,
     * groups them by day, and updates the RecyclerView.
     */
    private void refreshList() {
        List<DBManager.ScheduleItem> rawItems = dbManager.getUserSchedule(userEmail);

        // Flatten the list: If a class is "Mon/Wed", create two display items (one for Mon, one for Wed)
        List<DisplayItem> displayList = new ArrayList<>();
        for (DBManager.ScheduleItem item : rawItems) {
            String[] days = item.day.split("[/, ]+"); // Split by slash, comma, or space
            for (String day : days) {
                if(day.trim().isEmpty()) continue;
                displayList.add(new DisplayItem(day.trim(), item));
            }
        }

        // Sort chronologically (Day of Week -> Time)
        Collections.sort(displayList, new ScheduleComparator());

        // Group sorted items into Day buckets for the UI Adapter
        List<DayGroup> groupedList = new ArrayList<>();
        DayGroup currentGroup = null;

        for (DisplayItem item : displayList) {
            String dayKey = normalizeDay(item.splitDay);
            if (currentGroup == null || !currentGroup.dayName.equals(dayKey)) {
                currentGroup = new DayGroup(dayKey);
                groupedList.add(currentGroup);
            }
            currentGroup.items.add(item.originalItem);
        }

        rvSchedule.setAdapter(new GroupedScheduleAdapter(groupedList));
    }

    /**
     * Standardizes day strings to full names for consistent grouping.
     * @param day Input string (e.g., "M", "Mon", "Monday").
     * @return Full capitalized day name (e.g., "MONDAY").
     */
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

    // --- INNER DATA CLASSES FOR UI ---

    /** Wrapper to link a specific day (e.g., "Mon") to the full DB Item. */
    class DisplayItem {
        String splitDay;
        DBManager.ScheduleItem originalItem;
        public DisplayItem(String d, DBManager.ScheduleItem i) { splitDay = d; originalItem = i; }
    }

    /** Represents a visual section in the RecyclerView (e.g., "MONDAY" header + list of classes). */
    class DayGroup {
        String dayName;
        List<DBManager.ScheduleItem> items = new ArrayList<>();
        public DayGroup(String n) { dayName = n; }
    }

    /** Custom Comparator to sort schedule items by Day index (Mon=1, Sun=7) then by Time. */
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
            String d1 = normalizeDay(o1.splitDay);
            String d2 = normalizeDay(o2.splitDay);
            int order1 = dayOrder.containsKey(d1) ? dayOrder.get(d1) : 99;
            int order2 = dayOrder.containsKey(d2) ? dayOrder.get(d2) : 99;

            // Primary Sort: Day of Week
            if (order1 != order2) return Integer.compare(order1, order2);

            // Secondary Sort: Time
            try {
                Date t1 = timeFormat.parse(o1.originalItem.time);
                Date t2 = timeFormat.parse(o2.originalItem.time);
                if (t1 != null && t2 != null) return t1.compareTo(t2);
            } catch (ParseException e) { }
            return 0;
        }
    }

    // --- RECYCLERVIEW ADAPTER ---

    /** * Renders groups of schedules.
     * Uses a nested layout approach: One View for the Day Title, and a Container Layout for the list of subjects.
     */
    class GroupedScheduleAdapter extends RecyclerView.Adapter<GroupedScheduleAdapter.ViewHolder> {
        List<DayGroup> groups;

        public GroupedScheduleAdapter(List<DayGroup> groups) { this.groups = groups; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DayGroup group = groups.get(position);

            // Format Day Title vertically (e.g., M\nO\nN)
            String shortDay = group.dayName.length() >= 3 ? group.dayName.substring(0, 3) : group.dayName;
            StringBuilder vertBuilder = new StringBuilder();
            for(int i=0; i<shortDay.length(); i++) {
                vertBuilder.append(shortDay.charAt(i));
                if(i < shortDay.length()-1) vertBuilder.append("\n");
            }
            holder.tvDayTitle.setText(vertBuilder.toString());

            // Clear previous views to prevent duplication on bind
            holder.containerSubjects.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

            // Inflate and add individual class rows
            for (DBManager.ScheduleItem item : group.items) {
                View row = inflater.inflate(R.layout.item_schedule_row, holder.containerSubjects, false);
                ((TextView)row.findViewById(R.id.tvTime)).setText(item.time);
                ((TextView)row.findViewById(R.id.tvSubject)).setText(item.subject);
                ((TextView)row.findViewById(R.id.tvRoom)).setText(item.room);

                // Navigate to Map on click
                row.findViewById(R.id.btnNavigate).setOnClickListener(v -> {
                    if (getActivity() instanceof DashboardActivity) {
                        ((DashboardActivity) getActivity()).switchToMap(item.room);
                    }
                });

                // Edit on Long Press
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

    /**
     * Displays an AlertDialog to edit the details of a specific schedule item.
     * Populates spinners with Room, Day, and Time options.
     *
     * @param item The ScheduleItem object to be edited.
     */
    private void showEditDialog(DBManager.ScheduleItem item) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_schedule, null);

        // Find Views
        Spinner spinnerRooms = v.findViewById(R.id.spinnerRooms);
        Spinner spinnerDay = v.findViewById(R.id.spinnerDay);
        Spinner spinnerTime = v.findViewById(R.id.spinnerTime);
        Button btnSave = v.findViewById(R.id.btnSave);
        TextView btnCancel = v.findViewById(R.id.btnCancel);
        TextView tvTitle = v.findViewById(R.id.tvDialogTitle);

        tvTitle.setText("Edit " + item.subject);

        // 1. Setup ROOMS Spinner (Fetch from DB)
        List<String> rooms = dbManager.getAllRoomNames();
        setupSpinner(spinnerRooms, rooms, item.room);

        // 2. Setup DAYS Spinner (Static List)
        List<String> days = new ArrayList<>();
        days.add("Mon"); days.add("Tue"); days.add("Wed"); days.add("Thu"); days.add("Fri"); days.add("Sat"); days.add("Sun");
        days.add("Mon/Wed"); days.add("Tue/Thu"); // Common university combos
        setupSpinner(spinnerDay, days, item.day);

        // 3. Setup TIMES Spinner (Static intervals)
        List<String> times = new ArrayList<>();
        times.add("7:00 AM"); times.add("7:30 AM"); times.add("8:00 AM"); times.add("8:30 AM");
        times.add("9:00 AM"); times.add("9:30 AM"); times.add("10:00 AM"); times.add("10:30 AM");
        times.add("11:00 AM"); times.add("11:30 AM"); times.add("12:00 PM"); times.add("12:30 PM");
        times.add("1:00 PM"); times.add("1:30 PM"); times.add("2:00 PM"); times.add("2:30 PM");
        times.add("3:00 PM"); times.add("3:30 PM"); times.add("4:00 PM"); times.add("4:30 PM");
        times.add("5:00 PM"); times.add("5:30 PM"); times.add("6:00 PM");
        setupSpinner(spinnerTime, times, item.time);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(v);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // SAVE ACTION
        btnSave.setOnClickListener(view -> {
            String newRoom = spinnerRooms.getSelectedItem().toString();
            String newDay = spinnerDay.getSelectedItem().toString();
            String newTime = spinnerTime.getSelectedItem().toString();

            // Perform DB Update
            boolean success = dbManager.updateScheduleDetails(item.id, newRoom, newDay, newTime);

            if (success) {
                Toast.makeText(getContext(), "Schedule Updated!", Toast.LENGTH_SHORT).show();
                refreshList();
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Update failed.", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Helper to populate a Spinner and safely select the current value.
     * Handles minor string discrepancies (e.g., spacing issues).
     *
     * @param spinner The UI Spinner widget.
     * @param items The list of strings to populate.
     * @param currentVal The value that should be pre-selected.
     */
    private void setupSpinner(Spinner spinner, List<String> items, String currentVal) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);

        // Find position of current value
        int pos = -1;
        for(int i=0; i<items.size(); i++) {
            if(items.get(i).equalsIgnoreCase(currentVal)) {
                pos = i;
                break;
            }
        }
        // Fallback: If exact match not found (e.g. OCR gave "9:00AM" but list has "9:00 AM"), try simplified check
        if (pos == -1 && currentVal != null) {
            for(int i=0; i<items.size(); i++) {
                if(items.get(i).replace(" ","").equalsIgnoreCase(currentVal.replace(" ",""))) {
                    pos = i;
                    break;
                }
            }
        }

        if (pos >= 0) spinner.setSelection(pos);
    }
}