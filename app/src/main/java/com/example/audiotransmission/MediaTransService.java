package com.example.audiotransmission;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaTransService extends Service {
    private Notification notification;
    private Notification.Builder nb;
    private NotificationManager nm;
    private Context mContext;
    private static final String NITIFICATION_CHANEL_ID = "TransService";
    public static final int NOTIFICATION_ID = 100;
    public ListenerThread listenerThread;
    private static final int PORT = 7879;
    private static final int DEVICE_CONNECTING = 1;
    private static final int DEVICE_CONNECTED = 2;
    private static final int MSG_RECEIVED = 3;
    private static final int MSG_SENDED = 4;
    private static final int MSG_RECEIVED_ERROR = 5;
    private static final int MSG_SENDED_ERROR = 6;
    private AudioRecorder audioRecorder;
    private AudioManager audioManager;
    public ExecutorService mExecutor;
    private boolean isConnected;
    private int voluimeMax;
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
        nb =  new Notification.Builder(this);
        nb.setContentTitle(getString(R.string.app_name))
                .setContentText("StreamTransmitting")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(),R.mipmap.ic_launcher));
        if(Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(NITIFICATION_CHANEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
            nb.setChannelId(NITIFICATION_CHANEL_ID);
        }
            notification = nb.build();
            mExecutor = Executors.newCachedThreadPool();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(100,notification);
        startService(new Intent(this,NoticeCancelService.class));
        if(listenerThread == null) {
            listenerThread = new ListenerThread(PORT, mHandler);
            mExecutor.execute(listenerThread);
        }
        audioManager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
        voluimeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.i("设备最大音量",voluimeMax+"");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(CommonReceiver,intentFilter);
        Log.i("前台服务","开启中。。。");
        if(intent != null){
                String content = intent.getStringExtra("cmd");
                if(content != null) {
                    Log.i("字符串数据",content);
                    ConnectThread connectThread = new ConnectThread(listenerThread.getSocket(), mHandler);
                    connectThread.setHasPermission(true);
                    connectThread.setMsg(content);
                    mExecutor.execute(connectThread);
                }
        }
        return START_STICKY;
    }
    public void start(Context ctx, Intent intent){
        ctx.startService(intent);
    }
    //获取热点路由IP
    public String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf
                            .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            Log.d("Main", inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Main", ex.toString());
        }
        return null;
    }
    private final BroadcastReceiver CommonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
                int streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                Log.i("设备最大音量",maxVolume+", "+"设备当前音量:"+streamVolume);
                Log.i("当前设备音量百分比",(float) (1/2.0)+"");
                ConnectThread connectThread = new ConnectThread(listenerThread.getSocket(), mHandler);
                connectThread.setHasPermission(true);
                connectThread.setMsg(((float) streamVolume/maxVolume)+"");
                mExecutor.execute(connectThread);
            }
        }
    };
    public Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case DEVICE_CONNECTING:
                    //Toast.makeText(MainActivity.this, "接收到客户端的连接 ："+(String) msg.obj, Toast.LENGTH_SHORT).show();
                    mExecutor.execute(new ConnectThread(listenerThread.getSocket(),mHandler));
                    break;
                case DEVICE_CONNECTED:
                    if (audioRecorder == null) {
                        audioRecorder = AudioRecorder.getInstance();
                        audioRecorder.createDefaultAudio();
                    }
                    if (audioRecorder.getStatus() != AudioRecorder.Status.STATUS_START) {
                        audioRecorder.startRecord();
                        mExecutor.execute(() -> {
                            byte[] buff = new byte[audioRecorder.bufferSizeInBytes];
                            while (true) {
                                int length = audioRecorder.audioRecord.read(buff, 0, audioRecorder.bufferSizeInBytes);
                               // Log.i("音频录制长度", buff.length + "");
                                if (length != -1) {
                                    Socket socket =listenerThread.getSocket();
                                    ConnectThread connectThread = new ConnectThread(socket, mHandler);
                                    connectThread.setHasPermission(true);
                                    connectThread.setStream(buff);
                                    mExecutor.execute(connectThread);
                                } else {
                                    break;
                                }
                            }
                        });
                    }
                    if(!isConnected) {
                        Toast.makeText(MediaTransService.this, "连接成功", Toast.LENGTH_SHORT).show();
                        isConnected = true;
                    }
                    break;
                case MSG_SENDED:
                    //  Toast.makeText(MainActivity.this, "发送数据 :"+(String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_RECEIVED:
                    break;
                case MSG_SENDED_ERROR:
                    // Toast.makeText(MainActivity.this, "发送数据失败", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_RECEIVED_ERROR:
                    // Toast.makeText(MainActivity.this, "接收数据失败", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(CommonReceiver != null)
            unregisterReceiver(CommonReceiver);
    }
}
