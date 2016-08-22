package com.example.rishabh.bluetoothreaderasymm;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by rishabh on 1/5/16.
 */
public class TCPConnection {
    private final static String TAG = "TCPConnection";
    private final Socket socket;
    public final InputStream in;
    public final OutputStream out;
    TCPConnection(Socket socket) {
        this.socket = socket;
        InputStream tempin = null;
        OutputStream tempout = null;
        try {
            tempin = socket.getInputStream();
            tempout = socket.getOutputStream();
        }
        catch(IOException ex) {
            Log.e(TAG, "Error in getting the streams: " + ex.getMessage());
            ex.printStackTrace();
        }
        in = tempin;
        out = tempout;
    }

    public void write(byte[] bytes) {
        try {
            out.write(bytes);
            out.flush();
        }
        catch(IOException ex) {
            Log.e(TAG, "Error in writing message");
        }
    }

    public byte[] read() {
        int bytes=0;
        byte[] buffer = new byte[1024];
        while(true) {
            try {
                bytes = in.read(buffer);
                break;
            }
            catch(IOException ex) {
                Log.e(TAG, "Error in reading: " + ex.getMessage());
            }
        }
        if(bytes>0)
            return (Arrays.copyOf(buffer, bytes));
        else
            return null;
    }

    void cancel() {
        try {
            Log.i(TAG, "Closing tcpsocket");
            in.close();
            out.close();
            socket.close();
            Log.i(TAG, "TCPSocket closed");
        }
        catch(IOException ex) {
            Log.e(TAG, "Error in closing the socket: " + ex.getMessage());
        }
    }
}
