package com.example.rishabh.bluetoothcardasymm;

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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class TakeTCPAddress extends AppCompatActivity {

    //private TCPClient client;
    private final String TAG = "TakeTCPAddress";
    JavaCard javaCard;
    private MutualAuthenticationThread maThread = null;
    public String fileName = "MyTextFile.txt";
    public String dirName = "CardTextFiles";

    private final Handler uiHandler = new Handler() {
        // TODO: Do the weak reference thing

        /* Checks if external storage is available for read and write */
        public boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
            return false;
        }

        /* Checks if external storage is available to at least read */
        public boolean isExternalStorageReadable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state) ||
                    Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                return true;
            }
            return false;
        }

        public File getTextStorageDir(String fileName) {
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
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            /*
            if(msg.what == ResultActivity.FILE_TRANSFER) {
                TCPConnection tcpConnection = (TCPConnection) msg.obj;
                File dir = null;
                File file = null;
                if(isExternalStorageWritable()) {
                    dir = getTextStorageDir(dirName);
                    file = new File(dir, fileName);
                    BufferedReader brin = new BufferedReader(new InputStreamReader(tcpConnection.in));
                    BufferedWriter frout = null;
                    try {
                        frout = new BufferedWriter(new FileWriter(file));
                        String readData = null;
                        while((readData = brin.readLine()) != null) {
                            Log.d(TAG, "Data read length: " + readData.length());
                            frout.write(readData, 0, readData.length());
                        }
                        frout.close();
                        tcpConnection.cancel();
                    }
                    catch(IOException ex) {
                        ex.printStackTrace();
                    }
                    /*
                    FileOutputStream fout = null;
                    try {
                        fout = new FileOutputStream(file);
                        //String data = "This is new content";
                        Log.d(TAG, "Size of the data: " + data.length());
                        fout.write(data.getBytes());
                        fout.flush();
                        fout.close();
                    }
                    catch(IOException ex) {
                        ex.printStackTrace();
                    }

                }

                if(isExternalStorageReadable()) {
                    dir = getTextStorageDir(dirName);
                    file = new File(dir, fileName);
                    BufferedInputStream fin = null;
                    BufferedReader frin = null;
                    BufferedWriter brout = new BufferedWriter(new OutputStreamWriter(tcpConnection.out));
                    try {
                        frin = new BufferedReader(new FileReader(file));
                        String readData = null;
                        while((readData = frin.readLine()) != null) {
                            Log.d(TAG, "Data read length: " + readData.length());
                            brout.write(readData, 0, readData.length());
                        }
                        //fin = new BufferedInputStream(new FileInputStream(file));
                        brout.close();
                        tcpConnection.cancel();
                    }
                    catch(IOException ex) {
                        ex.printStackTrace();
                    }
                    //BufferedWriter bout = new BufferedWriter(new OutputStreamWriter(tcpConnection.out));
                }
                return;
             }
             */
        }
    };

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
                MainActivity.OPTION_TCP, javaCard, uiHandler);
        maThread.start();
        //client = new TCPClient(addressBox.getText().toString());
        //client.start();
    }
}
