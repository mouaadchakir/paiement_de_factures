package com.example.rappeldesfactures;



import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BillAdapter extends ArrayAdapter<Bill> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    
    private final Context context;
    private final List<Bill> bills;

    public BillAdapter(Context context, List<Bill> bills) {
        super(context, R.layout.bill_item, bills);
        this.context = context;
        this.bills = bills;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.bill_item, parent, false);
            
            viewHolder = new ViewHolder();
            viewHolder.nameTextView = convertView.findViewById(R.id.text_bill_name);
            viewHolder.amountTextView = convertView.findViewById(R.id.text_bill_amount);
            viewHolder.dueDateTextView = convertView.findViewById(R.id.text_bill_due_date);
            viewHolder.statusTextView = convertView.findViewById(R.id.text_bill_status);
            
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Get the current bill
        Bill bill = bills.get(position);

        // Set the bill data
        viewHolder.nameTextView.setText(bill.getName() != null ? bill.getName() : "");
        viewHolder.amountTextView.setText(String.format(Locale.getDefault(), "%.2f €", bill.getAmount()));

        // Format and set the due date
        try {
            String dueDateStr = bill.getDueDate();
            if (dueDateStr == null || dueDateStr.isEmpty()) {
                viewHolder.dueDateTextView.setText("");
            } else {
                Date dueDate = DATE_FORMAT.parse(dueDateStr);
                viewHolder.dueDateTextView.setText(DISPLAY_DATE_FORMAT.format(dueDate));
            }
            
            // Set days remaining text and color
            long daysUntilDue = getDaysUntilDue(bill.getDueDate());
            if (bill.isPaid()) {
                viewHolder.statusTextView.setText(R.string.paid);
                viewHolder.statusTextView.setTextColor(Color.GREEN);
            } else if (daysUntilDue < 0) {
                viewHolder.statusTextView.setText(R.string.overdue);
                viewHolder.statusTextView.setTextColor(Color.RED);
            } else if (daysUntilDue == 0) {
                viewHolder.statusTextView.setText(R.string.due_today);
                
                viewHolder.statusTextView.setTextColor(Color.parseColor("#FF9800")); // Orange
            } else if (daysUntilDue <= 3) {
                viewHolder.statusTextView.setText(context.getString(R.string.days_remaining, daysUntilDue));
                viewHolder.statusTextView.setTextColor(Color.parseColor("#FF9800")); // Orange
            } else {
                viewHolder.statusTextView.setText(context.getString(R.string.days_remaining, daysUntilDue));
                viewHolder.statusTextView.setTextColor(Color.parseColor("#2196F3")); // Blue
            }
        } catch (ParseException e) {
            e.printStackTrace();
            viewHolder.dueDateTextView.setText(bill.getDueDate());
            viewHolder.statusTextView.setText("");
        }

        return convertView;
    }

    private long getDaysUntilDue(String dueDate) throws ParseException {
        if (dueDate == null || dueDate.isEmpty()) {
            return 0; // Default to 0 days if no due date
        }
        
        Date today = new Date();
        Date due = DATE_FORMAT.parse(dueDate);
        
        long diffInMillis = due.getTime() - today.getTime();
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }

    private static class ViewHolder {
        TextView nameTextView;
        TextView amountTextView;
        TextView dueDateTextView;
        TextView statusTextView;
    }
}
