package com.example.rappeldesfactures;


import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Bill implements Serializable {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
    public static final int RECURRENCE_NONE = 0;
    public static final int RECURRENCE_DAILY = 1;
    public static final int RECURRENCE_WEEKLY = 2;
    public static final int RECURRENCE_MONTHLY = 3;
    public static final int RECURRENCE_YEARLY = 4;
    
    private long id;
    private String name;
    private double amount;
    private String dueDate;
    private boolean isPaid;
    private int recurrenceType;
    private int recurrenceInterval;

    // Default constructor
    public Bill() {
    }

    // Constructor with all fields except ID (for new bills)
    public Bill(String name, double amount, String dueDate, boolean isPaid) {
        this.name = name;
        this.amount = amount;
        this.dueDate = dueDate;
        this.isPaid = isPaid;
    }

    // Constructor with all fields including ID (for existing bills)
    public Bill(long id, String name, double amount, String dueDate, boolean isPaid) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.dueDate = dueDate;
        this.isPaid = isPaid;
        this.recurrenceType = RECURRENCE_NONE;
        this.recurrenceInterval = 1;
    }
    
    // Constructor with all fields including recurrence
    public Bill(long id, String name, double amount, String dueDate, boolean isPaid, 
                int recurrenceType, int recurrenceInterval) {
        this.id = id;
        this.name = name;
        this.amount = amount;
        this.dueDate = dueDate;
        this.isPaid = isPaid;
        this.recurrenceType = recurrenceType;
        this.recurrenceInterval = recurrenceInterval;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }
    
    public int getRecurrenceType() {
        return recurrenceType;
    }
    
    public void setRecurrenceType(int recurrenceType) {
        this.recurrenceType = recurrenceType;
    }
    
    public int getRecurrenceInterval() {
        return recurrenceInterval;
    }
    
    public void setRecurrenceInterval(int recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }
    
    public boolean isRecurring() {
        return recurrenceType != RECURRENCE_NONE;
    }
    
    public String getNextDueDate() {
        if (recurrenceType == RECURRENCE_NONE) {
            return dueDate;
        }
        
        try {
            Date date = DATE_FORMAT.parse(dueDate);
            if (date == null) return dueDate;
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            
            switch (recurrenceType) {
                case RECURRENCE_DAILY:
                    cal.add(Calendar.DAY_OF_MONTH, recurrenceInterval);
                    break;
                case RECURRENCE_WEEKLY:
                    cal.add(Calendar.WEEK_OF_YEAR, recurrenceInterval);
                    break;
                case RECURRENCE_MONTHLY:
                    cal.add(Calendar.MONTH, recurrenceInterval);
                    break;
                case RECURRENCE_YEARLY:
                    cal.add(Calendar.YEAR, recurrenceInterval);
                    break;
            }
            
            return DATE_FORMAT.format(cal.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return dueDate;
        }
    }

    // Utility methods
    public Date getDueDateAsDate() {
        try {
            if (dueDate == null || dueDate.isEmpty()) {
                return new Date();
            }
            return DATE_FORMAT.parse(dueDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }
    
    public String getFormattedAmount() {
        return String.format(Locale.getDefault(), "%.2f", amount);
    }
    
    public boolean isDueSoon(int daysThreshold) {
        try {
            if (dueDate == null || dueDate.isEmpty()) {
                return false;
            }
            
            Date today = new Date();
            Date due = DATE_FORMAT.parse(dueDate);
            
            long diff = due.getTime() - today.getTime();
            long daysDiff = diff / (24 * 60 * 60 * 1000);
            
            return daysDiff <= daysThreshold && daysDiff >= 0;
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }
}
