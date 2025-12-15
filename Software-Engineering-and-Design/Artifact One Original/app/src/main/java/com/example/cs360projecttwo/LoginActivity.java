package com.example.cs360projecttwo;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameField, passwordField;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        usernameField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
        // These buttons are only used in onCreate(), so use them as local variables
        findViewById(R.id.loginButton).setOnClickListener(view -> loginUser());
        findViewById(R.id.registerButton).setOnClickListener(view -> registerUser());

        setupBottomNav();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_login);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_login) {
                return true;
            } else if (id == R.id.nav_grid) {
                startActivity(new Intent(this, DataGridActivity.class));
                return true;
            } else if (id == R.id.nav_sms) {
                startActivity(new Intent(this, SMSActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loginUser() {
        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=? AND password=?",
                new String[]{username, password});

        if (cursor.moveToFirst()) {
            cursor.close();
            db.close();
            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, DataGridActivity.class));
        } else {
            cursor.close();
            db.close();
            Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser() {
        String username = usernameField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username=?", new String[]{username});

        if (cursor.moveToFirst()) {
            cursor.close();
            db.close();
            Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show();
        } else {
            db.execSQL("INSERT INTO users(username, password) VALUES(?, ?)", new Object[]{username, password});
            cursor.close();
            db.close();
            Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show();
        }
    }
}
