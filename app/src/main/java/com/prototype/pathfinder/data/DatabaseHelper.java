package com.prototype.pathfinder.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import java.util.Random; // For mock expansion

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "pathfinder.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    public static abstract class Users implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_NAME_USERNAME = "username";
        public static final String COLUMN_NAME_EMAIL = "email";
        public static final String COLUMN_NAME_PASSWORD = "hashed_password";
    }
    private static final String SQL_CREATE_USERS = "CREATE TABLE " + Users.TABLE_NAME + " (" +
            Users._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Users.COLUMN_NAME_USERNAME + " TEXT UNIQUE NOT NULL, " +
            Users.COLUMN_NAME_EMAIL + " TEXT UNIQUE NOT NULL, " +
            Users.COLUMN_NAME_PASSWORD + " TEXT NOT NULL);";

    // TestScores table
    public static abstract class TestScores implements BaseColumns {
        public static final String TABLE_NAME = "test_scores";
        public static final String COLUMN_NAME_TEST_ID = "test_id";
        public static final String COLUMN_NAME_QUANT = "quantitative";
        public static final String COLUMN_NAME_VERBAL = "verbal";
        public static final String COLUMN_NAME_LOGICAL = "logical";
    }
    private static final String SQL_CREATE_TEST_SCORES = "CREATE TABLE " + TestScores.TABLE_NAME + " (" +
            TestScores.COLUMN_NAME_TEST_ID + " TEXT PRIMARY KEY, " +
            TestScores.COLUMN_NAME_QUANT + " INTEGER, " +
            TestScores.COLUMN_NAME_VERBAL + " INTEGER, " +
            TestScores.COLUMN_NAME_LOGICAL + " INTEGER);";

    // Programs table
    public static abstract class Programs implements BaseColumns {
        public static final String TABLE_NAME = "programs";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESC = "description";
        public static final String COLUMN_NAME_REQ_QUANT = "req_quant_weight";
        public static final String COLUMN_NAME_REQ_VERBAL = "req_verbal_weight";
        public static final String COLUMN_NAME_REQ_LOGICAL = "req_logical_weight";
    }
    private static final String SQL_CREATE_PROGRAMS = "CREATE TABLE " + Programs.TABLE_NAME + " (" +
            Programs._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Programs.COLUMN_NAME_NAME + " TEXT NOT NULL, " +
            Programs.COLUMN_NAME_DESC + " TEXT, " +
            Programs.COLUMN_NAME_REQ_QUANT + " REAL, " +
            Programs.COLUMN_NAME_REQ_VERBAL + " REAL, " +
            Programs.COLUMN_NAME_REQ_LOGICAL + " REAL);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_TEST_SCORES);
        db.execSQL(SQL_CREATE_PROGRAMS);

        // Pre-populate TestScores (5 samples; expand with loop for 100)
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST001', 85, 70, 80);");
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST002', 60, 90, 75);");
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST003', 95, 50, 90);");
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST004', 40, 85, 60);");
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST005', 75, 80, 70);");
        // Mock expansion: For 100, use loop in code, but hardcoded for simplicity

        // Pre-populate Programs (5 examples)
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (1, 'BSIT', 'Tech-focused, high quant/logical.', 0.8, 0.4, 0.7);");
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (2, 'BSEE', 'Engineering, quant-heavy.', 0.9, 0.3, 0.6);");
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (3, 'BSOA', 'Admin, verbal-focused.', 0.4, 0.7, 0.3);");
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (4, 'BSBA', 'Business, balanced verbal.', 0.5, 0.8, 0.4);");
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (5, 'BSCE', 'Civil Eng, logical/quant.', 0.7, 0.2, 0.9);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Users.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TestScores.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Programs.TABLE_NAME);
        onCreate(db);
    }
}