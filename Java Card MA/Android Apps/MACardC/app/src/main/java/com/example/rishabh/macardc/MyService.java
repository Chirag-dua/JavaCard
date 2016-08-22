package com.example.rishabh.macardc;

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

import com.gotrust.sesdapi.SDSCException;
import com.gotrust.sesdapi.SESDAPI;


public class MyService extends EmulatorService {
    public static final String tag = "libHCEEmulator";
    public static String msg = "";

    /* The name of the IO file which will be binded for Input-Output on the card */
    private final String ioFileName = "SMART_IO.CRD";


    /* Mutual Authentication constants*/

    private byte[] selectAPDU = {0x00, (byte)0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03,
            0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x08, 0x02};



    /** The SESDAPI Object which will be used for communication */
    public SESDAPI obj = new SESDAPI();                 /** Make it private later and find an alt */

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String getIOFilePath() {
        StringBuffer name = new StringBuffer();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] accessible = getExternalFilesDirs(null);
            if(accessible.length < 2 || accessible[1] == null) {
                Log.e(tag, "Error in getting IO file path");
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
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            if (ioFilePath == null) {

            } else {
                Thread t1 = new Thread(new Runnable() {
                    public void run() {
                        try {
                            obj.SDSCCreateIoFile(ioFilePath);
                        } catch (SDSCException ex) {

                        }
                    }
                });
                t1.start();
            }
        } else {
			/* No need to do this for non-Kitkat versions */
            return;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(tag, "Service started");
        /* Initialize the card */
        try {
            Vector<String> devices = goListDevs();
            if(devices.size() == 1)
                this.phDevice = goConnectDev((String)devices.elementAt(0));
            obj.SDSCTransmitEx(phDevice, selectAPDU, 0, selectAPDU.length,
                    SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        }
        catch(SDSCException e) {
            Log.e(tag, "Error in initializing card");
            e.printStackTrace();
            return;
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(tag, "Service destroyed");
        obj.SDSCDisconnectDev(phDevice);
    }

    /* Mutual Authentication constants*/

    //private byte[] getModulusAPDU = {(byte)0xA0, (byte)0xB1, (byte)0x00, (byte)0x00, (byte)0x80};
    //private byte[] getRandomAPDU = {(byte)0xA0, (byte)0xB3, (byte)0x10, (byte)0x00, (byte)0x80};
    //private byte[] testMessageAPDU = {(byte)0xA0, (byte)0xB8, (byte)0x00, (byte)0x00, (byte)0x10};
    private byte[] getSignFromSE = {(byte)0xA0, (byte) 0xB5, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] getModulusIDFromSE = {(byte)0xA0, (byte) 0xB3, (byte)0x00,
            (byte) 0x00, (byte)0x88};

    private byte[] askEncStuff = {(byte)0xA0, (byte) 0xC3, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] askFinalConf = {(byte)0xA0, (byte) 0xC4, (byte)0x00, (byte)0x00, (byte)0x80};


    /* The file descriptor of the device */
    private int phDevice = -1;

    private byte[] CERT_FAILED = {(byte)0x69, (byte)0x84};


    private byte[] aes_key = {0x01, (byte)0xe5, 0x66, 0x42, 0x6a, 0x10, (byte)0xca, 0x21, 0x7e,
            (byte) 0x83, 0x26, (byte)0xb4, 0x37, (byte)0xa8, (byte)0xc9, (byte)0xf4};
    private byte ASK_CERT = (byte) 0x31;
    private byte SEND_CERT = (byte) 0x30;
    private byte SEND_CONF = (byte) 0x32;
    private byte ASK_CONF = (byte) 0x33;
    private byte SEND_ENC_STUFF_TO_CARD = (byte)0xC2;

    private static final int DEFAULT_SIZE = 256;

    /* This buffer is used for retrieving response from the Java Card */
    private byte[] bOutData = new byte[DEFAULT_SIZE];

    /* The SESDAPI Object which will be used for communication */
    // private SESDAPI obj = new SESDAPI();

    private byte[] n1;
    private PublicKey pkey_reader = null;
    private KeyPair keys = null;
    private byte[] nr = null;
    private byte[] idr = null;
    private byte[] payload = null;
    private byte[] ks = null;
    private byte[] cipPay = null;
    private byte[] signCertr = null;
    private byte[] apduToBeSent = null;
    //private byte[] n1n2;

    /* Mutual Authentication constants end */

    @Override
    public Response onReceiveCommand(Command command)
    {
        //StringBuffer str = new StringBuffer();

        //Log.d(tag, " DATA : " + new String(command.getPayload()));
        Log.d(tag, " DATA : " + Utils.ByteArrayToHexString(command.getCommandArray()));

        Response res = new Response();
        res.setResponseCode(Response.RESPONSE_SUCCESS_DONE);
        byte[] errorCheckArr = new byte[21];
        for(int i=0;i<21;i++)
            errorCheckArr[i] = (byte)0x01;
        res.setErrorCheck(errorCheckArr);

        byte[] idc = {(byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x02,
                (byte) 0x02, (byte) 0x02, (byte) 0x02};
        switch (command.getIns())
        {
            case ((byte) 0x30):
                long startTime = System.nanoTime();
                /** Receive the signature of certr and store it */
                Log.d(tag, "Receive signature of certr");
                signCertr = command.getPayload();
                Log.d(tag, "Length of the receive signature: " + signCertr.length);

                byte[] sendSignToSE = new byte[128 + 5];
                int j = 0;
                sendSignToSE[j++] = (byte)0xA0;
                sendSignToSE[j++] = (byte)0xC0;
                sendSignToSE[j++] = (byte)0x00;
                sendSignToSE[j++] = (byte)0x00;
                sendSignToSE[j++] = (byte)0x80;
                System.arraycopy(signCertr, 0, sendSignToSE, j, 128);
                int len = 0;
                try
                {
                    len = obj.SDSCTransmitEx(phDevice, sendSignToSE, 0, sendSignToSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
                }
                catch(SDSCException e)
                {
                    Log.e(tag, "SDSCException: " + e.getMessage());
                }

                if(Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
                    Log.d(tag , "Signature sent successfully");
                else
                {
                    Log.e(tag,
                            Utils.ByteArrayToHexString(Arrays.copyOf(bOutData, len)));
                    Log.e(tag, "Signature sending to the SE failed");
                    res.setResponseCode(Response.RESPONSE_ERROR);
                    return res;
                }
                long elapsedTime = System.nanoTime() - startTime;
                Log.i(tag, "Time for sending signature to SE: " + elapsedTime/1000000);
                break;
            case ((byte) 0x31):
                /** Receive key from reader app, send to the SE for verification */
                Log.d(tag, "Receiving key from the reader app");
                startTime = System.nanoTime();
                byte[] keyIDR = command.getPayload();
                Log.d(tag, "keyIDR: " + Utils.ByteArrayToHexString(keyIDR));
                Log.d(tag, "Length of key || ID receieved from reader app : " + keyIDR.length);

                byte[] sendKeyIDRToSE = new byte[136 + 5];
                j = 0;
                sendKeyIDRToSE[j++] = (byte)0xA0;
                sendKeyIDRToSE[j++] = (byte)0xC1;
                sendKeyIDRToSE[j++] = (byte)0x00;
                sendKeyIDRToSE[j++] = (byte)0x00;
                sendKeyIDRToSE[j++] = (byte)0x88;
                System.arraycopy(keyIDR, 0, sendKeyIDRToSE, j, keyIDR.length);
                len = obj.SDSCTransmitEx(phDevice, sendKeyIDRToSE, 0, sendKeyIDRToSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);

                if(Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
                    Log.d(tag , "Certificate verification successful");
                else
                {
                    if(Arrays.equals(Arrays.copyOf(bOutData, len), CERT_FAILED))
                        Log.e(tag, "Certificate verification failed");
                    else
                        Log.e(tag, "Error in certificate verification");
                    res.setResponseCode(Response.RESPONSE_ERROR);
                    return res;
                }
                elapsedTime = System.nanoTime() - startTime;
                Log.i(tag, "Time for sending key||ID to SE: " + elapsedTime/1000000);
                try
                {
                    byte[] signC = new byte[128 + 2];
                    startTime = System.nanoTime();
                    len = obj.SDSCTransmitEx(phDevice, getSignFromSE, 0, getSignFromSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, signC);

                    Log.d(tag, "Signature received from the card: " +
                            Utils.ByteArrayToHexString(Arrays.copyOf(signC,len-2)));
                    Log.d(tag, "Length of the received signature: " + len);
                    elapsedTime = System.nanoTime() - startTime;
                    Log.i(tag, "Time for retrieving signC from the SE: " + elapsedTime/1000000);
                    byte[] message = new byte[136 + 2];
                    startTime = System.nanoTime();
                    int lenMod = obj.SDSCTransmitEx(phDevice, getModulusIDFromSE, 0, getModulusIDFromSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, message);

                    Log.d(tag, "KeyID received from the card: " +
                            Utils.ByteArrayToHexString(Arrays.copyOf(message,lenMod-2)));
                    elapsedTime = System.nanoTime() - startTime;
                    Log.i(tag, "Time for retrieving Key||ID from the SE: " + elapsedTime / 1000000);
                    Log.d(tag, "Length of the received key || id: " + lenMod);
                    startTime = System.nanoTime();
                    byte[] nextMessageToBeSent = readEncStuff();
                    Log.d(tag, "Enc stuff received from the card: " +
                            Utils.ByteArrayToHexString(nextMessageToBeSent));
                    Log.d(tag, "Length of enc Stuff received from the card: " +
                            nextMessageToBeSent.length);
                    elapsedTime = System.nanoTime() - startTime;
                    Log.i(tag, "Time for retrieving E(Pbr, nc||IDc) from SE: " + elapsedTime/1000000);
                    apduToBeSent = Utils.ConcatArrays(Arrays.copyOf(signC,len-2),
                            Arrays.copyOf(message,lenMod-2), nextMessageToBeSent);
                    Log.d(tag, "CERT || EncStuff: " + Utils.ByteArrayToHexString(apduToBeSent));

                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                break;
            case ((byte) 0x32):
                Log.d(tag, "Send certc ");
                res.setPayload(apduToBeSent);
                break;
            case ((byte) 0x33):
                /** Receive confirmation */
                Log.d(tag, "Length of command payload received: " + command.getPayload().length);
                byte[] encStuff = command.getPayload();
                startTime = System.nanoTime();
                boolean succ = sendEncStuff(encStuff, this.SEND_ENC_STUFF_TO_CARD);
                if(!succ)
                {
                    Log.e(tag, "Encrypted stuff sending to the card failed");
                    //return false;
                }
                else
                {
                    Log.d(tag, "Encrypted stuff sent to the card successfully");
                }
                elapsedTime = System.nanoTime() - startTime;
                Log.i(tag, "Time for sending E(KPbc, nc||Idc||nr||Idr) to SE: " + elapsedTime/1000000);
                break;
            case ((byte) 0x34):
                /** Send conf */
                startTime = System.nanoTime();
                byte[] nextMessageToBeSent = readFinalConf();
                elapsedTime = System.nanoTime() - startTime;
                Log.i(tag, "Time for retrieving E(KPbr, Ks||nr||IDr) from SE: " + elapsedTime/1000000);
                Log.d(tag, "Confirmation received from the card: " +
                        Utils.ByteArrayToHexString(nextMessageToBeSent));

                Log.d(tag, "Length of the confirmation received from the card: " +
                        nextMessageToBeSent.length);
                res.setPayload(nextMessageToBeSent);
                Log.d(tag, "Mutual Authentication done");
                break;
        }

        //Log.d(tag,"Final Response to Command : "+ new String(res.getPayload()));
        Log.d(tag,"Final Response to Command : " +
                Utils.ByteArrayToHexString(res.getResponseBytes()));
        return res;
    }

    boolean sendEncStuff(byte[] data, byte code)
    {
        byte[] sendingEncStuff = new byte[133];
        int j=0;
        sendingEncStuff[j++] = (byte)0xA0;
        sendingEncStuff[j++] = code;
        sendingEncStuff[j++] = (byte)0x00;
        sendingEncStuff[j++] = (byte)0x00;
        sendingEncStuff[j++] = (byte)0x80;
        System.arraycopy(data, 0, sendingEncStuff, j, data.length);
        int len = obj.SDSCTransmitEx(phDevice, sendingEncStuff, 0, sendingEncStuff.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        if(Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
            return true;
        if(Arrays.equals(Arrays.copyOf(bOutData, len), CERT_FAILED))
            Log.e(tag, "Data verification failed");
        else
            Log.e(tag, "Error in data verification sending");
        return false;
    }

    byte[] readFinalConf()
    {
        int len = obj.SDSCTransmitEx(phDevice, askFinalConf, 0, askFinalConf.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        if(!Arrays.equals(Arrays.copyOfRange(bOutData, len-2, len), Response.RESPONSE_SUCCESS_DONE))
        {
            Log.e(tag, "Error in reading final conf from the card");
            Log.e(tag, "Error code: " +
                    Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len-2, len)));
        }
        byte[] ret = Arrays.copyOf(bOutData, len-2);
        return ret;
    }
    byte[] readEncStuff()
    {
        int len = obj.SDSCTransmitEx(phDevice, askEncStuff, 0, askEncStuff.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        byte[] ret = Arrays.copyOf(bOutData, len-2);
        Log.d(tag, "Enc stuff read from the SE with status code: " +
                Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len-2,len)));
        return ret;
    }

    private byte[] getModulusAPDU(PublicKey key) {
        byte[] mod = key.getEncoded();
        byte[] buf = new byte[5+128];
        int j = 0;
        buf[j++] = (byte)0x80; buf[j++] = (byte)0x30;
        buf[j++] = (byte)0x00; buf[j++] = (byte)0x00; buf[j++] = (byte)0x95;
        /**
         * This following loop is just to fill the place of the error check bytes with 0s
         */
        while(j<26)
        {
            buf[j++] = (byte)0x00;
        }

        for(int i = 29; i < 128+29; i++) {
            buf[j++] = mod[i];
        }
        return buf;
    }

    private byte[] generateRandomAPDU() {
        byte[] APDU = new byte[5+8];
        SecureRandom sr = new SecureRandom();
        n1 = new byte[8];
        int i = 0;
        APDU[i++] = (byte)0x80; APDU[i++] = (byte)0x31; APDU[i++] = 0x00;
        APDU[i++] = 0x00; APDU[i++] = 0x08;
        sr.nextBytes(n1);
        System.arraycopy(n1, 0, APDU, i, n1.length);
        return APDU;
    }

    private byte[] createEncryptedAPDU(PublicKey pkey) throws Exception {
        byte[] temp = new byte[8+16];
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.ENCRYPT_MODE, pkey);

        /* n1n2 is being used here, but we didn't set the value of n1n2 */
        //System.arraycopy(n1n2, 8, temp, 0, 8);

        System.arraycopy(aes_key, 0, temp, 8, 16);
        byte[] encrypted = c.doFinal(temp);
        byte[] APDU = new byte[encrypted.length+5];
        APDU[0] = (byte)0xA0; APDU[1] = (byte)0xB4; APDU[2] = 0x00; APDU[3] = 0x00;
        APDU[4] = (byte)encrypted.length;
        System.arraycopy(encrypted, 0, APDU, 5, 128);
        return APDU;
    }

    private byte[] decryptAES(byte[] data) throws Exception {
        SecretKeySpec spec = new SecretKeySpec(aes_key, "AES");
        Cipher c = Cipher.getInstance("AES/CBC/NoPadding");
        c.init(Cipher.DECRYPT_MODE, spec);
        byte[] temp = new byte[16];
        System.arraycopy(data, 0, temp, 0, 16);
        return c.doFinal(temp);
    }
}
