package com.prototype.pathfinder.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

/**
 * DatabaseHelper
 * <p>
 * This class manages the database creation and version management.
 * It defines the table schemas (Contracts) and handles the initial
 * population of data for Tests, Programs, and Locations.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "pathfinder_v2.db";
    private static final int DATABASE_VERSION = 3; // Incremented Version to force upgrade path

    // --- EXISTING TABLES CONTRACTS ---

    /**
     * Schema definition for the User table.
     * Stores authentication details.
     */
    public static abstract class Users implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_NAME_USERNAME = "username";
        public static final String COLUMN_NAME_EMAIL = "email";
        public static final String COLUMN_NAME_PASSWORD = "hashed_password";
    }

    /**
     * Schema definition for Test Scores.
     * Stores raw scores for aptitude categories.
     */
    public static abstract class TestScores implements BaseColumns {
        public static final String TABLE_NAME = "test_scores";
        public static final String COLUMN_NAME_TEST_ID = "test_id"; // e.g., TEST001
        public static final String COLUMN_NAME_QUANT = "quantitative";
        public static final String COLUMN_NAME_VERBAL = "verbal";
        public static final String COLUMN_NAME_LOGICAL = "logical";
    }

    /**
     * Schema definition for Academic Programs.
     * Contains program details and weighting requirements for recommendation logic.
     */
    public static abstract class Programs implements BaseColumns {
        public static final String TABLE_NAME = "programs";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_DESC = "description";
        // Weighting factors (0.0 - 1.0) representing importance of each skill for this program
        public static final String COLUMN_NAME_REQ_QUANT = "req_quant_weight";
        public static final String COLUMN_NAME_REQ_VERBAL = "req_verbal_weight";
        public static final String COLUMN_NAME_REQ_LOGICAL = "req_logical_weight";
    }

    // --- LOCATIONS TABLE (Updated for CMU) ---

    /**
     * Schema definition for Campus Locations.
     * Stores geospatial data for the map features.
     */
    public static abstract class Locations implements BaseColumns {
        public static final String TABLE_NAME = "locations";
        public static final String COL_NAME = "room_name";
        public static final String COL_LAT = "latitude";
        public static final String COL_LNG = "longitude";
        public static final String COL_DESC = "description";
    }

    // --- SCHEDULE TABLE ---

    /**
     * Schema definition for User Schedules.
     * Links specific subjects and times to a user email.
     */
    public static abstract class Schedules implements BaseColumns {
        public static final String TABLE_NAME = "schedules";
        public static final String COL_EMAIL = "user_email"; // Foreign key-like reference to Users
        public static final String COL_SUBJECT = "subject_code";
        public static final String COL_ROOM = "room_name";
        public static final String COL_DAY = "day_of_week";
        public static final String COL_TIME = "time_slot";
    }

    // --- SQL CREATE STATEMENTS ---

    private static final String SQL_CREATE_USERS = "CREATE TABLE " + Users.TABLE_NAME + " (" +
            Users._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Users.COLUMN_NAME_USERNAME + " TEXT UNIQUE NOT NULL, " +
            Users.COLUMN_NAME_EMAIL + " TEXT UNIQUE NOT NULL, " +
            Users.COLUMN_NAME_PASSWORD + " TEXT NOT NULL);";

    private static final String SQL_CREATE_TEST_SCORES = "CREATE TABLE " + TestScores.TABLE_NAME + " (" +
            TestScores.COLUMN_NAME_TEST_ID + " TEXT PRIMARY KEY, " +
            TestScores.COLUMN_NAME_QUANT + " INTEGER, " +
            TestScores.COLUMN_NAME_VERBAL + " INTEGER, " +
            TestScores.COLUMN_NAME_LOGICAL + " INTEGER);";

    private static final String SQL_CREATE_PROGRAMS = "CREATE TABLE " + Programs.TABLE_NAME + " (" +
            Programs._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Programs.COLUMN_NAME_NAME + " TEXT NOT NULL, " +
            Programs.COLUMN_NAME_DESC + " TEXT, " +
            Programs.COLUMN_NAME_REQ_QUANT + " REAL, " +
            Programs.COLUMN_NAME_REQ_VERBAL + " REAL, " +
            Programs.COLUMN_NAME_REQ_LOGICAL + " REAL);";

    private static final String SQL_CREATE_LOCATIONS = "CREATE TABLE " + Locations.TABLE_NAME + " (" +
            Locations._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Locations.COL_NAME + " TEXT NOT NULL, " +
            Locations.COL_LAT + " REAL, " +
            Locations.COL_LNG + " REAL, " +
            Locations.COL_DESC + " TEXT);";

    private static final String SQL_CREATE_SCHEDULES = "CREATE TABLE " + Schedules.TABLE_NAME + " (" +
            Schedules._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Schedules.COL_EMAIL + " TEXT, " +
            Schedules.COL_SUBJECT + " TEXT, " +
            Schedules.COL_ROOM + " TEXT, " +
            Schedules.COL_DAY + " TEXT, " +
            Schedules.COL_TIME + " TEXT);";

    /**
     * Constructor
     * @param context Application context
     */
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     * Creates tables and populates initial seed data.
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USERS);
        db.execSQL(SQL_CREATE_TEST_SCORES);
        db.execSQL(SQL_CREATE_PROGRAMS);
        db.execSQL(SQL_CREATE_LOCATIONS);
        db.execSQL(SQL_CREATE_SCHEDULES);

        populateInitialData(db);
    }

    /**
     * Helper method to insert static data into the database upon creation.
     * Includes:
     * 1. Mock Test Scores (Test IDs)
     * 2. Academic Programs with logic weights
     * 3. Real-world locations for Central Mindanao University (CMU)
     *
     * @param db The writeable database instance.
     */
    private void populateInitialData(SQLiteDatabase db) {
        // Test Scores
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST001', 85, 70, 80);");
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST002', 60, 90, 75);");
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST003', 95, 50, 90);");
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST004', 40, 85, 60);");
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('TEST005', 75, 80, 70);");
        db.execSQL("INSERT INTO " + TestScores.TABLE_NAME + " VALUES ('FAIL001', 40, 30, 35);");

        // Programs
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (1, 'BSIT', 'Tech-focused, high quant/logical.', 0.8, 0.4, 0.7);");
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (2, 'BSEE', 'Engineering, quant-heavy.', 0.9, 0.3, 0.6);");
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (3, 'BSOA', 'Admin, verbal-focused.', 0.4, 0.7, 0.3);");
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (4, 'BSBA', 'Business, balanced verbal.', 0.5, 0.8, 0.4);");
        db.execSQL("INSERT INTO " + Programs.TABLE_NAME + " VALUES (5, 'BSCE', 'Civil Eng, logical/quant.', 0.7, 0.2, 0.9);");

        // --- REAL CMU LOCATIONS ---
        // Coordinates approximation for Central Mindanao University, Musuan, Maramag, Bukidnon

        // 1. Admin Building (Approx Central)
        db.execSQL("INSERT INTO " + Locations.TABLE_NAME + " VALUES (null, 'Admin Building', 7.864722, 125.050833, 'Main Administration Office');");

        // 2. College of Arts and Sciences (CAS)
        db.execSQL("INSERT INTO " + Locations.TABLE_NAME + " VALUES (null, 'CAS Building', 7.8655, 125.0518, 'College of Arts & Sciences');");

        // 3. College of Information Sciences (ICS/IT)
        db.execSQL("INSERT INTO " + Locations.TABLE_NAME + " VALUES (null, 'ICS Building', 7.8640, 125.0525, 'Information Sciences & Computing');");

        // 4. University Hospital
        db.execSQL("INSERT INTO " + Locations.TABLE_NAME + " VALUES (null, 'University Hospital', 7.8615, 125.0485, 'CMU Hospital & Infirmary');");

        // 5. University Gym
        db.execSQL("INSERT INTO " + Locations.TABLE_NAME + " VALUES (null, 'University Gym', 7.8625, 125.0500, 'Sports & Events Center');");

        // 6. University Library
        db.execSQL("INSERT INTO " + Locations.TABLE_NAME + " VALUES (null, 'Main Library', 7.8650, 125.0510, 'University Library');");
    }

    /**
     * Called when the database version increases.
     * Currently uses a destructive strategy (DROP ALL and RECREATE).
     *
     * @param db The database.
     * @param oldVersion The old version number.
     * @param newVersion The new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + Users.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TestScores.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Programs.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Locations.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Schedules.TABLE_NAME);
        onCreate(db);
    }
}