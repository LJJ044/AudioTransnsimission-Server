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
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
    String SSID = "Device";
    String PASS = "12345678";
    private static final int PORT = 7879;
    private static final int DEVICE_CONNECTING = 1;
    private static final int DEVICE_CONNECTED = 2;
    private static final int MSG_RECEIVED = 3;
    private static final int MSG_SENDED = 4;
    private static final int MSG_RECEIVED_ERROR = 5;
    private static final int MSG_SENDED_ERROR = 6;
    public ExecutorService mExecutor;
    private ListenerThread listenerThread;
    private AudioRecorder audioRecorder;
    private  AudioManager audioManager;
    private int voluimeMax;
    private Button btn_receveA;
    private RadioButton rb_exit;
    private boolean isConnected;
    private boolean hasPermission;
    private MediaTransService mService;
    private Intent mIntent;
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mService = new MediaTransService();
        mIntent = new Intent(this,MediaTransService.class);
        mService.start(this,mIntent);
        hasPermission = requestPermissions();
        am = new WifiAdmin(this);
        rb_exit = (RadioButton) findViewById(R.id.btn_back);
        rb_exit.setButtonDrawable(new StateListDrawable());
        btn_receveA = (Button) this.findViewById(R.id.btn_receveA);
        mExecutor = Executors.newCachedThreadPool();
        audioManager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
        voluimeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.i("设备最大音量",voluimeMax+"");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION");
        registerReceiver(CommonReceiver,intentFilter);
        if(hasPermission) {
            listenerThread = new ListenerThread(PORT, mHandler);
            mExecutor.execute(listenerThread);
            String routeIp = getWifiApIpAddress();
            Toast.makeText(this, "本地路由IP: " + routeIp, Toast.LENGTH_SHORT).show();
        }
    }
    private boolean requestPermissions(){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
            boolean hasPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED;
            if (hasPermission) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION
                        , Manifest.permission.RECORD_AUDIO}, 0);
            } else {
                return true;
            }

            return false;
    }
    public void connect_hotspot(View v){
        am.openWifi();
        am.startScan();
        if(!am.isConnected)
        am.addNetwork(am.CreateWifiInfo(SSID,PASS,3));
    }
    public void open_mobile_data(View v){
        setMobileDataState(true);
    }
    public void create_wifi_hotspot(View v){
        setWifiHotSpotEnabled(true);    //创建热点
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
            try {
                Method configMethod = am.mWifiManager.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
                boolean isConfigured = (Boolean) configMethod.invoke(am.mWifiManager, config);
                Method method = am.mWifiManager.getClass().getMethod("startSoftAp", WifiConfiguration.class);
                //返回热点打开状态
                return (Boolean) method.invoke(am.mWifiManager,enabled);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
        return false;
    }
    /**
     * 打开移动网络
     * @param enabled 是否打开
     */
    public void setMobileDataState(boolean enabled) {
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setDataEnabled = telephonyManager.getClass().getDeclaredMethod("setDataEnabled",boolean.class);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(telephonyManager, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onStartAudioSend(View v) {

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
                                //Log.i("音频录制长度", buff.length + "");
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
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
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
    public void exit(View v){
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mService != null) {
            Log.i("前台服务","销毁中。。。");
            stopService(mIntent);
        }
        if(CommonReceiver != null)
            unregisterReceiver(CommonReceiver);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            Intent intent=new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        return false;
    }
}