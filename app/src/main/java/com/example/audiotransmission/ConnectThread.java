package com.example.audiotransmission;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static com.example.audiotransmission.AudioReader.audioReader;

public class ConnectThread implements Runnable{
    private Socket socket;
    private Handler mHandler;
    private InputStream is;
    private OutputStream os;
    private byte[] stream;
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

    private String msg;
    public ConnectThread(Socket socket, Handler mHandler) {
        this.socket = socket;
        this.mHandler = mHandler;

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
            // is = socket.getInputStream();
            if(os != null && hasPermission) {
                if (stream != null)
                    sendAudioBytes(stream);
                // sendData(msg);
            }
            // if(is != null) {
            //  obtainAudio();
            //obtainData();
            // }
        }catch (IOException e){
            e.printStackTrace();
        }
    }
    private void sendData(String str){
        try {
            os.write(str.getBytes());
            Message msg = Message.obtain();
            msg.what = MSG_SENDED;
            msg.obj = str;
            mHandler.sendMessage(msg);

        } catch (IOException e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(MSG_SENDED_ERROR);
        }
    }
    private void obtainAudio(){
        audioReader.readAudio();
        byte[] buffer = new byte[audioReader.bufferSizeInBytes];
        int length;
        while (true){
            try {
                Log.i("读取音频长度",buffer.length+"");
                if((length = is.read(buffer)) <0)
                    break;
                audioReader.audioTrack.write(buffer,0,length);
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
        try {
            os.write(audioData);
        } catch (IOException e) {
            e.printStackTrace();
            mHandler.sendEmptyMessage(MSG_SENDED_ERROR);
        }
    }
}
