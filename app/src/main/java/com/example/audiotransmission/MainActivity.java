package com.example.audiotransmission;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    private WifiAdmin am;
    private static String Wifi_Scan_Result = "wifi_result_obtained";
    private WifiManager.LocalOnlyHotspotReservation mReservation;
    String SSID = "OnePlus 7";
    String PASS = "12345678";
    private static final int PORT = 7879;
    private static final int DEVFICE_CONNECTING = 1;
    private static final int DEVFICE_CONNECTED = 2;
    private static final int MSG_RECEIVED = 3;
    private static final int MSG_SENDED = 4;
    private static final int MSG_RECEIVED_ERROR = 5;
    private static final int MSG_SENDED_ERROR = 6;
    private ExecutorService mExecutor;
    private ListenerThread listenerThread;
    private AudioReader audioReader;
    private  AudioManager audioManager;
    private AudioRecorder audioRecorder;
    private WifiApConnectReceiver wifiApConnectReceiver = null;
    private int voluimeMax;
    private Button btn_receveA;
    private RadioButton rb_exit;
    private Socket socket;
    private boolean hasPermission;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hasPermission = requestPermissions();
        am = new WifiAdmin(this);
        rb_exit = (RadioButton) findViewById(R.id.btn_back);
        rb_exit.setButtonDrawable(new StateListDrawable());
        btn_receveA = (Button) this.findViewById(R.id.btn_receveA);
        mExecutor = Executors.newCachedThreadPool();
        audioManager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
        voluimeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.i("设备最大音量",voluimeMax+"");
        //IntentFilter intentFilter = new IntentFilter();
        //intentFilter.addAction(Wifi_Scan_Result);
        //registerReceiver(WifiReceiver,new IntentFilter(Wifi_Scan_Result));
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean requestPermissions(){
        boolean hasPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED||
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        if(hasPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION
                    , Manifest.permission.RECORD_AUDIO}, 0);
        }else {
            return true;
        }
        return false;
    }
    public void connect_hotspot(View v){
        am.openWifi();
        am.startScan();
        if(!am.isConnected)
        am.addNetwork(am.CreateWifiInfo("OnePlus 7","12345678",3));
    }
    public void create_wifi_hotspot(View v){
        setWifiHotSpotEnabled(true);
    }
    private final BroadcastReceiver WifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
           if(!am.isConnected)
                am.addNetwork(am.CreateWifiInfo("OnePlus 7","12345678",3));
        }
    };
    // wifi热点开关
    public boolean setWifiHotSpotEnabled(boolean enabled) {
        //热点的配置类
        WifiConfiguration config = new WifiConfiguration();
        //配置热点的名称(可以在名字后面加点随机数什么的)
        config.SSID = SSID;
        //配置热点的密码
        config.preSharedKey = PASS;
        config.hiddenSSID = true;
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        //通过反射调用设置热点
        if(Build.VERSION.SDK_INT < 26) {
            if (am.mWifiManager.isWifiEnabled()) { // disable WiFi in any case
                //wifi和热点不能同时打开，所以打开热点的时候需要关闭wifi
                am.mWifiManager.setWifiEnabled(false);
            }
            try {
                Method method = am.mWifiManager.getClass().getMethod(
                        "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
                //返回热点打开状态
                return (Boolean) method.invoke(am.mWifiManager, config, enabled);
            } catch (Exception e) {
                return false;
            }
        }else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                },0);
                return false;
            }
            am.mWifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                @Override
                public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                    mReservation = reservation;
                    String SSID = reservation.getWifiConfiguration().SSID;
                    String PASS = reservation.getWifiConfiguration().preSharedKey;
                  //  callbak.onConnected("", sid, pwd);
                    Log.d("热点","SSID:"+SSID+" PASS:" +PASS);
                }

                @Override
                public void onStopped() {
                    mReservation = null;
                }

                @Override
                public void onFailed(int reason) {
                    Log.d("热点","wifi ap is failed to open");
                  //  callbak.onConnected("wifi ap is failed to open", null, null);
                }
            }, new Handler());
