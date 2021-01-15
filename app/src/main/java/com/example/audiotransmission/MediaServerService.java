package com.example.audiotransmission;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class MediaServerService extends Service {
    private Notification notification;
    private NotificationManager nm;
    private Context mContext;
    private static final String NITIFICATION_CHANEL_ID = "\"TransService\"";
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
            nm = (NotificationManager) this.getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(NITIFICATION_CHANEL_ID,getString(R.string.app_name),NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(null,null);
            nm.createNotificationChannel(channel);
            notification = new Notification.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("StreamTransmitting")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setChannelId(NITIFICATION_CHANEL_ID)
                    .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(0,notification);

        return START_STICKY;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void start(Context ctx, Intent intent){
        ctx.startForegroundService(intent);
    }
}
