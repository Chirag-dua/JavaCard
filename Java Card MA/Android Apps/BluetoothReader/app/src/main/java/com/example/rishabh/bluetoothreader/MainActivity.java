package com.example.rishabh.bluetoothreader;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.graphics.Bitmap;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import com.gotrust.sesdapi.SDSCException;
import com.gotrust.sesdapi.SESDAPI;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter adapter;

    private String tag = "BLUETOOTH_READER";

    public static final int OPTION_BLUETOOTH = 0;
    public static final int OPTION_TCP = 1;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(tag, "App is closing");
    }


    public void startBluetoothMA(View v) {
        Log.d(tag, "Bluetooth button clicked");
        Intent intent = new Intent(this, QRCodeActivity.class);
        Bundle bundle = new Bundle();
        adapter = BluetoothAdapter.getDefaultAdapter();
        bundle.putString("address", adapter.getAddress());
        bundle.putInt("OPTION", OPTION_BLUETOOTH);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void startTCPMA(View v) {
        Log.d(tag, "TCP button clicked");
        Intent intent = new Intent(this, TakeTCPAddress.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(tag, "onCreate");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
