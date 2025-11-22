package com.prototype.pathfinder.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.prototype.pathfinder.data.DatabaseHelper.Programs;
import com.prototype.pathfinder.data.DatabaseHelper.TestScores;
import com.prototype.pathfinder.data.DatabaseHelper.Users;
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

    // User methods
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

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password; // Fallback (not ideal)
        }
    }

    // TestScores
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

    // Programs
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

    public static class Program {
        public String name, desc;
        public double reqQuant, reqVerbal, reqLogical;
        public Program(String n, String d, double q, double v, double l) {
            name = n; desc = d; reqQuant = q; reqVerbal = v; reqLogical = l;
        }
    }
}