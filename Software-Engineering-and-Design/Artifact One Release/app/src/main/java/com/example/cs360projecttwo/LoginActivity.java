package com.example.cs360projecttwo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * LoginActivity
 *
 * Handles user authentication and registration for the Inventory app.
 * Uses SHA-256 password hashing and stores user role (admin/user).
 * NOTE: Login state is cleared on every fresh app start, so the user
 * must log in again each time they open the app.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText usernameField;
    private EditText passwordField;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔒 Always start logged out when the app is (re)opened
        clearAuthState();

        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        initViews();
        initClickListeners();
    }

    // ----------------- UI setup -----------------

    private void initViews() {
        usernameField = findViewById(R.id.usernameField);
        passwordField = findViewById(R.id.passwordField);
    }

    private void initClickListeners() {
        findViewById(R.id.loginButton).setOnClickListener(view -> loginUser());
        findViewById(R.id.registerButton).setOnClickListener(view -> registerUser());
    }

    // ----------------- Auth helpers -----------------

    private void clearAuthState() {
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .putString("userRole", "user")
                .apply();
    }

    private boolean validateCredentialsNotEmpty(String username, String password) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter both username and password.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // Additional validation for registration (length, no spaces, etc.)
    private boolean validateRegistrationInput(String username, String password) {
        if (!validateCredentialsNotEmpty(username, password)) {
            return false;
        }

        if (username.contains(" ")) {
            Toast.makeText(this, "Username cannot contain spaces.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (username.length() < 3) {
            Toast.makeText(this, "Username must be at least 3 characters.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void setLoggedIn(boolean loggedIn) {
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", loggedIn)
                .apply();
    }

    private void setUserRole(String role) {
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .putString("userRole", role)
                .apply();
    }

    // ----------------- Login & Register logic -----------------

    private void loginUser() {
        final String username = usernameField.getText().toString().trim();
        final String password = passwordField.getText().toString().trim();

        if (!validateCredentialsNotEmpty(username, password)) {
            return;
        }

        boolean valid = dbHelper.validateUser(username, password);
        if (valid) {
            setLoggedIn(true);
            String role = dbHelper.getUserRole(username);
            setUserRole(role);

            Toast.makeText(this,
                    "Login successful (" + role + ")", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, DataGridActivity.class));
            finish(); // don't let user go back to login with Back button
        } else {
            Toast.makeText(this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUser() {
        final String username = usernameField.getText().toString().trim();
        final String password = passwordField.getText().toString().trim();

        if (!validateRegistrationInput(username, password)) {
            return;
        }

        // All new registrations are standard users
        boolean success = dbHelper.registerUser(username, password, "user");

        if (!success) {
            Toast.makeText(this,
                    "User already exists. Please choose a different username.",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,
                    "User registered successfully. You can now log in.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
