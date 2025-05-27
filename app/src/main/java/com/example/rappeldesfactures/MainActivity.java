package com.example.rappeldesfactures;


import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private ListView billListView;
    private BillAdapter billAdapter;
    private List<Bill> billList;
    private List<Bill> filteredBillList;
    private BillDatabaseHelper dbHelper;
    private TextView emptyTextView;
    private Spinner sortSpinner;
    private TextView thisMonthAmountView;
    private TextView overdueCountView;
    private SimpleDateFormat dateFormat;
    private int currentSortOption = 0; // 0 = by date, 1 = by amount, 2 = by name, 3 = by status

    // Codes pour demander les permissions
    private static final int NOTIFICATION_PERMISSION_CODE = 123;
    private static final int EXACT_ALARM_PERMISSION_CODE = 124;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Demander la permission des notifications pour Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }

        // Vérifier la permission pour les alarmes exactes sur Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // Afficher un dialogue expliquant pourquoi nous avons besoin de cette permission
                new AlertDialog.Builder(this)
                        .setTitle("Permission requise")
                        .setMessage("Pour que les notifications de rappel fonctionnent à l'heure exacte, l'application a besoin de la permission de programmer des alarmes exactes. Veuillez l'activer dans les paramètres.")
                        .setPositiveButton("Ouvrir les paramètres", (dialog, which) -> {
                            // Ouvrir les paramètres pour activer cette permission
                            Intent intent = new Intent();
                            intent.setAction(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            }
        }

        // Initialize date format
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);

        // Initialize UI components
        billListView = findViewById(R.id.bill_list_view);
        emptyTextView = findViewById(R.id.empty_view);
        billListView.setEmptyView(emptyTextView);
        thisMonthAmountView = findViewById(R.id.text_this_month_amount);
        overdueCountView = findViewById(R.id.text_overdue_count);

        // Initialize sorting spinner
        sortSpinner = findViewById(R.id.sort_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Add sorting options
        spinnerAdapter.add(getString(R.string.sort_due_date));
        spinnerAdapter.add(getString(R.string.sort_amount));
        spinnerAdapter.add(getString(R.string.sort_name));
        spinnerAdapter.add(getString(R.string.sort_status));

        sortSpinner.setAdapter(spinnerAdapter);
        sortSpinner.setOnItemSelectedListener(this);

        // Setup filter button
        findViewById(R.id.filter_button).setOnClickListener(v -> showFilterDialog());

        // Setup test notification button
        findViewById(R.id.test_notification_button).setOnClickListener(v -> testNotification());

        // Initialize database helper
        dbHelper = new BillDatabaseHelper(this);
        
        // Démarrer le service de notification
        startService(new Intent(this, BillNotificationService.class));

        // Initialize bills list and adapter
        billList = new ArrayList<>();
        filteredBillList = new ArrayList<>();
        billAdapter = new BillAdapter(this, filteredBillList);
        billListView.setAdapter(billAdapter);

        // Setup FAB for adding a new bill
        FloatingActionButton fab = findViewById(R.id.fab_add_bill);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent(MainActivity.this, AddEditBillActivity.class);
                        // Add a flag to create a new task - this can help prevent some crashes
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, R.string.error_updating_bill, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Set click listener for bill items
        billListView.setOnItemClickListener((parent, view, position, id) -> {
            Bill selectedBill = filteredBillList.get(position);
            Intent intent = new Intent(MainActivity.this, AddEditBillActivity.class);
            intent.putExtra("bill_id", selectedBill.getId());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBillsFromDatabase();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            loadBillsFromDatabase();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadBillsFromDatabase() {
        billList.clear();
        billList.addAll(dbHelper.getAllBills());

        // Apply current sort and filter
        applyCurrentSortAndFilter();

        // Update summary data
        updateSummaryData();
    }

    private void applyCurrentSortAndFilter() {
        // First apply sort
        sortBills(currentSortOption);

        // Then apply current filter (no filter initially, so show all)
        filteredBillList.clear();
        filteredBillList.addAll(billList);

        billAdapter.notifyDataSetChanged();
    }

    private void sortBills(int sortOption) {
        currentSortOption = sortOption;

        switch (sortOption) {
            case 0: // Sort by due date (soonest first)
                Collections.sort(billList, (bill1, bill2) -> {
                    try {
                        Date date1 = dateFormat.parse(bill1.getDueDate());
                        Date date2 = dateFormat.parse(bill2.getDueDate());
                        if (date1 != null && date2 != null) {
                            return date1.compareTo(date2);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    return 0;
                });
                break;

            case 1: // Sort by amount (highest first)
                Collections.sort(billList, (bill1, bill2) ->
                        Double.compare(bill2.getAmount(), bill1.getAmount()));
                break;

            case 2: // Sort by name (A-Z)
                Collections.sort(billList, (bill1, bill2) -> {
                    String name1 = bill1.getName() != null ? bill1.getName() : "";
                    String name2 = bill2.getName() != null ? bill2.getName() : "";
                    return name1.compareToIgnoreCase(name2);
                });
                break;

            case 3: // Sort by status (overdue first, then due soon, then upcoming, then paid)
                Collections.sort(billList, (bill1, bill2) -> {
                    int status1 = getBillStatusValue(bill1);
                    int status2 = getBillStatusValue(bill2);
                    return Integer.compare(status1, status2);
                });
                break;
        }
    }

    private int getBillStatusValue(Bill bill) {
        if (bill.isPaid()) {
            return 4; // Paid bills come last
        }

        try {
            Date dueDate = dateFormat.parse(bill.getDueDate());
            if (dueDate == null) return 3; // Default to upcoming

            Date today = Calendar.getInstance().getTime();

            if (dueDate.before(today)) {
                return 0; // Overdue bills come first
            }

            // Calculate days difference
            long diffInMillis = dueDate.getTime() - today.getTime();
            long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);

            if (diffInDays == 0) {
                return 1; // Due today comes second
            } else if (diffInDays <= 3) {
                return 2; // Due soon comes third
            } else {
                return 3; // Upcoming comes fourth yaka ir ivbbi kjjad chia hata tta  hta ht
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return 3; // Default to upcoming
        }
    }

    private void updateSummaryData() {
        double thisMonthTotal = 0;
        int overdueCount = 0;

        // Get current month and year
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);

        for (Bill bill : billList) {
            // Skip paid bills
            if (bill.isPaid()) continue;

            try {
                // Check if bill is overdue
                Date dueDate = dateFormat.parse(bill.getDueDate());
                if (dueDate != null && dueDate.before(Calendar.getInstance().getTime())) {
                    overdueCount++;
                }

                // Check if bill is due this month
                Calendar dueCal = Calendar.getInstance();
                dueCal.setTime(dueDate != null ? dueDate : new Date());
                int billMonth = dueCal.get(Calendar.MONTH);
                int billYear = dueCal.get(Calendar.YEAR);

                if (billMonth == currentMonth && billYear == currentYear) {
                    thisMonthTotal += bill.getAmount();
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // Update the UI
        thisMonthAmountView.setText(String.format(Locale.getDefault(),
                getString(R.string.amount_format), thisMonthTotal));
        overdueCountView.setText(String.valueOf(overdueCount));
    }

    private void showFilterDialog() {
        // Create a dialog to show filtering options
        // This is a simple implementation - you could extend this with more options
        final String[] options = {"Toutes les factures", "Factures non payées", "Factures en retard", "Factures ce mois-ci"};
        final boolean[] selectedOptions = {true, false, false, false};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filtrer les factures")
                .setMultiChoiceItems(options, selectedOptions, (dialog, which, isChecked) -> {
                    // If "All Bills" is selected, deselect others
                    if (which == 0 && isChecked) {
                        for (int i = 1; i < selectedOptions.length; i++) {
                            selectedOptions[i] = false;
                            ((AlertDialog) dialog).getListView().setItemChecked(i, false);
                        }
                    }
                    // If any other option is selected, deselect "All Bills"
                    else if (which > 0 && isChecked) {
                        selectedOptions[0] = false;
                        ((AlertDialog) dialog).getListView().setItemChecked(0, false);
                    }

                    // If nothing is selected, select "All Bills"
                    boolean anySelected = false;
                    for (boolean selected : selectedOptions) {
                        if (selected) {
                            anySelected = true;
                            break;
                        }
                    }

                    if (!anySelected) {
                        selectedOptions[0] = true;
                        ((AlertDialog) dialog).getListView().setItemChecked(0, true);
                    }
                })
                .setPositiveButton("Appliquer", (dialog, id) -> {
                    // Apply the filter
                    applyFilter(selectedOptions);
                })
                .setNegativeButton("Annuler", (dialog, id) -> dialog.dismiss());

        builder.create().show();
    }

    private void applyFilter(boolean[] filterOptions) {
        filteredBillList.clear();

        // If "All Bills" is selected or no filter is applied
        if (filterOptions[0]) {
            filteredBillList.addAll(billList);
        } else {
            // Get current month and year for filtering
            Calendar cal = Calendar.getInstance();
            int currentMonth = cal.get(Calendar.MONTH);
            int currentYear = cal.get(Calendar.YEAR);
            Date today = cal.getTime();

            for (Bill bill : billList) {
                boolean shouldAdd = false;

                // Unpaid bills filter
                if (filterOptions[1] && !bill.isPaid()) {
                    shouldAdd = true;
                }

                // Overdue bills filter
                if (filterOptions[2] && !bill.isPaid()) {
                    try {
                        Date dueDate = dateFormat.parse(bill.getDueDate());
                        if (dueDate != null && dueDate.before(today)) {
                            shouldAdd = true;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                // This month's bills filter
                if (filterOptions[3]) {
                    try {
                        Date dueDate = dateFormat.parse(bill.getDueDate());
                        if (dueDate != null) {
                            Calendar dueCal = Calendar.getInstance();
                            dueCal.setTime(dueDate);
                            int billMonth = dueCal.get(Calendar.MONTH);
                            int billYear = dueCal.get(Calendar.YEAR);

                            if (billMonth == currentMonth && billYear == currentYear) {
                                shouldAdd = true;
                            }
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                if (shouldAdd) {
                    filteredBillList.add(bill);
                }
            }
        }

        billAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // Cette méthode est appelée quand un élément est sélectionné dans le spinner
        if (parent.getId() == R.id.sort_spinner) {
            sortBills(position);
            applyCurrentSortAndFilter();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Cette méthode est appelée quand aucun élément n'est sélectionné
        // Rien à faire ici, mais l'implémentation est requise
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, maintenant on peut programmer des notifications
                Toast.makeText(this, "Permission de notification accordée", Toast.LENGTH_SHORT).show();

                // Reprogrammer les notifications pour toutes les factures à venir
                new Thread(() -> {
                    BillDatabaseHelper dbHelper = new BillDatabaseHelper(this);
                    List<Bill> upcomingBills = dbHelper.getUpcomingBills(30);
                    for (Bill bill : upcomingBills) {
                        if (!bill.isPaid()) {
                            NotificationHelper.scheduleBillReminder(this, (int) bill.getId(),
                                    bill.getName(), bill.getAmount(), bill.getDueDate());
                        }
                    }
                }).start();
            } else {
                // Permission refusée
                Toast.makeText(this, "Les notifications sont désactivées. Vous ne recevrez pas de rappels pour vos factures.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Méthode pour tester les notifications immédiatement
    private void testNotification() {
        // Tester si la permission est accordée
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission de notification non accordée. Autorisation demandée.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
                return;
            }
        }

        // Créer une notification de test
        Toast.makeText(this, "Envoi d'une notification de test...", Toast.LENGTH_SHORT).show();

        // Créer une notification immédiate
        Intent notificationIntent = new Intent(this, NotificationReceiver.class);
        notificationIntent.putExtra("bill_id", 9999); // ID spécial pour test
        notificationIntent.putExtra("bill_name", "Facture de Test");
        notificationIntent.putExtra("bill_amount", 100.0);
        notificationIntent.putExtra("bill_due_date", dateFormat.format(Calendar.getInstance().getTime()));

        sendBroadcast(notificationIntent);
    }
}
