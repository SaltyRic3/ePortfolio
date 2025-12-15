package com.example.cs360projecttwo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DataGridActivity extends AppCompatActivity {

    private TableLayout dataTable;
    private EditText itemNameInput, itemQuantityInput;
    private DatabaseHelper dbHelper;
    private boolean isAdmin = false; // role-based access flag

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_grid);

        // Make sure the user is logged in before using this screen
        if (!checkAuth()) {
            return; // We already redirected to LoginActivity
        }

        readUserRole(); // sets isAdmin based on saved role

        dbHelper = new DatabaseHelper(this);

        dataTable = findViewById(R.id.dataTable);
        itemNameInput = findViewById(R.id.itemNameInput);
        itemQuantityInput = findViewById(R.id.itemQuantityInput);
        Button addItemButton = findViewById(R.id.addItemButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        addItemButton.setOnClickListener(v -> addItemToDatabase());
        logoutButton.setOnClickListener(v -> logout());

        loadInventoryItems();
        setupBottomNav();
    }

    private void readUserRole() {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String role = prefs.getString("userRole", "user");
        isAdmin = "admin".equalsIgnoreCase(role);
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
            finish(); // Prevent returning to this screen
            return false;
        }
        return true;
    }

    private void addItemToDatabase() {
        String itemName = itemNameInput.getText().toString().trim();
        String quantityStr = itemQuantityInput.getText().toString().trim();

        if (itemName.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_name_and_quantity), Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                Toast.makeText(this, "Quantity must be a positive number.", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException ex) {
            Toast.makeText(this, "Please enter a valid number for quantity.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("INSERT INTO inventory (item_name, item_quantity) VALUES (?, ?)",
                new Object[]{itemName, quantity});
        db.close();

        itemNameInput.setText("");
        itemQuantityInput.setText("");
        loadInventoryItems();
    }

    private void loadInventoryItems() {
        dataTable.removeAllViews();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM inventory", null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("item_id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
            int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("item_quantity"));

            TableRow row = new TableRow(this);

            TextView itemCell = new TextView(this);
            itemCell.setText(name);
            itemCell.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            itemCell.setPadding(8, 8, 8, 8);

            TextView quantityCell = new TextView(this);
            quantityCell.setText(String.valueOf(quantity));
            quantityCell.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            quantityCell.setPadding(8, 8, 8, 8);

            // Buttons Layout
            LinearLayout buttonLayout = new LinearLayout(this);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setLayoutParams(new TableRow.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            // Update Button
            Button updateButton = new Button(this);
            updateButton.setText(getString(R.string.button_update));
            updateButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            updateButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));
            updateButton.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1));
            updateButton.setPadding(8, 8, 8, 8);
            updateButton.setOnClickListener(v -> showUpdateDialog(id, quantity));

            // Delete Button
            Button deleteButton = new Button(this);
            deleteButton.setText(getString(R.string.button_delete));
            deleteButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            deleteButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1));
            deleteButton.setPadding(8, 8, 8, 8);
            deleteButton.setOnClickListener(v -> deleteItemFromDatabase(id));

            // If not admin, disable update/delete buttons (view-only role)
            if (!isAdmin) {
                updateButton.setEnabled(false);
                updateButton.setAlpha(0.4f);
                deleteButton.setEnabled(false);
                deleteButton.setAlpha(0.4f);
            }

            buttonLayout.addView(updateButton);
            buttonLayout.addView(deleteButton);

            row.addView(itemCell);
            row.addView(quantityCell);
            row.addView(buttonLayout);

            dataTable.addView(row);
        }

        cursor.close();
        db.close();
    }

    private void showUpdateDialog(int itemId, int currentQuantity) {
        // Hard check: block non-admins even if they somehow trigger the dialog
        if (!isAdmin) {
            Toast.makeText(this, "Only admin users may update inventory.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Item Quantity");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentQuantity));
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            // Extra safety check inside positive button
            if (!isAdmin) {
                Toast.makeText(this, "Unauthorized action.", Toast.LENGTH_SHORT).show();
                return;
            }

            String newQuantityStr = input.getText().toString().trim();
            if (!newQuantityStr.isEmpty()) {
                try {
                    int newQuantity = Integer.parseInt(newQuantityStr);
                    if (newQuantity <= 0) {
                        Toast.makeText(this, "Quantity must be positive.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.execSQL("UPDATE inventory SET item_quantity=? WHERE item_id=?",
                            new Object[]{newQuantity, itemId});
                    db.close();
                    loadInventoryItems();
                } catch (NumberFormatException ex) {
                    Toast.makeText(this, "Please enter a valid number for quantity.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void deleteItemFromDatabase(int itemId) {
        // Hard check: block non-admins from deleting
        if (!isAdmin) {
            Toast.makeText(this, "Only admin users may delete inventory.", Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM inventory WHERE item_id=?", new Object[]{itemId});
        db.close();
        loadInventoryItems();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_grid);

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
                return true;
            } else if (id == R.id.nav_sms) {
                startActivity(new Intent(this, SMSActivity.class));
                return true;
            }
            return false;
        });
    }

    /**
     * Logout: clear auth state and go back to LoginActivity.
     */
    private void logout() {
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .putString("userRole", "user")
                .apply();

        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
