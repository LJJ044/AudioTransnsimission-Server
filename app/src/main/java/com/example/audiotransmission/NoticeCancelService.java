package com.example.audiotransmission;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.RequiresApi;

public class NoticeCancelService extends Service {
    private static final String NITIFICATION_CHANEL_ID = "notification_cancel";
    public NoticeCancelService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = (NotificationManager) this.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder nb = new Notification.Builder(this);
        if(Build.VERSION.SDK_INT >= 26){
            NotificationChannel channel = new NotificationChannel(NITIFICATION_CHANEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
            nb.setChannelId(NITIFICATION_CHANEL_ID);
        }
        startForeground(MediaTransService.NOTIFICATION_ID,nb.build());
        new Thread(() -> {
            SystemClock.sleep(1000);
            stopForeground(true);
            nm.cancel(MediaTransService.NOTIFICATION_ID);
            stopSelf();
        }).start();
        return super.onStartCommand(intent, flags, startId);
    }
}