package com.example.rappeldesfactures;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationReceiver extends BroadcastReceiver {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    public void onReceive(Context context, Intent intent) {
        // Extract bill details from intent
        int billId = intent.getIntExtra("bill_id", -1);
        String billName = intent.getStringExtra("bill_name");
        double billAmount = intent.getDoubleExtra("bill_amount", 0.0);
        String dueDateStr = intent.getStringExtra("bill_due_date");
        
        // If this is a BOOT_COMPLETED broadcast, reschedule all notifications
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            rescheduleNotifications(context);
            return;
        }
        
        // Verify bill ID is valid
        if (billId == -1) {
            return;
        }
        
        // Create the notification content
        String notificationTitle = context.getString(R.string.notification_title);
        
        String formattedDueDate = dueDateStr != null ? dueDateStr : "";
        try {
            if (dueDateStr != null && !dueDateStr.isEmpty()) {
                Date dueDate = DATE_FORMAT.parse(dueDateStr);
                formattedDueDate = DISPLAY_DATE_FORMAT.format(dueDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        String notificationContent = context.getString(
                R.string.notification_content,
                billName,
                String.format(Locale.getDefault(), "%.2f DH", billAmount),
                formattedDueDate
        );
        
        // Create intent to open the app when notification is tapped
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                billId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notificationTitle)
                .setContentText(notificationContent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationContent))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        
        // Send the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(billId, builder.build());
    }
    
    private void rescheduleNotifications(Context context) {
        try {
            if (context == null) {
                return;
            }
            
            // Get all upcoming bills from database and reschedule their notifications
            BillDatabaseHelper dbHelper = new BillDatabaseHelper(context);
            for (Bill bill : dbHelper.getUpcomingBills(30)) { // Reschedule bills due within the next 30 days
                if (bill != null && !bill.isPaid()) {
                    String billName = bill.getName() != null ? bill.getName() : "";
                    String dueDate = bill.getDueDate() != null ? bill.getDueDate() : "";
                    
                    if (!dueDate.isEmpty()) {
                        NotificationHelper.scheduleBillReminder(
                                context,
                                (int) bill.getId(),
                                billName,
                                bill.getAmount(),
                                dueDate
                        );
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
