package com.example.bluetoothreader;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;

public class MainActivity extends Activity {
	
	private BluetoothAdapter adapter;
	
	private String tag = "BLUETOOTH_READER";
	
	private int REQUEST_ENABLE_BT = 1;
	
	private AcceptThread thread = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		adapter = BluetoothAdapter.getDefaultAdapter();
		if(adapter == null) {
			Log.e(tag, "Bluetooth not supported");
			return;
		}
		/* Enable bluetooth */
		try {
			Bitmap code = encodeAsBitmap(adapter.getAddress());
			ImageView image = (ImageView)findViewById(R.id.qrcode);
			image.setImageBitmap(code);
		}
		catch(WriterException ex) {
			Log.e(tag, "Cannot display QR Code: " + ex.getMessage());
		}
		if(!adapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		else {
			thread = new AcceptThread(adapter);
			thread.start();
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
			Log.i(tag, "Bluetooth Disabled");
		}
		else if(requestCode == REQUEST_ENABLE_BT) {
			/* Listen for a connection */
			thread = new AcceptThread(adapter);
			thread.start();
		}
	}
	
	Bitmap encodeAsBitmap(String str) throws WriterException {
	    BitMatrix result;
	    try {
	        result = new MultiFormatWriter().encode(str, 
	            BarcodeFormat.QR_CODE, 1000, 1000, null);
	        Log.d(tag, "String Encoded in QR Code");
	    } catch (IllegalArgumentException iae) {
	        // Unsupported format
	    	Log.e(tag, "Format not supported: " + iae.getMessage());
	        return null;
	    }
	    int w = result.getWidth();
	    int h = result.getHeight();
	    int[] pixels = new int[w * h];
	    for (int y = 0; y < h; y++) {
	        int offset = y * w;
	        for (int x = 0; x < w; x++) {
	            pixels[offset + x] = result.get(x, y) ?  0xFF000000 : 0xFFFFFFFF;
	        }
	    }
	    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	    bitmap.setPixels(pixels, 0, 1000, 0, 0, w, h);
	    return bitmap;
	}
}
