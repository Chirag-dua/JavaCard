package com.example.epurse;

import android.annotation.TargetApi;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Build;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;

import android.widget.TextView;
import android.widget.EditText;

import android.content.Intent;

import com.gotrust.sesdapi.SESDAPI;
import com.gotrust.sesdapi.SDSCException;

import java.util.Vector;
import java.io.File;

public class MyActivity extends ActionBarActivity {
	
	private static final int DEFAULT_SIZE = 256;
	
	public static final String EXTRA_MESSAGE = "com.example.epurse.EXTRA_MESSAGE";
	
	/* The name of the IO file which will be binded for Input-Output on the card */
	private final String ioFileName = "SMART_IO.CRD";
	
	/* The file descriptor of the device */
	private int phDevice = -1;
	
	/* APDU for selecting EPurse applet */
	private byte[] selectAPDU = {0x00, (byte)0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03, 0x04, 0x05,
								 0x06, 0x07, 0x08, 0x09, 0x03, 0x01};
	/* APDU for retrieving Balance */
	private byte[] getBalAPDU = {(byte)0xA0, (byte)0xB0, (byte)0x00, (byte)0x00, (byte)0x02};
	
	/* This buffer is used for retrieving response from the Java Card */
	private byte[] bOutData = new byte[DEFAULT_SIZE];
	
	/* The SESDAPI Object which will be used for communication */
	private SESDAPI obj = new SESDAPI();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my);
		
		/* Initialize the card */
		try {
			Vector<String> devices = goListDevs();
			if(devices.size() == 1)
				this.phDevice = goConnectDev((String)devices.elementAt(0));
			obj.SDSCTransmitEx(phDevice, selectAPDU, 0, selectAPDU.length, SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
		}
		catch(SDSCException e) {
			/* TODO: Handle the exception */
			
		}
	}
	
	@Override
	protected void onResume() {
		try {
			Vector<String> devices = goListDevs();
			if(devices.size() == 1)
				this.phDevice = goConnectDev((String)devices.elementAt(0));
			obj.SDSCTransmitEx(phDevice, selectAPDU, 0, selectAPDU.length, SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
		}
		catch(SDSCException e) {
			/* TODO: Handle the exception */
		}
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		if(this.phDevice != - 1) {
			obj.SDSCDisconnectDev(this.phDevice);
			this.phDevice = -1;
		}
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.my, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch(item.getItemId()) {
		case R.id.action_select_device:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private Vector<String> goListDevs() throws SDSCException {
		StringBuffer name = new StringBuffer();
		Vector<String> devices;
		
		/* Retrieve the list of connected devices */
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			File[] accessible = getExternalFilesDirs(null);
			if(accessible.length < 2 || accessible[1] == null) {
				/* TODO: Throw some exception */
			}
			String file = accessible[1].getPath();
			name.append(file.substring(0, file.indexOf("/files") + 1));
			name.append(this.ioFileName);
			devices = obj.SDSCListDevs(name.toString());
		}
		else {
			/* For non Kitkat versions, root directory is accessible */
			devices = obj.SDSCListDevs(this.ioFileName);
		}
		return devices;
	}
	
	private int goConnectDev(String ioFilePath) throws SDSCException {
		return obj.SDSCConnectDev(ioFilePath);
	}
	
	public void displayBalance(View view) {
		Intent intent = new Intent(this, DisplayMessageActivity.class);
		StringBuffer message = new StringBuffer();
		
		/* Send command APDU */
		try {
			int len = obj.SDSCTransmitEx(phDevice, getBalAPDU, 0, getBalAPDU.length, SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
			if(bOutData[len - 1] != (byte)0x00 || bOutData[len - 2] != (byte)0x90) {
				message.append("Error: Cannot retrieve balance: ");
				int err = (0x000000FF & bOutData[len - 2]) << 8;
				err = (err & 0xFFFFFF00) | (0x000000FF & bOutData[len - 1]);
				message.append(err);
				message.append('\n');
			}
			else {
				message.append("Balance is ");
				int x = (0x000000FF & bOutData[0]) << 8;
				x = (x & 0xFFFFFF00) | (0x000000FF & bOutData[1]);
				message.append(x);
				message.append('\n');
			}
		}
		catch(SDSCException e) {
			message.append("Error: " + e.getMessage() + "\n");
		}
		
		intent.putExtra(EXTRA_MESSAGE, message.toString());
		startActivity(intent);
	}
	
	public void addAmount(View view) {
		Intent intent = new Intent(this, DisplayMessageActivity.class);
		EditText editText = (EditText)findViewById(R.id.amount);
		
		try {
			short amt = Short.parseShort(editText.getText().toString());
			
			/* Construct command APDU */
			byte[] APDU = new byte[7];
			APDU[0] = (byte)0xA0; APDU[1] = (byte)0xB2; APDU[2] = (byte)0x00;
			APDU[3] = (byte)0x00; APDU[4] = (byte)0x02;
			APDU[5] = (byte)(0xFF & (amt >> 8));
			APDU[6] = (byte)(0xFF & amt);
			int len = obj.SDSCTransmitEx(phDevice, APDU, 0, 7, SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
			if(bOutData[len - 1] != (byte)0x00 || bOutData[len - 2] != (byte)0x90) {
				StringBuffer message = new StringBuffer();
				message.append("Error: Cannot retrieve balance: ");
				int err = (0x000000FF & bOutData[len - 2]) << 8;
				err = (err & 0xFFFFFF00) | (0x000000FF & bOutData[len - 1]);
				message.append(String.format("%02x\n", err));
				intent.putExtra(EXTRA_MESSAGE, message.toString());
			}
			else
				intent.putExtra(EXTRA_MESSAGE, "Amount has been added\n");	
		}
		catch(SDSCException e) {
			intent.putExtra(EXTRA_MESSAGE, "Error: " + e.getMessage() + "\n");
		}
		catch(NumberFormatException e) {
			intent.putExtra(EXTRA_MESSAGE, "Invalid Input\n");
		}
		startActivity(intent);
	}
	
	public void subtractAmount(View view) {
		Intent intent = new Intent(this, DisplayMessageActivity.class);
		EditText editText = (EditText)findViewById(R.id.amount);
		
		try {
			short amt = Short.parseShort(editText.getText().toString());
			
			/* Construct command APDU */
			byte[] APDU = new byte[7];
			APDU[0] = (byte)0xA0; APDU[1] = (byte)0xB4; APDU[2] = (byte)0x00;
			APDU[3] = (byte)0x00; APDU[4] = (byte)0x02;
			APDU[5] = (byte)(0xFF & (amt >> 8));
			APDU[6] = (byte)(0xFF & amt);
			int len = obj.SDSCTransmitEx(phDevice, APDU, 0, 7, SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
			if(bOutData[len - 1] != (byte)0x00 || bOutData[len - 2] != (byte)0x90) {
				StringBuffer message = new StringBuffer();
				message.append("Error: Cannot retrieve balance: ");
				int err = (0x000000FF & bOutData[len - 2]) << 8;
				err = (err & 0xFFFFFF00) | (0x000000FF & bOutData[len - 1]);
				message.append(String.format("%02x\n", err));
				intent.putExtra(EXTRA_MESSAGE, message.toString());
			}
			else
				intent.putExtra(EXTRA_MESSAGE, "Amount has been deducted\n");
		}
		catch(SDSCException e) {
			intent.putExtra(EXTRA_MESSAGE, "Error: " + e.getMessage() + "\n");
		}
		catch(NumberFormatException e) {
			intent.putExtra(EXTRA_MESSAGE, "Invalid Input\n");
		}
		startActivity(intent);
	}
}
