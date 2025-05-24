package com.example.rappeldesfactures;



import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BillDatabaseHelper extends SQLiteOpenHelper {
    // Database Info
    private static final String DATABASE_NAME = "bills.db";
    private static final int DATABASE_VERSION = 2;

    // Table Names
    public static final String TABLE_BILLS = "bills";

    // Bill Table Columns
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_DUE_DATE = "due_date";
    public static final String COLUMN_IS_PAID = "is_paid";
    public static final String COLUMN_RECURRENCE_TYPE = "recurrence_type";
    public static final String COLUMN_RECURRENCE_INTERVAL = "recurrence_interval";

    public BillDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_BILLS_TABLE = "CREATE TABLE " + TABLE_BILLS +
                "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_NAME + " TEXT NOT NULL," +
                COLUMN_AMOUNT + " REAL NOT NULL," +
                COLUMN_DUE_DATE + " TEXT NOT NULL," +
                COLUMN_IS_PAID + " INTEGER NOT NULL," +
                COLUMN_RECURRENCE_TYPE + " INTEGER NOT NULL DEFAULT 0," +
                COLUMN_RECURRENCE_INTERVAL + " INTEGER NOT NULL DEFAULT 1" +
                ")";

        db.execSQL(CREATE_BILLS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add new columns for recurring bills
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + 
                      COLUMN_RECURRENCE_TYPE + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + 
                      COLUMN_RECURRENCE_INTERVAL + " INTEGER NOT NULL DEFAULT 1");
        }
    }

    // Insert a bill into the database
    public long addBill(Bill bill) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, bill.getName());
        values.put(COLUMN_AMOUNT, bill.getAmount());
        values.put(COLUMN_DUE_DATE, bill.getDueDate());
        values.put(COLUMN_IS_PAID, bill.isPaid() ? 1 : 0);
        values.put(COLUMN_RECURRENCE_TYPE, bill.getRecurrenceType());
        values.put(COLUMN_RECURRENCE_INTERVAL, bill.getRecurrenceInterval());

        SQLiteDatabase db = this.getWritableDatabase();
        long insertId = db.insert(TABLE_BILLS, null, values);
        db.close();
        return insertId;
    }

    // Get all bills from the database
    public List<Bill> getAllBills() {
        List<Bill> bills = new ArrayList<>();

        String BILLS_SELECT_QUERY = "SELECT * FROM " + TABLE_BILLS + 
                                   " ORDER BY " + COLUMN_DUE_DATE + " ASC";

        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(BILLS_SELECT_QUERY, null);
        try {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        Bill bill = new Bill();
                        int idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID);
                        int nameIdx = cursor.getColumnIndexOrThrow(COLUMN_NAME);
                        int amountIdx = cursor.getColumnIndexOrThrow(COLUMN_AMOUNT);
                        int dueDateIdx = cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE);
                        int isPaidIdx = cursor.getColumnIndexOrThrow(COLUMN_IS_PAID);
                        int recurrenceTypeIdx = cursor.getColumnIndexOrThrow(COLUMN_RECURRENCE_TYPE);
                        int recurrenceIntervalIdx = cursor.getColumnIndexOrThrow(COLUMN_RECURRENCE_INTERVAL);

                        bill.setId(cursor.getLong(idIdx));
                        bill.setName(cursor.getString(nameIdx));
                        bill.setAmount(cursor.getDouble(amountIdx));
                        bill.setDueDate(cursor.getString(dueDateIdx));
                        bill.setPaid(cursor.getInt(isPaidIdx) == 1);
                        bill.setRecurrenceType(cursor.getInt(recurrenceTypeIdx));
                        bill.setRecurrenceInterval(cursor.getInt(recurrenceIntervalIdx));

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

    // Get a single bill by ID
    public Bill getBillById(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_BILLS, null, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)}, null, null, null, null);

        Bill bill = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                bill = new Bill();
                bill.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
                bill.setName(cursor.getString(cursor.getColumnIndex(COLUMN_NAME)));
                bill.setAmount(cursor.getDouble(cursor.getColumnIndex(COLUMN_AMOUNT)));
                bill.setDueDate(cursor.getString(cursor.getColumnIndex(COLUMN_DUE_DATE)));
                bill.setPaid(cursor.getInt(cursor.getColumnIndex(COLUMN_IS_PAID)) == 1);
                
                // Get recurrence info if available
                int recurrenceTypeIndex = cursor.getColumnIndex(COLUMN_RECURRENCE_TYPE);
                if (recurrenceTypeIndex != -1) {
                    bill.setRecurrenceType(cursor.getInt(recurrenceTypeIndex));
                }
                
                int recurrenceIntervalIndex = cursor.getColumnIndex(COLUMN_RECURRENCE_INTERVAL);
                if (recurrenceIntervalIndex != -1) {
                    bill.setRecurrenceInterval(cursor.getInt(recurrenceIntervalIndex));
                }
            }
            cursor.close();
        }
        db.close();
        return bill;
    }

    // Update an existing bill
    public int updateBill(Bill bill) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, bill.getName());
        values.put(COLUMN_AMOUNT, bill.getAmount());
        values.put(COLUMN_DUE_DATE, bill.getDueDate());
        values.put(COLUMN_IS_PAID, bill.isPaid() ? 1 : 0);
        values.put(COLUMN_RECURRENCE_TYPE, bill.getRecurrenceType());
        values.put(COLUMN_RECURRENCE_INTERVAL, bill.getRecurrenceInterval());

        SQLiteDatabase db = this.getWritableDatabase();
        int rowsUpdated = db.update(TABLE_BILLS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(bill.getId())});
        db.close();
        return rowsUpdated;
    }

    // Delete a bill
    public int deleteBill(long id) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsDeleted = 0;
        db.beginTransaction();
        try {
            rowsDeleted = db.delete(TABLE_BILLS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
        db.close();
        return rowsDeleted;
    }
    
    // Get upcoming bills due within the next 'days' days
    public List<Bill> getUpcomingBills(int days) {
        List<Bill> upcomingBills = new ArrayList<>();
        
        // Get current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        String today = dateFormat.format(calendar.getTime());
        
        // Calculate future date (today + days)
        calendar.add(Calendar.DAY_OF_MONTH, days);
        String futureDate = dateFormat.format(calendar.getTime());
        
        // Query for upcoming bills
        String UPCOMING_BILLS_QUERY = "SELECT * FROM " + TABLE_BILLS + 
                                  " WHERE " + COLUMN_IS_PAID + " = 0 AND " +
                                  COLUMN_DUE_DATE + " BETWEEN ? AND ? " +
                                  "ORDER BY " + COLUMN_DUE_DATE + " ASC";
        
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(UPCOMING_BILLS_QUERY, new String[]{today, futureDate});
        
        try {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        Bill bill = new Bill();
                        int idIdx = cursor.getColumnIndexOrThrow(COLUMN_ID);
                        int nameIdx = cursor.getColumnIndexOrThrow(COLUMN_NAME);
                        int amountIdx = cursor.getColumnIndexOrThrow(COLUMN_AMOUNT);
                        int dueDateIdx = cursor.getColumnIndexOrThrow(COLUMN_DUE_DATE);
                        int isPaidIdx = cursor.getColumnIndexOrThrow(COLUMN_IS_PAID);
                        int recurrenceTypeIdx = cursor.getColumnIndexOrThrow(COLUMN_RECURRENCE_TYPE);
                        int recurrenceIntervalIdx = cursor.getColumnIndexOrThrow(COLUMN_RECURRENCE_INTERVAL);

                        bill.setId(cursor.getLong(idIdx));
                        bill.setName(cursor.getString(nameIdx));
                        bill.setAmount(cursor.getDouble(amountIdx));
                        bill.setDueDate(cursor.getString(dueDateIdx));
                        bill.setPaid(cursor.getInt(isPaidIdx) == 1);
                        bill.setRecurrenceType(cursor.getInt(recurrenceTypeIdx));
                        bill.setRecurrenceInterval(cursor.getInt(recurrenceIntervalIdx));

                        upcomingBills.add(bill);
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
        
        db.close();
        return upcomingBills;
    }
}
