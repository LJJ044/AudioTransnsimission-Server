package com.example.audiotransmission;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

public class MediaTransReceiver extends BroadcastReceiver {
    private boolean isServiceRunning;
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()){
            case Intent.ACTION_BOOT_COMPLETED: {
                getServiceRunning(context);
                if(!isServiceRunning) {
                    Toast.makeText(context, "服务开启中....", Toast.LENGTH_SHORT).show();
                    context.startForegroundService(new Intent(context, MediaTransService.class));
                }
                break;
            }
            case Intent.ACTION_TIME_TICK: {
                getServiceRunning(context);
                if (!isServiceRunning) {
                    MediaTransService mediaTransService = new MediaTransService();
                    mediaTransService.start(context, new Intent(context, MediaTransService.class));
                }
                break;
            }
        }
    }
    private boolean getServiceRunning(Context ctx){
        ActivityManager manager = (ActivityManager)ctx.getSystemService(Context.ACTIVITY_SERVICE);

        for(ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)){
            if(!serviceInfo.service.getClassName().equals(ctx.getPackageName()+".MediaTransService")){
                isServiceRunning = false;
            }else {
                isServiceRunning = true;
                break;
            }
        }
        return isServiceRunning;
    }
}
