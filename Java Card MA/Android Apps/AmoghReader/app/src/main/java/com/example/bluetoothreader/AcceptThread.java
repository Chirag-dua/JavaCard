package com.example.bluetoothreader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.util.UUID;
import java.io.IOException;
import java.io.OutputStream;

public class AcceptThread extends Thread {
	private final BluetoothServerSocket serverSocket;
	
	private final String tag = "BLUETOOTH_READER_THREAD";
	
	AcceptThread(BluetoothAdapter adapter) {
		BluetoothServerSocket temp = null;
		try {
			temp = adapter.listenUsingRfcommWithServiceRecord("com.example.bluetoothreader", UUID.fromString("851506f2-e9c2-4fa0-9584-467c3c107122"));
		}
		catch(IOException ex) {
			Log.e(tag, "Cannot create server socket: " + ex.getMessage());
		}
		serverSocket = temp;
	}
	
	public void run() {
		BluetoothSocket commSocket = null;
		try {
			Log.i(tag, "Listening for a client");
			commSocket = serverSocket.accept();
			Log.i(tag, "Client found");
		}
		catch(IOException ex) {
			Log.e(tag, "Error in listening: " + ex.getMessage());
		}
		if(commSocket != null) {
			/* Communicate over here */
			try {
				OutputStream out = commSocket.getOutputStream();
				out.write("Amogh".getBytes());
			}
			catch(IOException ex) {
				Log.e(tag, "Cannot write data: " + ex.getMessage());
			}
			try {
				serverSocket.close();
			}
			catch(IOException ex) {
				Log.e(tag, "Cannot close the socket: " + ex.getMessage());
			}
		}
	}
	
	public void cancel() {
		try {
			Log.i(tag, "Closing server socket");
			serverSocket.close();
			Log.i(tag, "Server socket closed");
		}
		catch(IOException ex) {
			Log.e(tag, "Cannot close the socket: " + ex.getMessage());
		}
	}
}
