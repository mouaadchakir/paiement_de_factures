package com.example.rappeldesfactures;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BillNotificationService extends Service {
    private static final long CHECK_INTERVAL = 5 * 60 * 1000; // Vérifier toutes les 5 minutes
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private Handler handler;
    private Runnable checkBillsRunnable;
    private boolean isServiceRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        checkBillsRunnable = new Runnable() {
            @Override
            public void run() {
                checkForBillsDueToday();
                // Programmer la prochaine vérification
                if (isServiceRunning) {
                    handler.postDelayed(this, CHECK_INTERVAL);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceRunning) {
            isServiceRunning = true;
            Toast.makeText(this, "Service de notifications démarré", Toast.LENGTH_SHORT).show();
            
            // Démarrer le premier contrôle immédiatement
            handler.post(checkBillsRunnable);
        }
        
        // Redémarrer le service s'il est tué
        return START_STICKY;
    }

    private void checkForBillsDueToday() {
        try {
            // Obtenir l'heure actuelle
            Calendar now = Calendar.getInstance();
            int currentHour = now.get(Calendar.HOUR_OF_DAY);
            int currentMinute = now.get(Calendar.MINUTE);
            
            // Obtenir les factures à venir depuis la base de données
            BillDatabaseHelper dbHelper = new BillDatabaseHelper(this);
            List<Bill> upcomingBills = dbHelper.getUpcomingBills(2); // Factures des 2 prochains jours
            
            // Obtenir la date de demain
            Calendar tomorrow = Calendar.getInstance();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            String tomorrowDate = DATE_FORMAT.format(tomorrow.getTime());
            
            // Vérifier s'il est l'heure d'envoyer des notifications (vers 17:45)
            boolean isNotificationTime = (currentHour == 10 && currentMinute >= 00 && currentMinute <= 10);
            
            if (isNotificationTime) {
                for (Bill bill : upcomingBills) {
                    if (!bill.isPaid() && bill.getDueDate().equals(tomorrowDate)) {
                        // Envoyer une notification directement
                        Intent notificationIntent = new Intent(this, NotificationReceiver.class);
                        notificationIntent.putExtra("bill_id", (int) bill.getId());
                        notificationIntent.putExtra("bill_name", bill.getName());
                        notificationIntent.putExtra("bill_amount", bill.getAmount());
                        notificationIntent.putExtra("bill_due_date", bill.getDueDate());
                        
                        sendBroadcast(notificationIntent);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        isServiceRunning = false;
        handler.removeCallbacks(checkBillsRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
