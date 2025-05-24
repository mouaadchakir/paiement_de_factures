package com.example.rappeldesfactures;



import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

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
            viewHolder.cardBackground = convertView.findViewById(R.id.bill_background);
            viewHolder.billCard = convertView.findViewById(R.id.bill_card);
            
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        // Get the current bill
        Bill bill = bills.get(position);

        // Set the bill data
        viewHolder.nameTextView.setText(bill.getName() != null ? bill.getName() : "");
        viewHolder.amountTextView.setText(String.format(Locale.getDefault(), "%.2f DH", bill.getAmount()));

        // Format and set the due date
        try {
            String dueDateStr = bill.getDueDate();
            if (dueDateStr == null || dueDateStr.isEmpty()) {
                viewHolder.dueDateTextView.setText("");
            } else {
                Date dueDate = DATE_FORMAT.parse(dueDateStr);
                if (dueDate != null) {
                    viewHolder.dueDateTextView.setText(DISPLAY_DATE_FORMAT.format(dueDate));
                }
            }
            
            // Set bill status and styling
            long daysUntilDue = getDaysUntilDue(bill.getDueDate());
            int statusBgColor;
            int statusTextColor = context.getResources().getColor(R.color.black);
            int cardBgColor = context.getResources().getColor(R.color.white);
            String statusText;
            
            if (bill.isPaid()) {
                // Paid bill
                statusText = context.getString(R.string.status_paid);
                statusBgColor = context.getResources().getColor(R.color.bill_paid);
                cardBgColor = context.getResources().getColor(R.color.bill_paid);
            } else if (daysUntilDue < 0) {
                // Overdue bill
                statusText = context.getString(R.string.status_overdue);
                statusBgColor = context.getResources().getColor(R.color.bill_overdue);
                statusTextColor = context.getResources().getColor(R.color.red);
            } else if (daysUntilDue == 0) {
                // Due today
                statusText = context.getString(R.string.status_due_today);
                statusBgColor = context.getResources().getColor(R.color.bill_due_soon);
                statusTextColor = context.getResources().getColor(R.color.orange);
            } else if (daysUntilDue <= 3) {
                // Due soon
                statusText = context.getString(R.string.status_due_soon);
                statusBgColor = context.getResources().getColor(R.color.bill_due_soon);
                statusTextColor = context.getResources().getColor(R.color.orange);
            } else {
                // Upcoming
                statusText = context.getString(R.string.days_remaining, daysUntilDue);
                statusBgColor = context.getResources().getColor(R.color.bill_normal);
                statusTextColor = context.getResources().getColor(R.color.primary);
            }
            
            // Apply the styles
            viewHolder.statusTextView.setText(statusText);
            viewHolder.statusTextView.setTextColor(statusTextColor);
            viewHolder.statusTextView.getBackground().setColorFilter(
                    statusBgColor, PorterDuff.Mode.SRC_ATOP);
            viewHolder.cardBackground.setBackgroundColor(cardBgColor);
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
        LinearLayout cardBackground;
        CardView billCard;
    }
}
