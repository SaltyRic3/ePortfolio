package com.example.cs360projecttwo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// This helper class manages database creation and version management
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Name and Version
    private static final String DATABASE_NAME = "TrakerApp.db";
    private static final int DATABASE_VERSION = 1;

    // Table: users
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    // Table: inventory
    public static final String TABLE_INVENTORY = "inventory";
    public static final String COL_ITEM_ID = "item_id";
    public static final String COL_ITEM_NAME = "item_name";
    public static final String COL_ITEM_QUANTITY = "item_quantity";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Called when the database is created for the first time
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT NOT NULL UNIQUE, " +
                COL_PASSWORD + " TEXT NOT NULL);";

        // Create inventory table
        String createInventoryTable = "CREATE TABLE " + TABLE_INVENTORY + " (" +
                COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ITEM_NAME + " TEXT NOT NULL, " +
                COL_ITEM_QUANTITY + " INTEGER NOT NULL);";

        db.execSQL(createUsersTable);
        db.execSQL(createInventoryTable);
    }

    // Called when the database needs to be upgraded
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop existing tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        onCreate(db); // Recreate the database
    }
}
