package com.example.rappeldesfactures;



import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BillDatabaseHelper extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "billReminder.db";
    private static final int DATABASE_VERSION = 1;

    // Table Names
    private static final String TABLE_BILLS = "bills";

    // Bill Table Columns
    private static final String KEY_BILL_ID = "id";
    private static final String KEY_BILL_NAME = "name";
    private static final String KEY_BILL_AMOUNT = "amount";
    private static final String KEY_BILL_DUE_DATE = "due_date";
    private static final String KEY_BILL_IS_PAID = "is_paid";

    public BillDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_BILLS_TABLE = "CREATE TABLE " + TABLE_BILLS +
                "(" +
                KEY_BILL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_BILL_NAME + " TEXT NOT NULL," +
                KEY_BILL_AMOUNT + " REAL NOT NULL," +
                KEY_BILL_DUE_DATE + " TEXT NOT NULL," +
                KEY_BILL_IS_PAID + " INTEGER DEFAULT 0" +
                ")";

        db.execSQL(CREATE_BILLS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // Simplest implementation is to drop all old tables and recreate them
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_BILLS);
            onCreate(db);
        }
    }

    // Insert a bill into the database
    public long addBill(Bill bill) {
        SQLiteDatabase db = getWritableDatabase();
        long billId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_BILL_NAME, bill.getName());
            values.put(KEY_BILL_AMOUNT, bill.getAmount());
            values.put(KEY_BILL_DUE_DATE, bill.getDueDate());
            values.put(KEY_BILL_IS_PAID, bill.isPaid() ? 1 : 0);

            billId = db.insertOrThrow(TABLE_BILLS, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return billId;
    }

    // Get all bills from the database
    public List<Bill> getAllBills() {
        List<Bill> bills = new ArrayList<>();

        String BILLS_SELECT_QUERY = "SELECT * FROM " + TABLE_BILLS + 
                                   " ORDER BY " + KEY_BILL_DUE_DATE + " ASC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(BILLS_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        Bill bill = new Bill();
                        int idIdx = cursor.getColumnIndexOrThrow(KEY_BILL_ID);
                        int nameIdx = cursor.getColumnIndexOrThrow(KEY_BILL_NAME);
                        int amountIdx = cursor.getColumnIndexOrThrow(KEY_BILL_AMOUNT);
                        int dueDateIdx = cursor.getColumnIndexOrThrow(KEY_BILL_DUE_DATE);
                        int isPaidIdx = cursor.getColumnIndexOrThrow(KEY_BILL_IS_PAID);
                        
                        bill.setId(cursor.getLong(idIdx));
                        bill.setName(cursor.getString(nameIdx));
                        bill.setAmount(cursor.getDouble(amountIdx));
                        bill.setDueDate(cursor.getString(dueDateIdx));
                        bill.setPaid(cursor.getInt(isPaidIdx) == 1);
    
                        bills.add(bill);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return bills;
    }

    // Get unpaid bills due within a certain number of days
    public List<Bill> getUpcomingBills(int daysThreshold) {
        List<Bill> bills = new ArrayList<>();
        List<Bill> allBills = getAllBills();
        
        for (Bill bill : allBills) {
            if (!bill.isPaid() && bill.isDueSoon(daysThreshold)) {
                bills.add(bill);
            }
        }
        
        return bills;
    }

    // Get a single bill by ID
    public Bill getBill(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Bill bill = null;

        String BILL_SELECT_QUERY = "SELECT * FROM " + TABLE_BILLS + 
                                  " WHERE " + KEY_BILL_ID + " = ?";
        
        Cursor cursor = db.rawQuery(BILL_SELECT_QUERY, new String[]{String.valueOf(id)});
        try {
            if (cursor.moveToFirst()) {
                try {
                    bill = new Bill();
                    int idIdx = cursor.getColumnIndexOrThrow(KEY_BILL_ID);
                    int nameIdx = cursor.getColumnIndexOrThrow(KEY_BILL_NAME);
                    int amountIdx = cursor.getColumnIndexOrThrow(KEY_BILL_AMOUNT);
                    int dueDateIdx = cursor.getColumnIndexOrThrow(KEY_BILL_DUE_DATE);
                    int isPaidIdx = cursor.getColumnIndexOrThrow(KEY_BILL_IS_PAID);
                    
                    bill.setId(cursor.getLong(idIdx));
                    bill.setName(cursor.getString(nameIdx));
                    bill.setAmount(cursor.getDouble(amountIdx));
                    bill.setDueDate(cursor.getString(dueDateIdx));
                    bill.setPaid(cursor.getInt(isPaidIdx) == 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return bill;
    }

    // Update an existing bill
    public int updateBill(Bill bill) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsAffected = 0;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_BILL_NAME, bill.getName());
            values.put(KEY_BILL_AMOUNT, bill.getAmount());
            values.put(KEY_BILL_DUE_DATE, bill.getDueDate());
            values.put(KEY_BILL_IS_PAID, bill.isPaid() ? 1 : 0);

            rowsAffected = db.update(TABLE_BILLS, values, KEY_BILL_ID + " = ?", 
                                     new String[]{String.valueOf(bill.getId())});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return rowsAffected;
    }

    // Delete a bill
    public void deleteBill(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_BILLS, KEY_BILL_ID + " = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }
}
