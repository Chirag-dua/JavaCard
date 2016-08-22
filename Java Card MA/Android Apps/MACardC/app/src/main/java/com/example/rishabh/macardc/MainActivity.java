package com.example.rishabh.macardc;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


import com.gotrust.sesdapi.SESDAPI;
import com.gotrust.sesdapi.SDSCException;

import java.io.File;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private static final String tag = "MainActivity PAtient";

    private static final int DEFAULT_SIZE = 256;

    /* The name of the IO file which will be binded for Input-Output on the card */
    private final String ioFileName = "SMART_IO.CRD";

    /* The file descriptor of the device */
    private int phDevice = -1;

    /* The SESDAPI Object which will be used for communication */
    private SESDAPI obj = new SESDAPI();

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String getIOFilePath() {
        StringBuffer name = new StringBuffer();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] accessible = getExternalFilesDirs(null);
            if(accessible.length < 2 || accessible[1] == null) {
				/* TODO: Throw some exception */

            }
            String file = accessible[1].getPath();
            name.append(file.substring(0, file.indexOf("/files") + 1));
            name.append(this.ioFileName);
        }
        else {
            name.append(ioFileName);
        }
        return name.toString();
    }

    private Vector<String> goListDevs() throws SDSCException {
        String name = getIOFilePath();
        return obj.SDSCListDevs(name);
    }

    private int goConnectDev(String ioFilePath) throws SDSCException {
        return obj.SDSCConnectDev(ioFilePath);
    }

    private void goCreateIOFile(final String ioFilePath) {
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            if(ioFilePath == null) {

            }
            else {
                Thread t1 = new Thread(new Runnable() {
                    public void run() {
                        int n = 0;
                        try {

                                n = obj.SDSCCreateIoFile(ioFilePath);
                        }
                        catch(SDSCException ex) {
                            Log.e(tag, "Error in iofile generation: " + ex.getMessage());
                            Log.e(tag, "More memory required: " + n);
                        }
                    }
                });
                t1.start();
            }
        }
        else {
			/* No need to do this for non-Kitkat versions */
            return;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent i = new Intent(this, MyService.class);
        startService(i);
        Log.i(tag, "Service started");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent i = new Intent(this, MyService.class);
        stopService(i);
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
        if (id == R.id.ioFile) {
            String name = getIOFilePath();
            goCreateIOFile(name);
        }


        return super.onOptionsItemSelected(item);
    }
}

