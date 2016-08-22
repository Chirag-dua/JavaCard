package com.example.rishabh.extendedapducard;

import java.io.File;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.util.Arrays;
import java.util.Vector;


import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

public class MyService extends EmulatorService {
    public static final String tag = "libHCEEmulator";
    public static String msg = "";



    /* Mutual Authentication constants*/

    private byte[] selectAPDU = {0x00, (byte)0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03,
            0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x08, 0x02};



    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(tag, "Service started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(tag, "Service destroyed");
    }
    private static final int DEFAULT_SIZE = 256;

    /* This buffer is used for retrieving response from the Java Card */
    private byte[] bOutData = new byte[DEFAULT_SIZE];


    @Override
    public Response onReceiveCommand(Command command)
    {
        //StringBuffer str = new StringBuffer();

        //Log.d(tag, " DATA : " + new String(command.getPayload()));
        Response res = new Response();
        return res;
    }
}
