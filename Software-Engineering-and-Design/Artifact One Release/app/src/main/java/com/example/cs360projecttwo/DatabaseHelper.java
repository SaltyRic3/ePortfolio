package com.example.cs360projecttwo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "inventory.db";
    // Bump version when we change schema (adds role + password_hash)
    private static final int DATABASE_VERSION = 2;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD_HASH = "password_hash";
    private static final String COL_ROLE = "role";

    // Inventory table
    private static final String TABLE_INVENTORY = "inventory";
    private static final String COL_ITEM_ID = "item_id";
    private static final String COL_ITEM_NAME = "item_name";
    private static final String COL_ITEM_QUANTITY = "item_quantity";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        db.execSQL("CREATE TABLE " + TABLE_USERS + " ("
                + COL_USERNAME + " TEXT PRIMARY KEY, "
                + COL_PASSWORD_HASH + " TEXT NOT NULL, "
                + COL_ROLE + " TEXT NOT NULL DEFAULT 'user'"
                + ");");

        // Create inventory table
        db.execSQL("CREATE TABLE " + TABLE_INVENTORY + " ("
                + COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ITEM_NAME + " TEXT NOT NULL, "
                + COL_ITEM_QUANTITY + " INTEGER NOT NULL"
                + ");");

        // Seed a default admin user: username = admin, password = admin123
        createDefaultAdmin(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple strategy for this project: drop and recreate
        // (OK for class assignments; in production we'd migrate data instead)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_INVENTORY);
        onCreate(db);
    }

    // ----------- User & Auth helpers -----------

    private void createDefaultAdmin(SQLiteDatabase db) {
        String username = "admin";
        String rawPassword = "admin123";
        String hash = hashPassword(rawPassword);

        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD_HASH, hash);
        values.put(COL_ROLE, "admin");

        db.insert(TABLE_USERS, null, values);
    }

    public boolean userExists(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_USERNAME},
                COL_USERNAME + " = ?",
                new String[]{username},
                null, null, null)) {

            return cursor.moveToFirst();
        }
    }

    public boolean registerUser(String username, String rawPassword, String role) {
        if (userExists(username)) {
            return false;
        }

        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD_HASH, hashPassword(rawPassword));
        values.put(COL_ROLE, role);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean validateUser(String username, String rawPassword) {
        SQLiteDatabase db = getReadableDatabase();
        String hashedInput = hashPassword(rawPassword);

        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_PASSWORD_HASH},
                COL_USERNAME + " = ?",
                new String[]{username},
                null, null, null)) {

            if (!cursor.moveToFirst()) {
                return false;
            }

            String storedHash = cursor.getString(0);
            return hashedInput.equals(storedHash);
        }
    }

    public String getUserRole(String username) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_ROLE},
                COL_USERNAME + " = ?",
                new String[]{username},
                null, null, null)) {

            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        }
        // Default to standard user if not found
        return "user";
    }

    // ----------- Inventory helpers (optional wrappers, if needed) -----------

    public SQLiteDatabase getReadableInventoryDb() {
        return getReadableDatabase();
    }

    public SQLiteDatabase getWritableInventoryDb() {
        return getWritableDatabase();
    }

    // ----------- Password hashing (SHA-256) -----------

    private static String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e("DatabaseHelper", "SHA-256 not available, storing password in plain text!", e);
            // Fallback (should not happen, but avoids crash in class environment)
            return rawPassword;
        }
    }
}
