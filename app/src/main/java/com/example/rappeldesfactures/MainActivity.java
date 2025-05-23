package com.example.rappeldesfactures;


import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ListView billListView;
    private BillAdapter billAdapter;
    private List<Bill> billList;
    private BillDatabaseHelper dbHelper;
    private TextView emptyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_name);
        
        // Initialize UI components //
        billListView = findViewById(R.id.bill_list_view);
        emptyTextView = findViewById(R.id.empty_view);
        billListView.setEmptyView(emptyTextView);
        
        // Initialize database helper
        dbHelper = new BillDatabaseHelper(this);
        
        // Initialize bills list and adapter
        billList = new ArrayList<>();
        billAdapter = new BillAdapter(this, billList);
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
            Bill selectedBill = billList.get(position);
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
        billAdapter.notifyDataSetChanged();
    }
}
