package com.example.rishabh.bluetoothreaderasymm;

import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;

import android.util.Log;

public class TCPClient {

    private final int portNo = 4446;
    private String SERVERIP;
    private final String tag = "SIMPLE_CLIENT";
    private boolean running = false;
    private Socket commSocket=null;

    TCPClient(String address) {
        SERVERIP = address;
    }

     public byte[] readResponse() {

        running = true;

        try {
            Log.i(tag, "Connecting to server.....");
            //commSocket.connect(address);
            InetAddress serverAddress = InetAddress.getByName(SERVERIP);
            commSocket = new Socket(serverAddress, portNo);
            Log.i(tag, "Connected to server: " + commSocket.getRemoteSocketAddress());
            Log.i(tag, "Ab hogi guftagu");
            try {
                byte[] buffer = new byte[2];
                InputStream in = commSocket.getInputStream();
                in.read(buffer);
                Log.i(tag, "Data received: " + buffer[0] + " " + buffer[1]);
                return buffer;
            }
            catch(IOException ex) {
                Log.e(tag, ex.getMessage());
            }
        }
        catch(IOException ex) {
            Log.e(tag, ex.getMessage());
        }
        return(null);
    }

    void cancel() {
        try {
            Log.i(tag, "Closing TCPsocket");
            commSocket.close();
            Log.i(tag, "TCPSocket closed");
        }
        catch(IOException ex) {
            Log.e(tag, "Error in closing the socket: " + ex.getMessage());
        }
    }
}