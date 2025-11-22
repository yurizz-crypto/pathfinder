package com.prototype.pathfinder.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.prototype.pathfinder.data.DatabaseHelper.Programs;
import com.prototype.pathfinder.data.DatabaseHelper.TestScores;
import com.prototype.pathfinder.data.DatabaseHelper.Users;
import com.prototype.pathfinder.data.DatabaseHelper.Locations;
import com.prototype.pathfinder.data.DatabaseHelper.Schedules;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DBManager {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

    public DBManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (db != null) db.close();
    }

    // --- User Methods ---
    public boolean registerUser(String username, String email, String password) {
        try {
            String hashedPw = hashPassword(password);
            ContentValues values = new ContentValues();
            values.put(Users.COLUMN_NAME_USERNAME, username);
            values.put(Users.COLUMN_NAME_EMAIL, email);
            values.put(Users.COLUMN_NAME_PASSWORD, hashedPw);
            long id = db.insert(Users.TABLE_NAME, null, values);
            return id > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loginUser(String email, String password) {
        String hashedPw = hashPassword(password);
        Cursor cursor = db.query(Users.TABLE_NAME, new String[]{Users._ID},
                Users.COLUMN_NAME_EMAIL + "=? AND " + Users.COLUMN_NAME_PASSWORD + "=?",
                new String[]{email, hashedPw}, null, null, null);
        boolean valid = cursor.getCount() > 0;
        cursor.close();
        return valid;
    }

    public String getUsername(String email) {
        Cursor cursor = db.query(Users.TABLE_NAME, new String[]{Users.COLUMN_NAME_USERNAME},
                Users.COLUMN_NAME_EMAIL + "=?", new String[]{email}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String username = cursor.getString(0);
            cursor.close();
            return username;
        }
        return "Student";
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }

    // --- TestScores Methods ---
    public Map<String, Integer> getScoresById(String testId) {
        Map<String, Integer> scores = new HashMap<>();
        Cursor cursor = db.query(TestScores.TABLE_NAME, null,
                TestScores.COLUMN_NAME_TEST_ID + "=?",
                new String[]{testId}, null, null, null);
        if (cursor.moveToFirst()) {
            scores.put("quant", cursor.getInt(cursor.getColumnIndex(TestScores.COLUMN_NAME_QUANT)));
            scores.put("verbal", cursor.getInt(cursor.getColumnIndex(TestScores.COLUMN_NAME_VERBAL)));
            scores.put("logical", cursor.getInt(cursor.getColumnIndex(TestScores.COLUMN_NAME_LOGICAL)));
        }
        cursor.close();
        return scores;
    }

    // --- Programs Methods ---
    public List<Program> getAllPrograms() {
        List<Program> programs = new ArrayList<>();
        Cursor cursor = db.query(Programs.TABLE_NAME, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            Program p = new Program(
                    cursor.getString(cursor.getColumnIndex(Programs.COLUMN_NAME_NAME)),
                    cursor.getString(cursor.getColumnIndex(Programs.COLUMN_NAME_DESC)),
                    cursor.getDouble(cursor.getColumnIndex(Programs.COLUMN_NAME_REQ_QUANT)),
                    cursor.getDouble(cursor.getColumnIndex(Programs.COLUMN_NAME_REQ_VERBAL)),
                    cursor.getDouble(cursor.getColumnIndex(Programs.COLUMN_NAME_REQ_LOGICAL))
            );
            programs.add(p);
        }
        cursor.close();
        return programs;
    }

    // --- Schedule Methods ---
    public void addSchedule(String email, String subject, String room, String day, String time) {
        ContentValues values = new ContentValues();
        values.put(Schedules.COL_EMAIL, email);
        values.put(Schedules.COL_SUBJECT, subject);
        values.put(Schedules.COL_ROOM, room);
        values.put(Schedules.COL_DAY, day);
        values.put(Schedules.COL_TIME, time);
        db.insert(Schedules.TABLE_NAME, null, values);
    }

    // UPDATED: Now retrieves ID and maps it to ScheduleItem
    public List<ScheduleItem> getUserSchedule(String email) {
        List<ScheduleItem> list = new ArrayList<>();
        // We select all columns (null), which includes _ID
        Cursor cursor = db.query(Schedules.TABLE_NAME, null,
                Schedules.COL_EMAIL + "=?", new String[]{email}, null, null, null);

        while (cursor.moveToNext()) {
            // Retrieve the unique ID for the row
            long id = cursor.getLong(cursor.getColumnIndex(Schedules._ID));

            list.add(new ScheduleItem(
                    id,
                    cursor.getString(cursor.getColumnIndex(Schedules.COL_SUBJECT)),
                    cursor.getString(cursor.getColumnIndex(Schedules.COL_ROOM)),
                    cursor.getString(cursor.getColumnIndex(Schedules.COL_DAY)),
                    cursor.getString(cursor.getColumnIndex(Schedules.COL_TIME))
            ));
        }
        cursor.close();
        return list;
    }

    // NEW: Method to update room, day, and time
    public boolean updateScheduleDetails(long id, String newRoom, String newDay, String newTime) {
        ContentValues values = new ContentValues();
        values.put(Schedules.COL_ROOM, newRoom);
        values.put(Schedules.COL_DAY, newDay);
        values.put(Schedules.COL_TIME, newTime);

        // Update where _ID equals the provided id
        int rows = db.update(Schedules.TABLE_NAME, values, Schedules._ID + "=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // --- Location Methods ---
    public List<String> getAllRoomNames() {
        List<String> list = new ArrayList<>();
        Cursor cursor = db.query(Locations.TABLE_NAME, new String[]{Locations.COL_NAME}, null, null, null, null, null);
        while(cursor.moveToNext()){
            list.add(cursor.getString(0));
        }
        cursor.close();
        return list;
    }

    public LocationItem getLocation(String roomName) {
        Cursor cursor = db.query(Locations.TABLE_NAME, null, Locations.COL_NAME + "=?", new String[]{roomName}, null, null, null);
        LocationItem loc = null;
        if (cursor.moveToFirst()) {
            loc = new LocationItem(
                    cursor.getString(cursor.getColumnIndex(Locations.COL_NAME)),
                    cursor.getDouble(cursor.getColumnIndex(Locations.COL_LAT)),
                    cursor.getDouble(cursor.getColumnIndex(Locations.COL_LNG)),
                    cursor.getString(cursor.getColumnIndex(Locations.COL_DESC))
            );
        }
        cursor.close();
        return loc;
    }

    // --- Data Classes ---
    public static class Program {
        public String name, desc;
        public double reqQuant, reqVerbal, reqLogical;
        public Program(String n, String d, double q, double v, double l) {
            name = n; desc = d; reqQuant = q; reqVerbal = v; reqLogical = l;
        }
    }

    // UPDATED: Added ID field
    public static class ScheduleItem {
        public long id;
        public String subject, room, day, time;
        public ScheduleItem(long id, String s, String r, String d, String t) {
            this.id = id;
            subject = s; room = r; day = d; time = t;
        }
    }

    public static class LocationItem {
        public String name, desc;
        public double lat, lng;
        public LocationItem(String n, double la, double lo, String d) {
            name = n; lat = la; lng = lo; desc = d;
        }
    }
}