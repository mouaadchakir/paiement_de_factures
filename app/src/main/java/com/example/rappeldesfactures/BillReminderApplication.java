package com.example.rappeldesfactures;



import android.app.Application;

public class BillReminderApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize notification channel for Android 8.0+
        NotificationHelper.createNotificationChannel(this);
    }
}
