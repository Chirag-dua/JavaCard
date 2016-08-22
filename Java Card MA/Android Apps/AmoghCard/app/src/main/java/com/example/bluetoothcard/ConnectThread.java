package com.example.bluetoothcard;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class ConnectThread extends Thread {

	private final BluetoothSocket commSocket;
	
	public static final int MESSAGE_READ = 2;
	
	private Handler uiHandler;
	
	private final String tag = "BLUETOOTH_CARD_THREAD";
	
	ConnectThread(BluetoothDevice device, Handler mHandler) {
		BluetoothSocket temp = null;
		try {
			temp = device.createRfcommSocketToServiceRecord(UUID.fromString("851506f2-e9c2-4fa0-9584-467c3c107122"));
		}
		catch(IOException ex) {
			
		}
		commSocket = temp;
		uiHandler = mHandler;
	}
	
	public void run() {
		try {
			Log.i(tag, "Connecting to server socket");
			commSocket.connect();
			Log.i(tag, "Connection established");
		}
		catch(IOException ex) {
			try {
				Log.e(tag, "Error in connecting: " + ex.getMessage());
				commSocket.close();
			}
			catch(IOException closeEx) { }
			return;
		}
		/* Communication part */
		try {
			InputStream in = commSocket.getInputStream();
			byte[] buffer = new byte[128];
			int bytes = in.read(buffer);
			/* Send it to the main thread */
			uiHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
		}
		catch(IOException ex) {
			Log.e(tag, "Cannot read data: " + ex.getMessage());
		}
	}
	
	public void cancel() {
		try {
			commSocket.close();
		}
		catch(IOException ex) {
			Log.e(tag, "Cannot close the socket: " + ex.getMessage());
		}
	}
}
