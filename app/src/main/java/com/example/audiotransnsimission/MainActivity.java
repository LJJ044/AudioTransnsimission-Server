package com.example.audiotransnsimission;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import java.lang.reflect.Method;


public class MainActivity extends AppCompatActivity {
    private WifiAdmin am;
    private String Wifi_Scan_Result = "wifi_result_obtained";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        am = new WifiAdmin(this);
        //registerReceiver(WifiReceiver,new IntentFilter(Wifi_Scan_Result));
    }
    public void connect_hotspot(View v){
        am.openWifi();
        am.startScan();
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
        config.SSID = "Device";
        //配置热点的密码
        config.preSharedKey = "12121212";
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
            //am.mWifiManager.setWifiApConfiguration(config);
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);

        }
        return false;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(WifiReceiver != null)
//            unregisterReceiver(WifiReceiver);
    }


}