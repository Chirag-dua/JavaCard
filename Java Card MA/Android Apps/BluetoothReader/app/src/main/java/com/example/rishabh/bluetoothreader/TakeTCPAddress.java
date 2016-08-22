package com.example.rishabh.bluetoothreader;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class TakeTCPAddress extends AppCompatActivity {

    //private TCPClient client;
    private final String TAG = "TakeTCPAddress";
    JavaCard javaCard;
    private MutualAuthenticationThread maThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_tcpaddress);
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
        javaCard = JavaCard.getInstance(getApplicationContext());
        maThread = new MutualAuthenticationThread(addressBox.getText().toString(),
                MainActivity.OPTION_TCP, javaCard);
        maThread.start();
        //client = new TCPClient(addressBox.getText().toString());
        //client.start();
    }
}
