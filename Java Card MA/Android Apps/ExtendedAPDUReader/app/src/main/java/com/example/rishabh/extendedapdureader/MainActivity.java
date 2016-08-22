package com.example.rishabh.extendedapdureader;

import android.annotation.TargetApi;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

public class MainActivity extends ReaderActivity
{

    public static final String tag = "Main Activity";
    public static int mMaxTransceiveLength;
    public static boolean mExtendedApduSupported;
    public static int mTimeout;

    public static Command command;
    public static Command commandAuth;
    public static Response response;

    /* Mutual Authentication constants*/

    private byte[] selectAPDU = {0x00, (byte)0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03,
            0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x08, 0x01};

    private byte[] getSignFromSE = {(byte)0xA0, (byte) 0xB5, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] getModulusIDFromSE = {(byte)0xA0, (byte) 0xB3, (byte)0x00,
            (byte) 0x00, (byte)0x88};

    private byte[] verifyCertFromSE = new byte[133];
    private byte[] askEncStuff = {(byte)0xA0, (byte) 0xC3, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] aes_key = {0x01, (byte)0xe5, 0x66, 0x42, 0x6a, 0x10, (byte)0xca, 0x21, 0x7e,
            (byte) 0x83, 0x26, (byte)0xb4, 0x37, (byte)0xa8, (byte)0xc9, (byte)0xf4};
    private byte ASK_CERT = (byte) 0x32;
    private byte SEND_SIGN = (byte) 0x30;
    private byte SEND_KEYID = (byte) 0x31;
    private byte SEND_CONF = (byte) 0x33;
    private byte ASK_CONF = (byte) 0x34;
    private byte SEND_FINAL_CONF_CARD = (byte)0xC4;
    private byte SEND_ENC_STUFF_TO_CARD = (byte)0xC2;
    private byte[] CERT_FAILED = {(byte)0x69, (byte)0x84};

    private static final int DEFAULT_SIZE = 256;

    /* This buffer is used for retrieving response from the Java Card */
    private byte[] bOutData = new byte[DEFAULT_SIZE];


    private byte[] n1;
    //private byte[] n1n2;
    //private  TextView textView;
    /* Mutual Authentication constants end */


    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(tag, "starting reader");
        enableReaderMode();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        disableReaderMode();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onHceStarted(IsoDep isoDep)
    {
        Log.d(tag, "onHCEStarted");
        isoDep.setTimeout(36000);
        Log.i(tag, "Max transceive length: " + isoDep.getMaxTransceiveLength());
        boolean flag = isoDep.isExtendedLengthApduSupported();
        if(flag)
            Log.i(tag, "Extended apdu is supported");
        else
            Log.i(tag, "Extended apdu is not supported");

        byte[] myApdu = new byte[350];
        for(int i = 0; i<myApdu.length; i++)
            myApdu[i] = (byte)(i%256);
        Log.d(tag, "apdu to be sent: " + Utils.ByteArrayToHexString(myApdu));
        byte[] resp = null;
        try
        {
            resp = isoDep.transceive(myApdu);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        Log.d(tag, "Response received: " + Utils.ByteArrayToHexString(resp));

        /*
        long startTime = System.nanoTime();
        boolean success = mutualAuthentication(isoDep);
        if(!success)
            Log.e(tag, "Mutual Authentication failed");
        else
        {
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(tag, "Elapsed time: " + elapsedTime / 1000000);
            //Toast.makeText(this, "Mutual Authentication done", Toast.LENGTH_SHORT).show();
            Log.d(tag, "Mutual Authentication successful");
        }
        */
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }
}
