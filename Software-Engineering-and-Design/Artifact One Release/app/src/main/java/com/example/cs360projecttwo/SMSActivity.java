package com.example.cs360projecttwo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SMSActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private TextView permissionStatus;
    private Button sendTestSmsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms);

        // 🔒 Make sure the user is logged in before using this screen
        if (!checkAuth()) {
            return;
        }

        permissionStatus = findViewById(R.id.permissionStatus);
        Button requestSmsButton = findViewById(R.id.requestSmsButton);
        sendTestSmsButton = findViewById(R.id.sendTestSmsButton);

        requestSmsButton.setOnClickListener(v -> checkSmsPermission());
        sendTestSmsButton.setOnClickListener(v -> sendTestSMS());
        sendTestSmsButton.setEnabled(false);

        setupBottomNav();
    }

    /**
     * Check whether the user is logged in.
     * If not, send them back to the LoginActivity.
     */
    private boolean checkAuth() {
        boolean loggedIn = getSharedPreferences("auth", MODE_PRIVATE)
                .getBoolean("isLoggedIn", false);

        if (!loggedIn) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return false;
        }
        return true;
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        } else {
            permissionStatus.setText(getString(R.string.sms_permission_granted));
            sendTestSmsButton.setEnabled(true);
        }
    }

    private void sendTestSMS() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String testNumber = "5554"; // Emulator number; replace for real device

            smsManager.sendTextMessage(testNumber, null,
                    "Test alert: Inventory low!", null, null);

            Toast.makeText(this, "Test SMS sent successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE) {
            boolean granted = grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;

            permissionStatus.setText(granted ?
                    getString(R.string.sms_permission_granted) :
                    getString(R.string.sms_permission_denied));

            sendTestSmsButton.setEnabled(granted);
        }
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_sms);

        // Hide Login tab when user is already logged in
        if (getSharedPreferences("auth", MODE_PRIVATE).getBoolean("isLoggedIn", false)) {
            bottomNav.getMenu().findItem(R.id.nav_login).setVisible(false);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_login) {
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            } else if (id == R.id.nav_grid) {
                startActivity(new Intent(this, DataGridActivity.class));
                return true;
            } else return id == R.id.nav_sms;
        });
    }
}
