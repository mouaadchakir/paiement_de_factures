package com.example.rappeldesfactures;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditBillActivity extends AppCompatActivity {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private EditText nameEditText, amountEditText, dueDateEditText, recurrenceIntervalEditText;
    private CheckBox isPaidCheckBox, isRecurringCheckBox;
    private Button saveButton, deleteButton;
    private Spinner recurrenceTypeSpinner;
    private LinearLayout recurringOptionsLayout;
    private TextView intervalUnitTextView;

    private BillDatabaseHelper dbHelper;
    private long billId = -1;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_bill);

        // Initialize calendar
        calendar = Calendar.getInstance();

        // Initialize database helper
        dbHelper = new BillDatabaseHelper(this);

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        nameEditText = findViewById(R.id.edit_bill_name);
        amountEditText = findViewById(R.id.edit_bill_amount);
        dueDateEditText = findViewById(R.id.edit_bill_due_date);
        isPaidCheckBox = findViewById(R.id.checkbox_is_paid);
        saveButton = findViewById(R.id.button_save);
        deleteButton = findViewById(R.id.button_delete);

        // Recurring bill UI elements
        isRecurringCheckBox = findViewById(R.id.checkbox_is_recurring);
        recurringOptionsLayout = findViewById(R.id.recurring_options);
        recurrenceTypeSpinner = findViewById(R.id.spinner_recurrence_type);
        recurrenceIntervalEditText = findViewById(R.id.edit_recurrence_interval);
        intervalUnitTextView = findViewById(R.id.text_interval_unit);

        // Setup recurrence type spinner
        setupRecurrenceTypeSpinner();

        // Setup recurring checkbox listener
        isRecurringCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            recurringOptionsLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Set date picker for due date field
        dueDateEditText.setOnClickListener(v -> showDatePickerDialog());

        // Set up save button click listener
        saveButton.setOnClickListener(v -> saveBill());

        // Set up delete button click listener
        deleteButton.setOnClickListener(v -> deleteBill());

        // Check if editing an existing bill
        if (getIntent() != null && getIntent().hasExtra("bill_id")) {
            billId = getIntent().getLongExtra("bill_id", -1);

            if (billId != -1) {
                // Editing an existing bill
                setTitle(R.string.edit_bill);
                loadBillData();
            } else {
                // Adding a new bill
                setTitle(R.string.add_bill);
                deleteButton.setVisibility(View.GONE);

                // Set default due date to tomorrow
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                updateDateDisplay();
            }
        } else {
            // Adding a new bill
            setTitle(R.string.add_bill);
            deleteButton.setVisibility(View.GONE);

            // Set default due date to tomorrow
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDateDisplay();
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateDisplay();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void updateDateDisplay() {
        dueDateEditText.setText(DISPLAY_DATE_FORMAT.format(calendar.getTime()));
    }

    private void setupRecurrenceTypeSpinner() {
        // Create an array adapter with recurrence options
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        adapter.add(getString(R.string.days));
        adapter.add(getString(R.string.weeks));
        adapter.add(getString(R.string.months));
        adapter.add(getString(R.string.years));

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recurrenceTypeSpinner.setAdapter(adapter);

        // Default to monthly recurrence
        recurrenceTypeSpinner.setSelection(2); // RECURRENCE_MONTHLY - 1

        // Set up listener to update interval unit text
        recurrenceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateIntervalUnitText(position + 1); // +1 because our constants start at 1
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void updateIntervalUnitText(int recurrenceType) {
        switch (recurrenceType) {
            case Bill.RECURRENCE_DAILY:
                intervalUnitTextView.setText(R.string.days);
                break;
            case Bill.RECURRENCE_WEEKLY:
                intervalUnitTextView.setText(R.string.weeks);
                break;
            case Bill.RECURRENCE_MONTHLY:
                intervalUnitTextView.setText(R.string.months);
                break;
            case Bill.RECURRENCE_YEARLY:
                intervalUnitTextView.setText(R.string.years);
                break;
        }
    }

    private void loadBillData() {
        if (billId != -1) {
            Bill bill = dbHelper.getBillById(billId);
            if (bill != null) {
                nameEditText.setText(bill.getName());
                amountEditText.setText(String.format(Locale.getDefault(), "%.2f", bill.getAmount()));
                dueDateEditText.setText(DISPLAY_DATE_FORMAT.format(bill.getDueDateAsDate()));
                isPaidCheckBox.setChecked(bill.isPaid());

                // Set recurring bill options
                boolean isRecurring = bill.isRecurring();
                isRecurringCheckBox.setChecked(isRecurring);
                recurringOptionsLayout.setVisibility(isRecurring ? View.VISIBLE : View.GONE);

                if (isRecurring) {
                    recurrenceTypeSpinner.setSelection(bill.getRecurrenceType() - 1); // -1 because RECURRENCE_NONE = 0 and we don't show it in spinner
                    recurrenceIntervalEditText.setText(String.valueOf(bill.getRecurrenceInterval()));
                    updateIntervalUnitText(bill.getRecurrenceType());
                }

                // Set calendar to the bill's due date for date picker
                try {
                    Date dueDate = DATE_FORMAT.parse(bill.getDueDate());
                    if (dueDate != null) {
                        calendar.setTime(dueDate);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Set default due date to today
            updateDateDisplay();
            deleteButton.setVisibility(View.GONE);

            // Default recurring settings
            isRecurringCheckBox.setChecked(false);
            recurringOptionsLayout.setVisibility(View.GONE);
            recurrenceTypeSpinner.setSelection(Bill.RECURRENCE_MONTHLY - 1); // Default to monthly
            recurrenceIntervalEditText.setText("1");
        }
    }

    private void saveBill() {
        String name = nameEditText.getText().toString().trim();
        String amountStr = amountEditText.getText().toString().trim();
        String dueDate = DATE_FORMAT.format(calendar.getTime());
        boolean isPaid = isPaidCheckBox.isChecked();
        boolean isRecurring = isRecurringCheckBox.isChecked();

        // Get recurrence settings if bill is recurring
        int recurrenceType = Bill.RECURRENCE_NONE;
        int recurrenceInterval = 1;

        if (isRecurring) {
            recurrenceType = recurrenceTypeSpinner.getSelectedItemPosition() + 1; // +1 because spinner starts at 0 but our constants start at 1

            try {
                recurrenceInterval = Integer.parseInt(recurrenceIntervalEditText.getText().toString());
                if (recurrenceInterval < 1) recurrenceInterval = 1;
            } catch (NumberFormatException e) {
                recurrenceInterval = 1;
            }
        }

        // Validate inputs
        if (name.isEmpty()) {
            nameEditText.setError(getString(R.string.error_field_required));
            nameEditText.requestFocus();
            return;
        }

        if (amountStr.isEmpty()) {
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

        Bill bill;
        if (billId != -1) {
            bill = dbHelper.getBillById(billId);
            if (bill == null) {
                bill = new Bill();
                bill.setId(billId);
            }
        } else {
            bill = new Bill();
        }

        bill.setName(name);
        bill.setAmount(amount);
        bill.setDueDate(dueDate);
        bill.setPaid(isPaid);
        bill.setRecurrenceType(isRecurring ? recurrenceType : Bill.RECURRENCE_NONE);
        bill.setRecurrenceInterval(recurrenceInterval);

        long result;
        if (billId != -1) {
            result = dbHelper.updateBill(bill);
            if (result > 0) {
                Toast.makeText(this, R.string.bill_updated, Toast.LENGTH_SHORT).show();
                // Update notification for this bill if it was updated
                if (!isPaid) {
                    scheduleNotification(bill);
                } else if (isRecurring) {
                    // If a recurring bill is marked as paid, create the next occurrence
                    createNextRecurrence(bill);
                }
            } else {
                Toast.makeText(this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
            }
        } else {
            result = dbHelper.addBill(bill);
            if (result > 0) {
                bill.setId(result); // Set the ID of the new bill
                Toast.makeText(this, R.string.bill_added, Toast.LENGTH_SHORT).show();
                // Schedule notification for new bill
                if (!isPaid) {
                    scheduleNotification(bill);
                }
            } else {
                Toast.makeText(this, R.string.error_adding_bill, Toast.LENGTH_SHORT).show();
            }
        }

        if (result > 0) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void deleteBill() {
        if (billId == -1) {
            return; // Nothing to delete
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_bill_title)
                .setMessage(R.string.delete_bill_message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    if (dbHelper != null) {
                        int result = dbHelper.deleteBill(billId);
                        if (result > 0) {
                            // Cancel any notifications for this bill
                            cancelNotification(billId);
                            Toast.makeText(AddEditBillActivity.this, R.string.bill_deleted, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(AddEditBillActivity.this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void createNextRecurrence(Bill bill) {
        // If bill is not recurring, don't create next recurrence
        if (!bill.isRecurring()) {
            return;
        }

        // Create a new bill for the next due date
        Bill nextBill = new Bill();
        nextBill.setName(bill.getName());
        nextBill.setAmount(bill.getAmount());
        nextBill.setDueDate(bill.getNextDueDate());
        nextBill.setPaid(false);
        nextBill.setRecurrenceType(bill.getRecurrenceType());
        nextBill.setRecurrenceInterval(bill.getRecurrenceInterval());

        // Save the next occurrence
        long nextBillId = dbHelper.addBill(nextBill);
        if (nextBillId > 0) {
            nextBill.setId(nextBillId);
            scheduleNotification(nextBill);
        }
    }

    private void scheduleNotification(Bill bill) {
        NotificationHelper.scheduleBillReminder(this, (int) bill.getId(),
                bill.getName(), bill.getAmount(), bill.getDueDate());
    }

    private void cancelNotification(long billId) {
        NotificationHelper.cancelBillReminder(this, (int) billId);
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