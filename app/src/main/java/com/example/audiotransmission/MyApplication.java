package com.example.audiotransmission;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.widget.Toast;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //Thread.setDefaultUncaughtExceptionHandler(handler);
    }
    private Thread.UncaughtExceptionHandler handler = (t, e) -> {
        handleException(e);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        restartService(); //发生崩溃异常时,重启服务
    };
    private void handleException(Throwable e){
        if(e == null)
            return;
        //使用Toast来显示异常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(getApplicationContext(),"程序出现异常，即将重启", Toast.LENGTH_LONG).show();
                Looper.loop();
            }
        }.start();
    }
    private void restartService() {
        Intent intent = new Intent(this, MediaTransService.class);
        PendingIntent restartIntent = PendingIntent.getService(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //退出程序
        AlarmManager mgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000,
                restartIntent); // 1秒钟后重启应用
        //结束进程之前可以把你程序的注销或者退出代码放在这段代码之前
        android.os.Process.killProcess(android.os.Process.myPid());

    }
}
