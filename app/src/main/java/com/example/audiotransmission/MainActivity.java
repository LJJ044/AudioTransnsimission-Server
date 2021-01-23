package com.example.audiotransmission;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
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
import android.os.ResultReceiver;
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
import java.lang.reflect.Field;
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
    String SSID = "haiyou123";
    String PASS = "12341234";
    private static final int PORT = 7879;
    public ExecutorService mExecutor;
    private ListenerThread listenerThread;
    private Button btn_receveA;
    private RadioButton rb_exit;
    private boolean isConnected;
    private boolean hasPermission;
    private Intent mIntent;
    private boolean isServiceRunning;
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getServiceRunning();
        if(!isServiceRunning){
            Log.i("MediaTransService","未在运行");
            mIntent = new Intent(this,MediaTransService.class);
            MediaTransService mediaTransService = new MediaTransService();
            mediaTransService.start(this,mIntent);
        }else {
            Log.i("MediaTransService","运行中...");
        }
        hasPermission = requestPermissions();
        am = new WifiAdmin(this);
        rb_exit = (RadioButton) findViewById(R.id.btn_back);
        rb_exit.setButtonDrawable(new StateListDrawable());
        btn_receveA = (Button) this.findViewById(R.id.btn_receveA);
        mExecutor = Executors.newCachedThreadPool();
    }
    private boolean getServiceRunning(){
        ActivityManager manager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);

        for(ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)){
            if(!serviceInfo.service.getClassName().equals(getPackageName()+".MediaTransService")){
                isServiceRunning = false;
            }else {
                isServiceRunning = true;
            }
        }
        return isServiceRunning;
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
    public void output_car(View v){
        Intent intent = new Intent(this,MediaTransService.class);
        intent.putExtra("cmd","1");
        startService(intent);
    }
    public void output_pad(View v) {
        Intent intent = new Intent(this,MediaTransService.class);
        intent.putExtra("cmd","0");
        startService(intent);
    }
    public void create_wifi_hotspot(View v){
        setWifiHotSpotEnabled(true);    //创建热点
    }

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
//                ConnectivityManager cm =(ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
//                cm.startTethering(ConnectivityManager.TETHERING_WIFI,
//                        true, new OnStartTetheringCallback());
                setWifiApEnabledForAndroid_O(true);
               /* Field wField = am.mWifiManager.getClass().getDeclaredField("mService");
                wField.setAccessible(true);
                Object iWifiMgr = wField.get(am.mWifiManager);
                Class iWifiMgrClass = Class.forName(iWifiMgr.getClass().getName());
                Method configMethod = iWifiMgrClass.getMethod("setWifiApConfiguration", WifiConfiguration.class);
                configMethod.invoke(iWifiMgr,config);*/
                //boolean isConfigured = (Boolean) configMethod.invoke(am.mWifiManager, config);

                //Method method = am.mWifiManager.getClass().getMethod("startSoftAp", WifiConfiguration.class);
                //返回热点打开状态
                //return (Boolean) method.invoke(am.mWifiManager,enabled);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
        return false;
    }
    public void setWifiApEnabledForAndroid_O(boolean state){
        ConnectivityManager connManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        Field iConnMgrField;
        try{
            iConnMgrField = connManager.getClass().getDeclaredField("mService");
            iConnMgrField.setAccessible(true);
            Object iConnMgr = iConnMgrField.get(connManager);
            Class<?> iConnMgrClass = Class.forName(iConnMgr.getClass().getName());
            //打开热点
            if(state) {
                Method startTethering = iConnMgrClass.getMethod("startTethering", int.class, ResultReceiver.class, boolean.class);
                startTethering.invoke(iConnMgr, 0, null, true);
            //关闭热点
            }else {
                Method startTethering = iConnMgrClass.getMethod("stopTethering",int.class);
                startTethering.invoke(iConnMgr,0);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * 打开移动网络
     * @param enabled 是否打开
     */
    public void setMobileDataState(boolean enabled) {
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setDataEnabled = telephonyManager.getClass().getDeclaredMethod("setDataEnabled",boolean.class);
            setDataEnabled.setAccessible(true);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(telephonyManager, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMobileData(boolean enabled){
            ConnectivityManager cm =(ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        try {
            Class cMr = Class.forName(cm.getClass().getName());
            Field field = cMr.getDeclaredField("mService");
            field.setAccessible(true);
            Object ICMgr = field.get(cm);
            Class ICMgrClass = Class.forName(ICMgr.getClass().getName());
            Method setDataEnabled = ICMgrClass.getDeclaredMethod("setMobileDataEnabled",Boolean.TYPE);
            setDataEnabled.setAccessible(true);
            setDataEnabled.invoke(ICMgr,enabled);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void SendToServer(View v){
        DhcpInfo dhcpInfo = am.mWifiManager.getDhcpInfo();
        Toast.makeText(this, "wifi已连接到热点SSID: " + intToIp(dhcpInfo.serverAddress), Toast.LENGTH_SHORT).show();
        mExecutor.execute(() -> {
            Socket socket = null;
            try {
                socket = new Socket(intToIp(dhcpInfo.serverAddress), PORT);
                Log.d("SSID的IP: ",intToIp(dhcpInfo.serverAddress));
                if (socket != null) {
                    ConnectThread connectThread = new ConnectThread(socket,null);
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
                 ConnectThread connectThread = new ConnectThread(socket, null);
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
    public void exit(View v){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
      /*  if(mService != null) {
            Log.i("前台服务","销毁中。。。");
            stopService(mIntent);
        }*/
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