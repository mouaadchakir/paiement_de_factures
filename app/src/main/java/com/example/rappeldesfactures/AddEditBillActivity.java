package com.example.rappeldesfactures;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditBillActivity extends AppCompatActivity {
    private EditText nameEditText;
    private EditText amountEditText;
    private EditText dueDateEditText;
    private CheckBox isPaidCheckBox;
    private Button saveButton;
    private Button deleteButton;

    private BillDatabaseHelper dbHelper;
    private long billId = -1;
    private Bill currentBill;

    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_bill);

        try {
            // Initialize date formatting objects
            calendar = Calendar.getInstance();
            dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            
            // Initialize UI components
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }
    
            // Initialize database helper
            dbHelper = new BillDatabaseHelper(this);
    
            // Initialize views
            nameEditText = findViewById(R.id.edit_bill_name);
            amountEditText = findViewById(R.id.edit_bill_amount);
            dueDateEditText = findViewById(R.id.edit_bill_due_date);
            isPaidCheckBox = findViewById(R.id.checkbox_is_paid);
            saveButton = findViewById(R.id.button_save);
            deleteButton = findViewById(R.id.button_delete);

            // Check if editing an existing bill
            if (getIntent() != null && getIntent().hasExtra("bill_id")) {
                billId = getIntent().getLongExtra("bill_id", -1);
    
                if (billId != -1) {
                    // Editing an existing bill
                    setTitle(R.string.edit_bill);
                    loadBillData();
                    if (deleteButton != null) {
                        deleteButton.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Adding a new bill
                    setTitle(R.string.add_bill);
                    if (deleteButton != null) {
                        deleteButton.setVisibility(View.GONE);
                    }
    
                    // Set default due date to tomorrow
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    updateDateDisplay();
                }
            } else {
                // Adding a new bill
                setTitle(R.string.add_bill);
                if (deleteButton != null) {
                    deleteButton.setVisibility(View.GONE);
                }
    
                // Set default due date to tomorrow
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                updateDateDisplay();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
        }

        // Set up date picker dialog for due date field
        if (dueDateEditText != null) {
            dueDateEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        showDatePickerDialog();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(AddEditBillActivity.this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Set up save button click listener
        if (saveButton != null) {
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        saveBill();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (AddEditBillActivity.this != null && !isFinishing()) {
                            Toast.makeText(AddEditBillActivity.this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        // Set up delete button click listener
        if (deleteButton != null) {
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        deleteBill();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (AddEditBillActivity.this != null && !isFinishing()) {
                            Toast.makeText(AddEditBillActivity.this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
    }

    private void loadBillData() {
        try {
            // Check that required objects are initialized
            if (calendar == null) {
                calendar = Calendar.getInstance();
            }
            
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            
            if (displayDateFormat == null) {
                displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            }
            
            // Load bill data from database
            if (dbHelper == null) {
                dbHelper = new BillDatabaseHelper(this);
            }
            
            if (dbHelper != null && billId != -1) {
                try {
                    currentBill = dbHelper.getBill(billId);
                    
                    if (currentBill != null) {
                        // Populate UI with bill data
                        if (nameEditText != null) {
                            nameEditText.setText(currentBill.getName());
                        }
                        
                        if (amountEditText != null) {
                            amountEditText.setText(String.valueOf(currentBill.getAmount()));
                        }
                        
                        if (isPaidCheckBox != null) {
                            isPaidCheckBox.setChecked(currentBill.isPaid());
                        }
                        
                        // Set calendar to bill's due date
                        try {
                            String dueDateStr = currentBill.getDueDate();
                            if (!TextUtils.isEmpty(dueDateStr)) {
                                Date dueDate = dateFormat.parse(dueDateStr);
                                if (dueDate != null) {
                                    calendar.setTime(dueDate);
                                    updateDateDisplay();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // If due date parsing fails, set to today
                            calendar.setTime(new Date());
                            updateDateDisplay();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
                // Last resort handling
            }
        }
    }

    private void showDatePickerDialog() {
        try {
            if (calendar == null) {
                calendar = Calendar.getInstance();
            }
            
            final Calendar tempCalendar = Calendar.getInstance();
            tempCalendar.setTimeInMillis(calendar.getTimeInMillis());
            
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            try {
                                if (calendar != null) {
                                    calendar.set(Calendar.YEAR, year);
                                    calendar.set(Calendar.MONTH, month);
                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                    updateDateDisplay();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(AddEditBillActivity.this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    tempCalendar.get(Calendar.YEAR),
                    tempCalendar.get(Calendar.MONTH),
                    tempCalendar.get(Calendar.DAY_OF_MONTH)
            );

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDateDisplay() {
        try {
            if (dueDateEditText != null && calendar != null && displayDateFormat != null) {
                dueDateEditText.setText(displayDateFormat.format(calendar.getTime()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Don't show a toast here as this might create an infinite loop in some edge cases
        }
    }

    private void saveBill() {
        try {
            // Check if required objects are initialized
            if (dateFormat == null || calendar == null) {
                calendar = Calendar.getInstance();
                dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            
            // Validate inputs
            if (nameEditText == null || amountEditText == null || dueDateEditText == null || isPaidCheckBox == null) {
                Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if database helper is initialized
            if (dbHelper == null) {
                dbHelper = new BillDatabaseHelper(this);
                if (dbHelper == null) {
                    Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Get and validate the bill name
            String name = "";
            if (nameEditText.getText() != null) {
                name = nameEditText.getText().toString().trim();
            }
            if (TextUtils.isEmpty(name)) {
                nameEditText.setError(getString(R.string.error_field_required));
                nameEditText.requestFocus();
                return;
            }

            // Get and validate the amount
            String amountStr = "";
            if (amountEditText.getText() != null) {
                amountStr = amountEditText.getText().toString().trim();
            }
            if (TextUtils.isEmpty(amountStr)) {
                amountEditText.setError(getString(R.string.error_field_required));
                amountEditText.requestFocus();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    amountEditText.setError(getString(R.string.error_invalid_amount));
                    amountEditText.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                amountEditText.setError(getString(R.string.error_invalid_amount));
                amountEditText.requestFocus();
                return;
            }

            // Get due date display value (just for validation)
            String dueDateDisplay = "";
            if (dueDateEditText.getText() != null) {
                dueDateDisplay = dueDateEditText.getText().toString().trim();
            }
            if (TextUtils.isEmpty(dueDateDisplay)) {
                dueDateEditText.setError(getString(R.string.error_field_required));
                dueDateEditText.requestFocus();
                return;
            }

            // Format due date for storage - using calendar which was set by the date picker
            String dueDate = dateFormat.format(calendar.getTime());
            boolean isPaid = isPaidCheckBox.isChecked();

            // Check if we're adding a new bill or updating an existing one
            long result = -1;
            if (billId == -1) {
                try {
                    // Add new bill
                    Bill newBill = new Bill(0, name, amount, dueDate, isPaid);
                    result = dbHelper.addBill(newBill);
    
                    // If bill was added successfully and is not marked as paid, schedule notification
                    if (result > 0 && !isPaid) {
                        try {
                            NotificationHelper.scheduleBillReminder(this, (int) result, name, amount, dueDate);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Continue even if notification scheduling fails
                        }
                    }
    
                    if (result > 0) {
                        Toast.makeText(this, R.string.bill_added, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.error_adding_bill, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.error_adding_bill, Toast.LENGTH_SHORT).show();
                }
            } else {
                try {
                    // Default wasPaid to opposite of current isPaid
                    boolean wasPaid = false;
    
                    // Update existing bill
                    if (currentBill == null) {
                        // If currentBill is null, create a new one
                        currentBill = new Bill(billId, name, amount, dueDate, isPaid);
                        if (dbHelper != null) {
                            result = dbHelper.addBill(currentBill);
                        } else {
                            Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        // Update existing bill
                        currentBill.setName(name);
                        currentBill.setAmount(amount);
                        currentBill.setDueDate(dueDate);
    
                        // Check if payment status changed
                        wasPaid = currentBill.isPaid();
                        currentBill.setPaid(isPaid);
    
                        if (dbHelper != null) {
                            result = dbHelper.updateBill(currentBill);
                        } else {
                            Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
    
                    if (result > 0) {
                        try {
                            // If bill was unpaid and now is paid, cancel notification
                            if (!wasPaid && isPaid) {
                                NotificationHelper.cancelBillReminder(this, (int) billId);
                            }
                            // If bill was paid and now is unpaid, schedule notification
                            else if (wasPaid && !isPaid) {
                                NotificationHelper.scheduleBillReminder(this, (int) billId, name, amount, dueDate);
                            }
                            // If bill remains unpaid but details changed, update notification
                            else if (!isPaid) {
                                NotificationHelper.updateBillReminder(this, (int) billId, name, amount, dueDate);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Continue even if notification handling fails
                        }
    
                        Toast.makeText(this, R.string.bill_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteBill() {
        try {
            if (billId != -1) {
                // Check if database helper is initialized
                if (dbHelper == null) {
                    dbHelper = new BillDatabaseHelper(this);
                }
                
                // Delete from database
                if (dbHelper != null) {
                    try {
                        dbHelper.deleteBill(billId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                    return;
                }

                // Cancel any scheduled notifications for this bill
                try {
                    NotificationHelper.cancelBillReminder(this, (int) billId);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Continue even if notification cancellation fails
                }

                Toast.makeText(this, R.string.bill_deleted, Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
                // Last resort handling
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}