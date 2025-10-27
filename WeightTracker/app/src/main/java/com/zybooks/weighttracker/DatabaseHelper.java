package com.zybooks.weighttracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// Handles everything related to the local SQLite database for the Weight Tracker app.
// Stores user accounts, their goals, and weight entries.
@SuppressWarnings("SpellCheckingInspection")
public class DatabaseHelper extends SQLiteOpenHelper implements AutoCloseable {

    // Database name and version
    private static final String DB_NAME = "weighttracker.db";
    private static final int DB_VERSION = 2; // version 2 includes new user info columns

    // Users table
    private static final String TABLE_USERS    = "users";
    private static final String COL_USER_ID    = "_id";
    private static final String COL_USERNAME   = "username";
    private static final String COL_PASSWORD   = "password";
    private static final String COL_GOAL       = "goal_weight";
    private static final String COL_EMAIL      = "email";
    private static final String COL_FIRST      = "first_name";
    private static final String COL_LAST       = "last_name";
    private static final String COL_SEC_Q      = "sec_question";
    private static final String COL_SEC_AH     = "sec_answer_hash"; // kept for compatibility
    private static final String COL_CREATED_AT = "created_at";

    // Weights table
    private static final String TABLE_WEIGHTS  = "weights";
    private static final String COL_WEIGHT_ID  = "_id";
    private static final String COL_USER_FK    = "user_id";
    private static final String COL_DATE       = "entry_date";
    private static final String COL_WEIGHT     = "weight_lbs";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // Creates both tables the first time the app runs
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Users table: stores basic account info and goal data
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID    + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME   + " TEXT UNIQUE, " +
                COL_PASSWORD   + " TEXT, " +
                COL_GOAL       + " REAL DEFAULT 0, " +
                COL_EMAIL      + " TEXT, " +
                COL_FIRST      + " TEXT, " +
                COL_LAST       + " TEXT, " +
                COL_SEC_Q      + " TEXT, " +
                COL_SEC_AH     + " TEXT, " +
                COL_CREATED_AT + " INTEGER" +
                ")");

        // Adds an index so no two users can share the same email (still allows empty ones)
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON " +
                TABLE_USERS + "(" + COL_EMAIL + ")");

        // Weights table: stores each logged weight tied to a specific user
        db.execSQL("CREATE TABLE " + TABLE_WEIGHTS + " (" +
                COL_WEIGHT_ID  + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USER_FK    + " INTEGER, " +
                COL_DATE       + " TEXT, " +
                COL_WEIGHT     + " REAL, " +
                "FOREIGN KEY(" + COL_USER_FK + ") REFERENCES " +
                TABLE_USERS + "(" + COL_USER_ID + "))");
    }

    // Runs if the database version changes (adds new columns without deleting data)
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Adds any missing columns if upgrading from the first version
            if (!columnExists(db, TABLE_USERS, COL_EMAIL)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_EMAIL + " TEXT");
            }
            if (!columnExists(db, TABLE_USERS, COL_FIRST)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_FIRST + " TEXT");
            }
            if (!columnExists(db, TABLE_USERS, COL_LAST)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_LAST + " TEXT");
            }
            if (!columnExists(db, TABLE_USERS, COL_SEC_Q)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_SEC_Q + " TEXT");
            }
            if (!columnExists(db, TABLE_USERS, COL_SEC_AH)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_SEC_AH + " TEXT");
            }
            if (!columnExists(db, TABLE_USERS, COL_CREATED_AT)) {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_CREATED_AT + " INTEGER");
            }

            // Recreates the email index if it wasn’t already there
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON " +
                    TABLE_USERS + "(" + COL_EMAIL + ")");
        }
    }

    // Creates a new user with all registration details filled in
    public long createUserFull(String first, String last, String email,
                               String username, String password,
                               String secQuestion, String secAnswerPlain) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_FIRST, trimOrNull(first));
        v.put(COL_LAST,  trimOrNull(last));
        v.put(COL_EMAIL, trimLower(email));
        v.put(COL_USERNAME, username == null ? null : username.trim());
        v.put(COL_PASSWORD, password);
        v.put(COL_SEC_Q, trimOrNull(secQuestion));
        v.put(COL_SEC_AH, trimOrNull(secAnswerPlain)); // stored as plain text for now
        v.put(COL_CREATED_AT, System.currentTimeMillis());
        return db.insert(TABLE_USERS, null, v);
    }

    // Checks if a username already exists
    public boolean usernameExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USERNAME + "=?", new String[]{username == null ? "" : username.trim()},
                null, null, null);
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    // Checks if an email is already being used
    public boolean emailExists(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_EMAIL + "=?", new String[]{trimLower(email)},
                null, null, null);
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    // Confirms a user ID still exists in the database (useful if a session needs to be verified)
    @SuppressWarnings("unused")
    public boolean userIdExists(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USER_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        boolean found = c.moveToFirst();
        c.close();
        return found;
    }

    // Checks if a username and password match, and returns that user's ID
    public long authenticate(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username == null ? "" : username.trim(), password},
                null, null, null);
        long userId = -1;
        if (c.moveToFirst()) userId = c.getLong(0);
        c.close();
        return userId;
    }

    // Finds a username by matching an email address
    public String getUsernameByEmail(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_USERNAME},
                COL_EMAIL + "=?", new String[]{trimLower(email)},
                null, null, null);
        String username = null;
        if (c.moveToFirst()) username = c.getString(0);
        c.close();
        return username;
    }

    // Gets the stored security question for a user
    public String getSecurityQuestion(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_SEC_Q},
                COL_USERNAME + "=?", new String[]{username == null ? "" : username.trim()},
                null, null, null);
        String question = null;
        if (c.moveToFirst()) question = c.getString(0);
        c.close();
        return question;
    }

    // Gets the security answer (still plain text for this project)
    public String getSecurityAnswer(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_USERS, new String[]{COL_SEC_AH},
                COL_USERNAME + "=?", new String[]{username == null ? "" : username.trim()},
                null, null, null);
        String answer = null;
        if (c.moveToFirst()) answer = c.getString(0);
        c.close();
        return answer;
    }

    // Updates a user’s password when they reset it
    public boolean updatePassword(String username, String newPassword) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PASSWORD, newPassword);
        int rows = db.update(TABLE_USERS, cv, COL_USERNAME + "=?",
                new String[]{username == null ? "" : username.trim()});
        return rows > 0;
    }

    // Sets or updates a user’s goal weight
    public void setGoalWeight(long userId, double goal) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_GOAL, goal);
        db.update(TABLE_USERS, cv, COL_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
    }

    // Returns the goal weight for a user (0 if none is set)
    public double getGoalWeight(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT " + COL_GOAL +
                        " FROM " + TABLE_USERS +
                        " WHERE " + COL_USER_ID + "=?",
                new String[]{String.valueOf(userId)});
        double goal = 0;
        if (c.moveToFirst()) goal = c.getDouble(0);
        c.close();
        return goal;
    }

    // Adds a new weight record for a user
    @SuppressWarnings("UnusedReturnValue")
    public long addWeight(long userId, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_USER_FK, userId);
        v.put(COL_DATE, date);
        v.put(COL_WEIGHT, weight);
        return db.insert(TABLE_WEIGHTS, null, v);
    }

    // Returns all stored weight entries for a specific user
    public Cursor getAllWeights(long userId) {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TABLE_WEIGHTS,
                new String[]{COL_WEIGHT_ID, COL_DATE, COL_WEIGHT},
                COL_USER_FK + "=?",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_DATE + " DESC");
    }

    // Updates an existing weight entry (kept for possible edit feature)
    @SuppressWarnings({"UnusedReturnValue", "unused"})
    public int updateWeight(long id, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put(COL_DATE, date);
        v.put(COL_WEIGHT, weight);
        return db.update(TABLE_WEIGHTS, v,
                COL_WEIGHT_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    // Deletes a specific weight entry by its row ID
    @SuppressWarnings("UnusedReturnValue")
    public int deleteWeight(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_WEIGHTS,
                COL_WEIGHT_ID + "=?",
                new String[]{String.valueOf(id)});
    }

    // Checks if a column exists in a table (helps prevent crashes when upgrading)
    @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "SameParameterValue"})
    private boolean columnExists(SQLiteDatabase db, String table, String column) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (nameIdx >= 0 && column.equalsIgnoreCase(c.getString(nameIdx))) {
                    return true;
                }
            }
        }
        return false;
    }

    // Safely trims strings or returns null if empty
    private static String trimOrNull(String s) {
        return s == null ? null : s.trim();
    }

    // Trims and converts to lowercase (used for emails)
    private static String trimLower(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }
}