//            am.mWifiManager.setWifiApConfiguration(config);
//            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);

        }
        return false;
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onStartAudioReceive(View v) {
        if(hasPermission) {
            btn_receveA.setEnabled(false);
            listenerThread = new ListenerThread(PORT, mHandler);
            mExecutor.execute(listenerThread);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String routeIp = getWifiApIpAddress();
            Toast.makeText(this, "本地路由IP: " + routeIp, Toast.LENGTH_SHORT).show();
            if (routeIp == null)
                routeIp = "192.168.43.1";
            if (audioRecorder == null) {
                audioRecorder = AudioRecorder.getInstance();
                audioRecorder.createDefaultAudio();
            }
            if (audioRecorder.getStatus() != AudioRecorder.Status.STATUS_START) {
                audioRecorder.startRecord();
                String finalRouteIp = routeIp;

                mExecutor.execute(() -> {
                    byte[] buff = new byte[audioRecorder.bufferSizeInBytes];
                    while (true) {
                        int length = audioRecorder.audioRecord.read(buff, 0, audioRecorder.bufferSizeInBytes);
                        Log.i("音频录制长度", buff.length + "");
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
            //String finalRouteIp = routeIp;
            //服务端自己监听自己的socket
/*        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(finalRouteIp,PORT);
                    ConnectThread connectThread = new ConnectThread(socket,mHandler);
                    mExecutor.execute(connectThread);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });*/
        }
    }
    public void SendToServer(View v){
       // wifiApConnectReceiver = new WifiApConnectReceiver();
       // registerReceiver(wifiApConnectReceiver,new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        DhcpInfo dhcpInfo = am.mWifiManager.getDhcpInfo();
        Toast.makeText(this, "wifi已连接到热点SSID: " + intToIp(dhcpInfo.serverAddress), Toast.LENGTH_SHORT).show();
        mExecutor.execute(() -> {
            Socket socket = null;
            try {
                socket = new Socket(intToIp(dhcpInfo.serverAddress), PORT);
                Log.d("SSID的IP: ",intToIp(dhcpInfo.serverAddress));
                if (socket != null) {
                    ConnectThread connectThread = new ConnectThread(socket, mHandler);
                    mExecutor.execute(connectThread);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    public void SendToClient(View v){
        String ip = getWifiApIpAddress();
        Toast.makeText(this, "本地路由地址："+ip, Toast.LENGTH_SHORT).show();
         mExecutor.execute(() -> {
             Socket socket = listenerThread.getSocket();
             if (socket != null) {
                 ConnectThread connectThread = new ConnectThread(socket, mHandler);
                 connectThread.setHasPermission(true);
                 connectThread.setMsg("你好，客户端");
                 mExecutor.execute(connectThread);

             }
         });
    }

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

    public class WifiApConnectReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                    WifiInfo wifiInfo = am.mWifiManager.getConnectionInfo();
                    if(wifiInfo != null) {
                        DhcpInfo dhcpInfo = am.mWifiManager.getDhcpInfo();
                        Toast.makeText(context, "wifi已连接到热点SSID: " + intToIp(dhcpInfo.ipAddress), Toast.LENGTH_SHORT).show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Socket socket = null;
                                try {
                                    socket = new Socket(intToIp(dhcpInfo.ipAddress), PORT);
                                    Log.d("IP OF SSID: ",intToIp(dhcpInfo.ipAddress));
                                    if (socket != null) {
                                        ConnectThread connectThread = new ConnectThread(socket, mHandler);
                                        mExecutor.execute(connectThread);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    }

            }else {
                Toast.makeText(context, "wifi未连接到热点", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private String intToIp(int paramInt)
    {
        return (paramInt & 0xFF) + "." + (0xFF & paramInt >> 8) + "." + (0xFF & paramInt >> 16) + "."
                + (0xFF & paramInt >> 24);
    }
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case DEVFICE_CONNECTING:
                    //Toast.makeText(MainActivity.this, "接收到客户端的连接 ："+(String) msg.obj, Toast.LENGTH_SHORT).show();
                    mExecutor.execute(new ConnectThread(listenerThread.getSocket(),mHandler));
                    break;
                case DEVFICE_CONNECTED:
                    Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    break;
                case MSG_SENDED:
                  //  Toast.makeText(MainActivity.this, "发送数据 :"+(String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case MSG_RECEIVED: //接收到音量（字符串）
                    /*Integer valuePercent = Integer.parseInt((String) msg.obj);
                    int value = valuePercent * voluimeMax;
                    if(value > voluimeMax)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,value ,0);*/
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
    public void exit(View v){
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(WifiReceiver != null)
//            unregisterReceiver(WifiReceiver);
        if(wifiApConnectReceiver != null)
            unregisterReceiver(wifiApConnectReceiver);
    }


}