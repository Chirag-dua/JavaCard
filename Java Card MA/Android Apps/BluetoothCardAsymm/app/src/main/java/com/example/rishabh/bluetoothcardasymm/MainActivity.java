package com.example.rishabh.bluetoothcardasymm;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    String address;
    private final static String tag = "MainActivity";
    private BluetoothAdapter adapter;
    public final static int OPTION_BLUETOOTH = 0;
    public final static int OPTION_TCP = 1;
    JavaCard javaCard;

    public static String fileName = "MyTextFile.txt";
    public static String dirName = "CardTextFiles";

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
            Log.d(tag, "Directory already present");
        }
        else if(!file.mkdirs()) {
            Log.e(tag, "Directory not created");
        }
        else {
            Log.d(tag, "Directory created");
        }
        return file;
    }

    //onClick fn for the ReadAddress button
    public void readBluetoothAddress(View v) {
        Intent intent = new Intent(this, CameraTestActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt("OPTION", OPTION_BLUETOOTH);
        intent.putExtras(bundle);
        startActivity(intent);
        //address = "C4:3A:BE:BD:9C:00";
    }

    //onClick for TCP connection
    public void connectTCP(View v) {
        Intent intent = new Intent(this, TakeTCPAddress.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        javaCard = JavaCard.getInstance(getApplicationContext());
        javaCard.SDSCDisconnectDev();
        Log.d(tag, "onDestroy");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
