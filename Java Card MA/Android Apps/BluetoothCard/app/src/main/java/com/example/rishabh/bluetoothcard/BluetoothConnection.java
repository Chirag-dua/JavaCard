package com.example.rishabh.bluetoothcard;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Created by rishabh on 23/4/16.
 */
public class BluetoothConnection {
    private static final String tag = "BluetoothConnection";
    private final BluetoothSocket socket;
    private final InputStream in;
    private final OutputStream out;
    BluetoothConnection(BluetoothSocket socket) {
        this.socket = socket;
        InputStream tempin = null;
        OutputStream tempout = null;
        try {
            tempin = socket.getInputStream();
            tempout = socket.getOutputStream();
        }
        catch(IOException ex) {
            Log.e(tag, "Error in getting the streams: " + ex.getMessage());
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
            Log.e(tag, "Error in writing message");
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
                Log.e(tag, "Error in reading: " + ex.getMessage());
            }
        }
        if(bytes>0)
            return (Arrays.copyOf(buffer,bytes));
        else
            return null;
    }

    void cancel() {
        try {
            Log.i(tag, "Closing bluetoothsocket");
            in.close();
            out.close();
            socket.close();
            Log.i(tag, "BluetoothSocket closed");
        }
        catch(IOException ex) {
            Log.e(tag, "Error in closing the socket: " + ex.getMessage());
        }
    }
}
