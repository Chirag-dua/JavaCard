package com.example.rishabh.bluetoothreaderasymm;

import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class TakeTCPAddress extends AppCompatActivity {

    //private TCPClient client;
    private final String TAG = "TakeTCPAddress";
    JavaCard javaCard;
    private MutualAuthenticationThread maThread = null;
    private String locationAddress;
    public String fileName = "MyTextFile.txt";
    public String dirName = "ReaderTextFiles";


    private final Handler uiHandler = new Handler() {
        // TODO: Do the weak reference thing
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String timings = (String) msg.obj;
            Bundle bundle = new Bundle();
            bundle.putString("TIMINGS", timings);
            bundle.putInt("WHAT", msg.what);
            Intent intent = new Intent(TakeTCPAddress.this, ResultActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_tcpaddress);
        Intent intent = getIntent();
        locationAddress = null;
        if(intent.hasExtra("LocationServerAddress")) {
            locationAddress = intent.getStringExtra("LocationServerAddress");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        javaCard.SDSCDisconnectDev();
        Log.e(TAG, "Destroying card");
        if(maThread != null && maThread.isAlive()) {
            maThread.cancel();
        }
        Log.i(TAG, "TakeTCTAddress is destroyed");
    }

    public void connectToServer(View v) {
        EditText addressBox = (EditText)findViewById(R.id.address);
        String address = addressBox.getText().toString();
        javaCard = JavaCard.getInstance(getApplicationContext());
        maThread = new MutualAuthenticationThread(locationAddress,
                address,
                MainActivity.OPTION_TCP,
                javaCard,
                uiHandler);
        maThread.start();
    }
}
