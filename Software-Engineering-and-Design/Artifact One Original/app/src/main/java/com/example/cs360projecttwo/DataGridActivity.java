package com.example.cs360projecttwo;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_grid);

        dbHelper = new DatabaseHelper(this);

        dataTable = findViewById(R.id.dataTable);
        itemNameInput = findViewById(R.id.itemNameInput);
        itemQuantityInput = findViewById(R.id.itemQuantityInput);
        Button addItemButton = findViewById(R.id.addItemButton);

        addItemButton.setOnClickListener(v -> addItemToDatabase());

        loadInventoryItems();
        setupBottomNav();
    }

    private void addItemToDatabase() {
        String itemName = itemNameInput.getText().toString().trim();
        String quantityStr = itemQuantityInput.getText().toString().trim();

        if (itemName.isEmpty() || quantityStr.isEmpty()) {
            Toast.makeText(this, getString(R.string.enter_name_and_quantity), Toast.LENGTH_SHORT).show();
            return;
        }

        int quantity = Integer.parseInt(quantityStr);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("INSERT INTO inventory (item_name, item_quantity) VALUES (?, ?)", new Object[]{itemName, quantity});
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
            buttonLayout.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // Update Button
            Button updateButton = new Button(this);
            updateButton.setText(getString(R.string.button_update));
            updateButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            updateButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));
            updateButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            updateButton.setPadding(8, 8, 8, 8);
            updateButton.setOnClickListener(v -> showUpdateDialog(id, quantity));

            // Delete Button
            Button deleteButton = new Button(this);
            deleteButton.setText(getString(R.string.button_delete));
            deleteButton.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            deleteButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            deleteButton.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            deleteButton.setPadding(8, 8, 8, 8);
            deleteButton.setOnClickListener(v -> deleteItemFromDatabase(id));

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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Item Quantity");

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(currentQuantity));
        builder.setView(input);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newQuantityStr = input.getText().toString().trim();
            if (!newQuantityStr.isEmpty()) {
                int newQuantity = Integer.parseInt(newQuantityStr);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("UPDATE inventory SET item_quantity=? WHERE item_id=?", new Object[]{newQuantity, itemId});
                db.close();
                loadInventoryItems();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void deleteItemFromDatabase(int itemId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM inventory WHERE item_id=?", new Object[]{itemId});
        db.close();
        loadInventoryItems();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_grid);

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
}
