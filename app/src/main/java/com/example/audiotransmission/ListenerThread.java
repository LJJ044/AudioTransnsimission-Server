package com.example.audiotransmission;

import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ListenerThread implements Runnable {
    private Handler mHandlerR;
    private ServerSocket serverSocket;
    private Socket socket;
    private int port;
    private static final int DEVFICE_CONNECTING = 1;
    public ListenerThread(int Port,Handler mHandler) {
        this.serverSocket = serverSocket;
        this.mHandlerR = mHandler;
        this.port = Port;
        try {
            serverSocket = new ServerSocket(Port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if(serverSocket != null)
                    socket = serverSocket.accept();
                Message msg = Message.obtain();
                msg.what = DEVFICE_CONNECTING;
                msg.obj = "我来咯";
                mHandlerR.sendMessage(msg);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
    public Socket getSocket() {
        return socket;
    }

}
