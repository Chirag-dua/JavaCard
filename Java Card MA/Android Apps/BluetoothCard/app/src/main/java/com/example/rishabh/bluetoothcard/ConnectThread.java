package com.example.rishabh.bluetoothcard;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by rishabh on 8/4/16.
 */
public class ConnectThread extends Thread {
    private final String tag = "ConnectThread";
    final private BluetoothAdapter adapter;
    final private BluetoothDevice device;

    ConnectThread(String address) {
        adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice temp = null;
        if(adapter == null) {
            Log.e(tag, "Bluetooth not supported");
        }
        if(adapter != null) {
            temp = adapter.getRemoteDevice(address);
        }
        device = temp;
    }

    public void waitTillBtEnable() {
        try {
            while (!adapter.isEnabled()) {
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        super.run();

    }





}
