package com.example.rappeldesfactures;



import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationHelper {
    public static final String CHANNEL_ID = "bill_reminder_channel";
    private static final long DAY_IN_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final int NOTIFICATION_REQUEST_CODE = 100;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // Create notification channel for Android 8.0 and higher
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(context.getString(R.string.notification_channel_description));
            
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Schedule a bill reminder notification
    public static void scheduleBillReminder(Context context, int billId, String billName, double amount, String dueDate) {
        try {
            if (context == null || dueDate == null || dueDate.isEmpty()) {
                return; // Skip scheduling if we have null values
            }
            
            // Get due date and calculate notification time (1 day before due date)
            Date dueDateObj = DATE_FORMAT.parse(dueDate);
            
            Calendar notificationTime = Calendar.getInstance();
            notificationTime.setTime(dueDateObj);
            notificationTime.add(Calendar.DAY_OF_MONTH, -1); // Notify 1 day before due date
            notificationTime.set(Calendar.HOUR_OF_DAY, 10); // Notify at 10:00 (22:00 AM)
            notificationTime.set(Calendar.MINUTE, 00);
            notificationTime.set(Calendar.SECOND, 16);
            
            // Check if notification time has already passed, in which case notify tomorrow
            Calendar now = Calendar.getInstance();
            if (notificationTime.before(now)) {
                // If due date is today, notify in 1 hour //
                if (dueDateObj.getTime() - now.getTimeInMillis() < DAY_IN_MILLIS) {
                    notificationTime.setTimeInMillis(now.getTimeInMillis() + TimeUnit.HOURS.toMillis(1));
                } else {
                    // Otherwise, skip this notification
                    return;
                }
            }
            
            // Create intent for the notification
            Intent notificationIntent = new Intent(context, NotificationReceiver.class);
            notificationIntent.putExtra("bill_id", billId);
            notificationIntent.putExtra("bill_name", billName);
            notificationIntent.putExtra("bill_amount", amount);
            notificationIntent.putExtra("bill_due_date", dueDate);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    billId, // Use bill ID as request code to ensure uniqueness
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Schedule the notification
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            
            // Utiliser une notification immédiate pour les tests
            if (System.currentTimeMillis() + 60000 > notificationTime.getTimeInMillis()) {
                // Si l'heure de notification est dans moins d'une minute, programmer une notification immédiate
                Toast.makeText(context, "Notification immédiate programmée...", Toast.LENGTH_SHORT).show();
                context.sendBroadcast(notificationIntent);
            } else {
                // Sinon, utiliser des alarmes moins précises qui sont plus susceptibles de fonctionner
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
                    // Pour Android 12+, utiliser une alarme inexacte qui est moins susceptible d'être limitée
                    if (alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(context, "Programmation d'alarme exacte autorisée", Toast.LENGTH_SHORT).show();
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime.getTimeInMillis(), pendingIntent);
                    } else {
                        Toast.makeText(context, "Programmation d'alarme exacte NON autorisée, utilisation d'alarme inexacte", Toast.LENGTH_LONG).show();
                        alarmManager.set(AlarmManager.RTC_WAKEUP, notificationTime.getTimeInMillis(), pendingIntent);
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6-11
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notificationTime.getTimeInMillis(), pendingIntent);
                } else { // Versions plus anciennes
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime.getTimeInMillis(), pendingIntent);
                }
            }
            
            // Afficher un message de débogage pour confirmer la programmation
            Toast.makeText(context, "Notification programmée pour " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date(notificationTime.getTimeInMillis())), Toast.LENGTH_LONG).show();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    
    // Cancel a scheduled bill reminder
    public static void cancelBillReminder(Context context, int billId) {
        if (context == null) {
            return; // Skip if context is null
        }
        
        try {
            Intent notificationIntent = new Intent(context, NotificationReceiver.class);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    billId,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Update an existing bill reminder (cancel and reschedule)
    public static void updateBillReminder(Context context, int billId, String billName, double amount, String dueDate) {
        if (context == null) {
            return; // Skip if context is null
        }
        
        try {
            cancelBillReminder(context, billId);
            scheduleBillReminder(context, billId, billName, amount, dueDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
