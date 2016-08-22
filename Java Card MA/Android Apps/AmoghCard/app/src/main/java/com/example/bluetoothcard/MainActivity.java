package com.example.bluetoothcard;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.util.Log;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {
	
	/* Bluetooth address of the reader */
	private String BTAddress = null;
	
	public static final String EXTRA_ADDRESS = "com.example.bluetoothcard.EXTRA_ADDRESS";
	
	private BluetoothDevice device;
	
	private BluetoothAdapter adapter;
	
	private String tag = "BLUETOOTH_CARD";
	
	private int REQUEST_ENABLE_BT = 1;
	
	private int BT_OBTAIN = 2;
	
	private ConnectThread thread = null;
	
	private Handler mHandler;
	
	private TextView textView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		BTAddress = getIntent().getStringExtra(EXTRA_ADDRESS);
		//BTAddress = "00:1A:7D:DA:71:13";
		Log.i(tag, BTAddress);
		textView = (TextView)findViewById(R.id.text_box);
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				if(msg.what == ConnectThread.MESSAGE_READ) {
					byte[] buffer = (byte [])msg.obj;
					int bytes = msg.arg1;
					File[] accessible = getExternalFilesDirs(null);
					String file = accessible[0].getPath();
					StringBuffer name = new StringBuffer();
					name.append(file.substring(0, file.indexOf("/files") + 1));
					name.append("Pi");
					Log.i(tag, "Filename is: " + name);
					File encryptFile = new File(name.toString());
					try {
						FileOutputStream out = new FileOutputStream(encryptFile);
						out.write(buffer, 0, bytes);
						out.close();
					}
					catch(IOException ex) {
						Log.e(tag, "Cannot write data: " + ex.getMessage());
					}
				}
			}
		};
		adapter = BluetoothAdapter.getDefaultAdapter();
		device = adapter.getRemoteDevice(BTAddress);
		if(adapter == null) {
			Log.e(tag, "Bluetooth not supported");
			return;
		}
		
		/* Enable bluetooth */
		if(!adapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		else {
			thread = new ConnectThread(device, mHandler);
			thread.run();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	 public void onDestroy() {
		 super.onDestroy();
		if(thread != null && thread.isAlive()) {
			thread.cancel();
		}
		Log.i(tag, "App is closing");
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
			Log.e(tag, "Bluetooth disabled");
			return;
		}
		else if(requestCode == REQUEST_ENABLE_BT) {
			thread = new ConnectThread(device, mHandler);
			thread.run();
		}
		/* else if(requestCode == BT_OBTAIN && resultCode == Activity.RESULT_CANCELED) {
			Log.i(tag, "Didn't scanned BT Address");
			return;
		}
		else if(requestCode == BT_OBTAIN) {
			/* Obtain the remote bluetooth device object using the MAC Address */
			/* BTAddress = data.getStringExtra(EXTRA_ADDRESS);
			Log.i(tag, "Obtained BT Address: " + BTAddress);
			device = adapter.getRemoteDevice(BTAddress);
		} */
	}
}
