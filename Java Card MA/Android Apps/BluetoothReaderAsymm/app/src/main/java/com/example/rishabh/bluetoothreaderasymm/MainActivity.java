package com.example.rishabh.bluetoothreaderasymm;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    private BluetoothAdapter adapter;

    public static final int OPTION_BLUETOOTH = 0;
    public static final int OPTION_TCP = 1;

    public static String fileName = "MyTextFile.txt";
    public static String dirName = "ReaderTextFiles";

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static File getTextStorageDir(String fileName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), fileName);
        if(file.isDirectory()) {
            Log.d(TAG, "Directory already present");
        }
        else if(!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        else {
            Log.d(TAG, "Directory created");
        }
        return file;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "App is closing");
    }


    public void startBluetoothMA(View v) {
        Log.d(TAG, "Bluetooth button clicked");
        Intent intent = new Intent(this, QRCodeActivity.class);
        Bundle bundle = new Bundle();
        adapter = BluetoothAdapter.getDefaultAdapter();
        bundle.putString("address", adapter.getAddress());
        bundle.putInt("OPTION", OPTION_BLUETOOTH);
        /** Location server */
        EditText addressBox = (EditText)findViewById(R.id.locationServerAddress);
        bundle.putString("LocationServerAddress", addressBox.getText().toString());
        /** End */
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void startTCPMA(View v) {
        Log.d(TAG, "TCP button clicked");
        Intent intent = new Intent(this, TakeTCPAddress.class);
        /** Location server */
        Bundle bundle = new Bundle();
        EditText addressBox = (EditText)findViewById(R.id.locationServerAddress);
        bundle.putString("LocationServerAddress", addressBox.getText().toString());
        intent.putExtras(bundle);
        /** End */
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");
    }
}
