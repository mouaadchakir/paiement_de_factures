package com.example.rappeldesfactures;


import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Bill implements Serializable {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    
    private long id;
    private String name;
    private double amount;
    private String dueDate;
    private boolean isPaid;

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
