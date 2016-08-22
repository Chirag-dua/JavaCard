package com.example.rishabh.bluetoothcard;

import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

import android.util.Log;

public class TCPClient extends Thread {

    private final int portNo = 4444;
    private String serverIp = null;
    private final String tag = "SIMPLE_CLIENT";
    private boolean running = false;
    private int MAX_SIZE = 1024;
    public TCPClient(String serverIp) {
        this.serverIp = serverIp;
    }

    public void run() {
        super.run();
        running = true;
        try {
            Log.i(tag, "Connecting to server.....");
            //commSocket.connect(address);
            InetAddress serverAddress = InetAddress.getByName(serverIp);
            Socket commSocket = new Socket(serverAddress, portNo);
            Log.i(tag, "Connected to server: " + commSocket.getRemoteSocketAddress());
            Log.i(tag, "Ab hogi guftagu");
            int bytes;
            byte[] buffer = new byte[MAX_SIZE];
            try {
                InputStream in = commSocket.getInputStream();
                bytes = in.read(buffer);
                Log.i(tag, "Data received: " + buffer[0] + " " + buffer[1]);
            }
            catch(IOException ex) {
                Log.e(tag, ex.getMessage());
            }
            finally {
                commSocket.close();
                Log.i(tag, "Connection closed");
            }
        }
        catch(IOException ex) {
            Log.e(tag, ex.getMessage());
            ex.printStackTrace();
        }
    }
}

