package com.example.audiotransmission;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static com.example.audiotransmission.AudioReader.audioReader;

public class ConnectThread implements Runnable{
    private Socket socket;
    private Handler mHandler;
    private InputStream is;
    private OutputStream os;
    private PrintWriter pw;
    private Gson gson;
    private byte[] stream;
    private String msg;
    private boolean hasPermission;
    private static final int DEVICE_CONNECTED = 2;
    private static final int MSG_RECEIVED = 3;
    private static final int MSG_SENDED = 4;
    private static final int MSG_RECEIVED_ERROR = 5;
    private static final int MSG_SENDED_ERROR = 6;
    private Context mContext;
    public byte[] getStream() {
        return stream;
    }

    public void setStream(byte[] stream) {
        this.stream = stream;
    }
    public boolean isHasPermission() {
        return hasPermission;
    }

    public void setHasPermission(boolean hasPermission) {
        this.hasPermission = hasPermission;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public ConnectThread(Socket socket, Handler mHandler) {
        this.socket = socket;
        this.mHandler = mHandler;
        gson = new Gson();
    }
    @Override
    public void run() {
        if(socket == null) {
            Log.i("sock异常","socket为空");
            return;
        }

        mHandler.sendEmptyMessage(DEVICE_CONNECTED);

        try{
            os =socket.getOutputStream();
            pw = new PrintWriter(os);
            // is = socket.getInputStream();
            if(os != null && hasPermission) {
                if (stream != null)
                    sendAudioBytes(stream);
                if(!TextUtils.isEmpty(msg))
                 sendData(msg);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void sendData(String str){
        TransmissionBean transmissionBean = new TransmissionBean();
        transmissionBean.contentType = 1;
        transmissionBean.content = str;
        pw.write(gson.toJson(transmissionBean,TransmissionBean.class));
        pw.flush();

    }
    private void obtainAudio(){
        audioReader.readAudio();
        byte[] buffer = new byte[audioReader.bufferSizeInBytes];
        int length;
        while (true){
            try {
                if((length = is.read(buffer)) <0)
                    break;
                audioReader.audioTrack.write(buffer,0,length);
                Log.i("读取音频长度",buffer.length+"");
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(MSG_RECEIVED_ERROR);
            }

        }
    }
    private void obtainData(){
        byte[] buffer = new byte[1024];
        int length;
        while (true){
            try {
                if((length = is.read(buffer)) <0)
                    break;
                byte[] data= new byte[length];
                System.arraycopy(buffer, 0, data, 0, length);
                Message msg = Message.obtain();
                msg.what = MSG_RECEIVED;
                msg.obj = new String(data);
                mHandler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.sendEmptyMessage(MSG_RECEIVED_ERROR);
            }

        }

    }
    private void sendAudioBytes(byte[] audioData){
        TransmissionBean transmissionBean = new TransmissionBean();
        transmissionBean.contentType = 0;
        transmissionBean.content = Base64.encodeToString(audioData,Base64.DEFAULT);
        pw.write(gson.toJson(transmissionBean,TransmissionBean.class));
        pw.flush();
    }
}
