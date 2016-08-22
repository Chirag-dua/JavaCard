package com.example.rishabh.bluetoothcardasymm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.gotrust.sesdapi.SDSCException;
import com.gotrust.sesdapi.SESDAPI;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by rishabh on 8/4/16.
 */
public class MutualAuthenticationThread extends Thread {
    private final String tag = "MAThread";
    private final String TimeTag = "TimeTag";
    private final BluetoothAdapter adapter;
    private BluetoothDevice device;
    private BluetoothConnection bluetoothConnection;
    private TCPConnection tcpConnection;
    private final JavaCard javaCard;
    private BluetoothSocket socket;
    Handler uiHandler;

    //Mutual Authentication variables
    private byte[] selectAPDU = {0x00, (byte)0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x08, 0x02};

    //private byte[] getModulusAPDU = {(byte)0xA0, (byte)0xB1, (byte)0x00, (byte)0x00, (byte)0x80};
    //private byte[] getRandomAPDU = {(byte)0xA0, (byte)0xB3, (byte)0x10, (byte)0x00, (byte)0x80};
    //private byte[] testMessageAPDU = {(byte)0xA0, (byte)0xB8, (byte)0x00, (byte)0x00, (byte)0x10};
    private byte[] getRandsIdsFromSE = {(byte)0x80, (byte) 0x30, (byte)0x00, (byte)0x00, (byte)0x10};
    private byte[] getMacsFromSE = {(byte)0x80, (byte) 0x32, (byte)0x00, (byte)0x00, (byte)0x18};

    private byte[] getSignFromSE = {(byte)0xA0, (byte) 0xB5, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] getModulusIDFromSE = {(byte)0xA0, (byte) 0xB3, (byte)0x00,
            (byte) 0x00, (byte)0x88};
    private byte[] getSLocPFromSE = {(byte) 0xA0, (byte) 0xC7, (byte)0x00, (byte)0x00, (byte) 0x80};

    private byte[] askEncStuff = {(byte)0xA0, (byte) 0xC3, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] askFinalConf = {(byte)0xA0, (byte) 0xC4, (byte)0x00, (byte)0x00, (byte)0x80};

    private byte[] CERT_FAILED = {(byte)0x69, (byte)0x84};
    private byte ASK_CERT = (byte) 0x31;
    private byte SEND_CERT = (byte) 0x30;
    private byte SEND_CONF = (byte) 0x32;
    private byte ASK_CONF = (byte) 0x33;
    private byte SEND_ENC_STUFF_TO_CARD = (byte)0xC2;
    private byte SEND_LOCP = (byte) 0xC6;
    private byte EVERYTHING_OK = (byte) 0x01;

    private static final int DEFAULT_SIZE = 1024;

    /* This buffer is used for retrieving response from the Java Card */
    private byte[] bOutData = new byte[DEFAULT_SIZE];
    private final int option;
    private byte[] signCertr = null;
    private byte[] apduToBeSent = null;
    private String serverIp = null;

    /* TCP constants */
    private final byte GET_LIST = (byte) 0x05;
    private final byte OK_TO_CONNECT = (byte) 0x02;
    private final byte READ_ACK = (byte) 0x01;
    private final int portNo = 4444;
    private final int doctorPortNo = 4123;
    public static int MAX_SIZE_FILE = 1024;

    private final byte FILE_READ= (byte) 0x03;


    MutualAuthenticationThread(String address, int option, JavaCard javaCard, Handler handler) {
        uiHandler = handler;
        this.javaCard = javaCard;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.option = option;
        if(option == MainActivity.OPTION_BLUETOOTH) {
            device = adapter.getRemoteDevice(address);
        }
        else if(option == MainActivity.OPTION_TCP) {
            serverIp = address;
        }
    }

    public void waitTillBtEnable() {
        try {
            while (!adapter.isEnabled()) {
                Thread.sleep(10);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BluetoothSocket establishBluetoothConnection() {
        BluetoothSocket ret = null;
        try {
            ret = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("851506f2-e9c2-4fa0-9584-467c3c107122"));
        }
        catch(IOException ex) {
            Log.e(tag, "Unable to get BluetoothSocket from BluetoothDevice: " + ex.getMessage());
            ex.printStackTrace();
            return ret;
        }
        adapter.cancelDiscovery();
        try {
            Log.i(tag, "Connecting to server socket");
            ret.connect();
            Log.i(tag, "Connection Established");
        }
        catch(IOException ex) {
            try {
                Log.d(tag, "Connect fail: Trying fallback...");
                ret = (BluetoothSocket)device.getClass().getMethod("createInsecureRfcommSocket",
                        new Class[]{int.class}).invoke(device,1);
                ret.connect();
                Log.d(tag, "Connected in fallback");
            }
            catch(Exception e) {
                Log.e(tag, "Connection failed in fallback");
                try {
                    ret.close();
                }
                catch(IOException ee) {
                    Log.e(tag, "Socket close exception: " + ee.toString());
                    ret = null;
                }
            }
        }
        return ret;
    }

    public Response onReceiveCommandAsymm(Command command) throws Exception {
        //StringBuffer str = new StringBuffer();

        //Log.d(tag, " DATA : " + new String(command.getPayload()));
        Log.d(tag, " DATA : " + Utils.ByteArrayToHexString(command.getCommandArray()));

        Response res = new Response();
        res.setResponseCode(Response.RESPONSE_SUCCESS_DONE);
        byte[] errorCheckArr = new byte[21];
        for(int i=0;i<21;i++)
            errorCheckArr[i] = (byte)0x01;
        res.setErrorCheck(errorCheckArr);
        /*
        byte[] idc = {(byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x02,
                (byte) 0x02, (byte) 0x02, (byte) 0x02};
        */
        byte[] SLocP = null;
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
                    len = javaCard.SDSCTransmitEx(sendSignToSE, 0, sendSignToSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
                }
                catch(SDSCException e)
                {
                    Log.e(tag, "SDSCException: " + e.getMessage());
                    throw e;
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
                Log.i(TimeTag, "Time for sending signature to SE: " + elapsedTime/1000000);
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
                try {
                    len = javaCard.SDSCTransmitEx(sendKeyIDRToSE, 0, sendKeyIDRToSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
                }
                catch(SDSCException e) {
                    Log.e(tag, "SDSCException: " + e.getMessage());
                    throw e;
                }

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
                Log.i(TimeTag, "Time for sending key||ID to SE: " + elapsedTime/1000000);
                /** Sending LocP to the Patient SE */

                byte locP[] = new byte[1];
                locP[0] = (byte) 0x60;

                byte[] sendLocPToSE = new byte[6];
                j=0;
                sendLocPToSE[j++] = (byte) 0xA0;
                sendLocPToSE[j++] = SEND_LOCP;
                sendLocPToSE[j++] = (byte) 0x00;
                sendLocPToSE[j++] = (byte) 0x00;
                sendLocPToSE[j++] = (byte) 0x01;
                System.arraycopy(locP, 0, sendLocPToSE, j, locP.length);
                Log.d(tag, "LocP being sent to the card");
                len = 0;
                try {
                    len = javaCard.SDSCTransmitEx(sendLocPToSE, 0, sendLocPToSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
                }
                catch(SDSCException e) {
                    Log.e(tag, "SDSCException occured: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
                Log.d(tag, "Length of the received response from card: " + len);
                if(!Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
                {
                    Log.e(tag, "Error in sending LocP to the card");
                    Log.e(tag, "Status code received: " +
                            Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len - 2, len)));
                    res.setResponseCode(Response.RESPONSE_ERROR);
                    return res;
                }
                elapsedTime = System.nanoTime() - startTime;
                Log.i(TimeTag, "Time for sending LocP to the SE: " + elapsedTime / 1000000);



                try
                {
                    byte[] signC = new byte[128 + 2];
                    startTime = System.nanoTime();
                    len = javaCard.SDSCTransmitEx(getSignFromSE, 0, getSignFromSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, signC);

                    Log.d(tag, "Signature received from the card: " +
                            Utils.ByteArrayToHexString(Arrays.copyOf(signC,len-2)));
                    Log.d(tag, "Length of the received signature: " + len);
                    elapsedTime = System.nanoTime() - startTime;
                    Log.i(TimeTag, "Time for retrieving signC from the SE: " + elapsedTime/1000000);
                    byte[] message = new byte[136 + 2];
                    startTime = System.nanoTime();
                    int lenMod = javaCard.SDSCTransmitEx(getModulusIDFromSE, 0, getModulusIDFromSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, message);

                    Log.d(tag, "KeyID received from the card: " +
                            Utils.ByteArrayToHexString(Arrays.copyOf(message,lenMod-2)));
                    elapsedTime = System.nanoTime() - startTime;
                    Log.i(TimeTag, "Time for retrieving Key||ID from the SE: " + elapsedTime / 1000000);
                    Log.d(tag, "Length of the received key || id: " + lenMod);
                    startTime = System.nanoTime();
                    byte[] nextMessageToBeSent = readEncStuff();
                    Log.d(tag, "Enc stuff received from the card: " +
                            Utils.ByteArrayToHexString(nextMessageToBeSent));
                    Log.d(tag, "Length of enc Stuff received from the card: " +
                            nextMessageToBeSent.length);
                    elapsedTime = System.nanoTime() - startTime;
                    Log.i(TimeTag, "Time for retrieving E(Pbr, nc||IDc) from SE: " + elapsedTime/1000000);
                    apduToBeSent = Utils.ConcatArrays(Arrays.copyOf(signC,len-2),
                            Arrays.copyOf(message,lenMod-2), nextMessageToBeSent);
                    Log.d(tag, "CERT || EncStuff: " + Utils.ByteArrayToHexString(apduToBeSent));

                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    throw e;
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
                Log.i(TimeTag, "Time for sending E(KPbc, nc||Idc||nr||Idr) to SE: " + elapsedTime/1000000);
                break;
            case ((byte) 0x34):
                /** Send conf */
                startTime = System.nanoTime();
                byte[] nextMessageToBeSent = readFinalConf();
                elapsedTime = System.nanoTime() - startTime;
                Log.i(TimeTag, "Time for retrieving E(KPbr, Ks||nr||IDr) from SE: " + elapsedTime/1000000);
                Log.d(tag, "Confirmation received from the card: " +
                        Utils.ByteArrayToHexString(nextMessageToBeSent));

                Log.d(tag, "Length of the confirmation received from the card: " +
                        nextMessageToBeSent.length);
                res.setPayload(nextMessageToBeSent);
                res.setResponseCode(Response.RESPONSE_STOP_PROCESS);
                Log.d(tag, "Mutual Authentication done");
                break;
            case ((byte)0x35) :
                /** Read SLocP from the SE */
                startTime = System.nanoTime();
                len = 0;
                SLocP = new byte[128+2];
                try
                {
                    len = javaCard.SDSCTransmitEx(getSLocPFromSE, 0, getSLocPFromSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, SLocP);
                }
                catch(SDSCException e)
                {
                    Log.e(tag, "SDSCException occured: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
                elapsedTime = System.nanoTime() - startTime;
                Log.i(TimeTag, "Time for retrieving SLocP from the SE: " + elapsedTime / 1000000);
                //str.append("Time for retrieving signature from the SE: " + elapsedTime / 1000000 + "\n");
                Log.d(tag, "SLocP received from the card");
                Log.d(tag, "Length of the received SLocP: " + len);
                /** Send SLocP to the reader */
                Log.d(tag, "Sending SLocP to the reader");
                res.setPayload(SLocP);
                break;
            case ((byte)0x36) :
                /** Receive the Location server response from the reader */
                Log.d(tag, "Receiving the Location server response from the reader");
                byte[] locationResponse = command.getPayload();
                if(locationResponse[0] != EVERYTHING_OK) {
                    Log.e(tag, "Everything is not okay in location server response");
                    res.setResponseCode(Response.RESPONSE_ERROR);
                    return res;
                }
                break;
        }

        //Log.d(tag,"Final Response to Command : "+ new String(res.getPayload()));
        Log.d(tag, "Final Response to Command : " +
                Utils.ByteArrayToHexString(res.getResponseBytes()));
        return res;
    }

    byte[] readFinalConf()
    {
        int len = javaCard.SDSCTransmitEx(askFinalConf, 0, askFinalConf.length,
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
        int len = javaCard.SDSCTransmitEx(sendingEncStuff, 0, sendingEncStuff.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        if(Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
            return true;
        if(Arrays.equals(Arrays.copyOf(bOutData, len), CERT_FAILED))
            Log.e(tag, "Data verification failed");
        else
            Log.e(tag, "Error in data verification sending");
        return false;
    }
    byte[] readEncStuff()
    {
        int len = javaCard.SDSCTransmitEx(askEncStuff, 0, askEncStuff.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        byte[] ret = Arrays.copyOf(bOutData, len-2);
        Log.d(tag, "Enc stuff read from the SE with status code: " +
                Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len - 2, len)));
        return ret;
    }
    public Response onReceiveCommand(Command command) {
        long startTime, elapsedTime;
        int len;
        //Log.d(tag, " DATA : " + new String(command.getPayload()));
        Log.d(tag, "DATA array: " + Utils.ByteArrayToHexString(command.getCommandArray()));
        Log.d(tag, "DATA payload: " + Utils.ByteArrayToHexString(command.getPayload()));

        Response res = new Response();
        res.setResponseCode(Response.RESPONSE_SUCCESS_DONE);
        byte[] errorCheckArr = new byte[21];
        for(int i=0;i<21;i++)
            errorCheckArr[i] = (byte)0x01;
        res.setErrorCheck(errorCheckArr);

        switch (command.getIns()) {
            case ((byte) 0x30):
                try {
                    /** Send the rands || ids */
                    startTime = System.nanoTime();
                    byte[] randsids = new byte[16 + 2];
                    len = 0;
                    try {
                        len = javaCard.SDSCTransmitEx(getRandsIdsFromSE, 0, getRandsIdsFromSE.length,
                                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, randsids);
                    } catch (SDSCException e) {
                        Log.e(tag, "SDSCException: " + e.getMessage());
                        throw e;
                    }
                    elapsedTime = System.nanoTime() - startTime;
                    Log.i(tag, "Time for retrieving RANDS||IDS from the SE: " + elapsedTime / 1000000);
                    Log.d(tag, "Rands||IDs received from the card: " +
                            Utils.ByteArrayToHexString(Arrays.copyOf(randsids, len - 2)));
                    Log.d(tag, "Length of the received data: " + len);
                    //Log.d(tag, "Receive signature of certr");
                    res.setPayload(Arrays.copyOf(randsids, len - 2));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case ((byte) 0x31):
                try {
                    Log.d(tag, "Receiving macp from the reader app");

                    byte[] rrmi = command.getPayload();
                    Log.d(tag, "rrmi: " + Utils.ByteArrayToHexString(rrmi));
                    Log.d(tag, "Length of rrmi: " + rrmi.length);
                    startTime = System.nanoTime();
                    byte[] dataToSE = new byte[5 + 40];
                    int j = 0;
                    dataToSE[j++] = (byte) 0x80;
                    dataToSE[j++] = (byte) 0x31;
                    dataToSE[j++] = (byte) 0x00;
                    dataToSE[j++] = (byte) 0x00;
                    dataToSE[j++] = (byte) 0x28;
                    System.arraycopy(rrmi, 0, dataToSE, j, rrmi.length);
                    len = javaCard.SDSCTransmitEx(dataToSE, 0, dataToSE.length,
                            SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);

                    if (Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE)) {
                        Log.d(tag, "Macp verification successful");
                    } else {
                        if (Arrays.equals(Arrays.copyOf(bOutData, len), CERT_FAILED))
                            Log.e(tag, "Macp verification failed");
                        else
                            Log.e(tag, "Error in macp sending");
                        res.setResponseCode(Response.RESPONSE_ERROR);
                        return res;
                    }
                    elapsedTime = System.nanoTime() - startTime;
                    Log.i(tag, "Time for sending RAND_S || RAND_P || MAC_P || ID_P to SE: " +
                            elapsedTime / 1000000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case ((byte) 0x32):
                try {
                    Log.d(tag, "Send macs");
                    len = 0;
                    startTime = System.nanoTime();
                    try {
                        len = javaCard.SDSCTransmitEx(getMacsFromSE, 0, getMacsFromSE.length,
                                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
                    } catch (SDSCException e) {
                        Log.e(tag, "SDSCException: " + e.getMessage());
                    }
                    elapsedTime = System.nanoTime() - startTime;
                    Log.i(tag, "Time for retrieving RAND_S || MAC_S from the card: " + elapsedTime / 1000000);
                    Log.d(tag, "Rands||MAC_s received from the card: " +
                            Utils.ByteArrayToHexString(Arrays.copyOf(bOutData, len - 2)));
                    Log.d(tag, "Length of the received data: " + len);
                    //Log.d(tag, "Receive signature of certr");
                    res.setPayload(Arrays.copyOf(bOutData, len - 2));
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case ((byte) 0x33):
                res.setResponseCode(Response.RESPONSE_STOP_PROCESS);
        }

        //Log.d(tag,"Final Response to Command : "+ new String(res.getPayload()));
        Log.d(tag, "Final Response to Command : " +
                Utils.ByteArrayToHexString(res.getResponseBytes()));

        return res;
    }

    boolean mutualAuthentication() {
        //TODO: Add progress bar
        javaCard.selectDeviceAndApplet();
        while(true) {
            byte[] inData = null;
            if(option == MainActivity.OPTION_BLUETOOTH) {
                inData = bluetoothConnection.read();
            }
            else if(option == MainActivity.OPTION_TCP)
                inData = tcpConnection.read();
            if(inData == null) {
                Log.e(tag, "Error in reading");
                return false;
            }
            Log.d(tag, "inData received from the reader: " +
                    Utils.ByteArrayToHexString(inData));
                Command command = new Command(inData);
            Response res = null;
            try {
                res = onReceiveCommandAsymm(command);
            }
            catch(Exception e) {
                Log.e(tag, "Getting a response for this command failed");
                e.printStackTrace();
                return false;
            }
            if(Arrays.equals(res.getResponseCode(), Response.RESPONSE_STOP_PROCESS)) {
                if(option == MainActivity.OPTION_BLUETOOTH)
                    bluetoothConnection.write(res.getResponseBytes());
                else
                    tcpConnection.write(res.getResponseBytes());
                Log.i(tag, "Mutual Authentication successful");
                return true;
            }
            if(!(Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))) {
                Log.i(tag, "Mutual Authentication unsuccessful");
                return false;
            }
            if(option == MainActivity.OPTION_BLUETOOTH)
                bluetoothConnection.write(res.getResponseBytes());
            else
                tcpConnection.write(res.getResponseBytes());
        }
    }

    Socket establishTCPConnection() {
        Socket commSocket = null;
        try {
            InetAddress serverAddress = InetAddress.getByName(serverIp);
            commSocket = new Socket(serverAddress, portNo);
        }
        catch(IOException ex) {
            Log.e(tag, "Error in establishing TCP Connection: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
        int bytes;
        byte[] buffer = new byte[DEFAULT_SIZE];
        buffer[0] = GET_LIST;
        OutputStream out=null;
        InputStream in = null;
        try {
            in = commSocket.getInputStream();
            out = commSocket.getOutputStream();
        }
        catch(IOException ex) {
            Log.e(tag, "Cannot get the streams: " + ex.getMessage());
            return null;
        }
        try {
            out.write(buffer, 0, 1);
            out.flush();
        }
        catch(IOException ex) {
            Log.e(tag, "Error in writing to the server");
            ex.printStackTrace();
            return null;
        }
        while(true) {
            try {
                bytes = in.read(buffer);
                break;
            } catch (IOException ex) {
                Log.e(tag, "Error in reading: " + ex.getMessage());
            }
        }
        if(bytes != 1) {
            Log.e(tag, "Error in response received from the server");
            Log.e(tag, "Response received: " + bytes);
            return null;
        }
        int noOfConnectedServers = buffer[0];
        String[] IPAddress = new String[noOfConnectedServers];
        for(int i=0;i<noOfConnectedServers;i++) {
            try {
                bytes = in.read(buffer);
            }
            catch(IOException ex) {
                Log.e(tag, "Error in reading: " + ex.getMessage());
            }
            IPAddress[i] = new String(Arrays.copyOf(buffer,bytes));
            Log.d(tag, "Received address: " + IPAddress[i]);
            buffer[0] = READ_ACK;
            try {
                out.write(buffer, 0, 1);
                out.flush();
            }
            catch(IOException ex) {
                Log.e(tag, "Error in sending acknowledgement: " + ex.getMessage());
                ex.printStackTrace();
                break;
            }
        }
        // We are choosing the first IP Address as the desired one
        Log.d(tag, "Sending the index of the chosen doctor");
        buffer[0] = 0x00;
        try {
            out.write(buffer, 0, 1);
            out.flush();
        }
        catch(IOException ex) {
            Log.e(tag, "Error in writing to the server: " + ex.getMessage());
            return null;
        }
        while(true) {
            try {
                bytes = in.read(buffer);
                break;
            }
            catch (IOException ex) {
                Log.e(tag, "Error in reading: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        if(buffer[0] != OK_TO_CONNECT) {
            Log.e(tag, "Not okay to connect");
            Log.e(tag, "Response received: " + buffer[0]);
            Log.e(tag, "Length of the response received: " + bytes);
            return null;
        }
        Log.d(tag, "Received OK_TO_CONNECT");
        /*
        try {
            Thread.sleep(100);
        }
        catch(InterruptedException ex) {
            Log.e(tag, "Interrupted exception : " + ex.getMessage());
        }
        */
        Log.d(tag, "Length of the IP address: " + IPAddress[0].length());
        //String ipAddress = IPAddress[0].substring(1,14);
        String[] tempString = IPAddress[0].split("[/:]");
        String ipAddress = tempString[1];
        Log.d(tag, "Length of the substring: " + ipAddress.length());
        Log.d(tag, "Substring: " + ipAddress);
        Socket mSocket = null;
        try {
            InetAddress doctorAddress = InetAddress.getByName(ipAddress);
            mSocket = new Socket(doctorAddress, doctorPortNo);
        }
        catch(IOException ex) {
            Log.e(tag, "Error in connecting with the doctor: " + ex.getMessage());
            return null;
        }
        return mSocket;
    }

    @Override
    public void run() {
        super.run();
        Log.d(tag, "run on Mutual Authentication thread");
        if(option == MainActivity.OPTION_BLUETOOTH) {
            if(adapter == null) {
                Log.e(tag, "Bluetooth not supported");
                return;
            }
            if (!adapter.isEnabled()) {
                adapter.enable();
                waitTillBtEnable();
            }
            socket = establishBluetoothConnection();
            if(socket == null) {
                Log.e(tag, "Bluetooth connection could not be established");
                return;
            }
            Log.i(tag, "Bluetooth connection successful");
            bluetoothConnection = new BluetoothConnection(socket);
            /*
            long startTime = System.nanoTime();
            boolean success = mutualAuthentication();
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Total time taken for mutual authentication: " + elapsedTime/1000000);
            */

            File dir,file;
            if(MainActivity.isExternalStorageWritable()) {
                dir = MainActivity.getTextStorageDir(MainActivity.dirName);
                file = new File(dir, MainActivity.fileName);
                FileOutputStream fout = null;

                //Read file length
                int fileSize;
                byte[] fileLength = bluetoothConnection.read();
                if(fileLength == null) {
                    Log.e(tag, "Error in file length");
                    fileSize=0;
                }
                else {
                    fileSize = Utils.byteArrayToInt(fileLength);
                }

                MAX_SIZE_FILE = Math.max(1024,fileSize);

                //Read the file from the connection
                try {
                    fout = new FileOutputStream(file);
                    byte[] readData;
                    int bytes,totalRead=0;
                    while(totalRead < fileSize) {
                        readData = bluetoothConnection.read();
                        if(readData == null) {
                            Log.e(tag, "readData is null");
                            break;
                        }
                        totalRead += readData.length;
                        fout.write(readData,0,readData.length);
                        fout.flush();
                    }
                    fout.close();
                    bluetoothConnection.write(new byte[]{FILE_READ});
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                MAX_SIZE_FILE=1024;
            }
            else {
                Log.e(tag, "External storage not writeable");
            }

            cancel();
        }
        else if(option == MainActivity.OPTION_TCP) {
            Socket tcpSocket = establishTCPConnection();
            if(tcpSocket == null) {
                Log.e(tag, "TCP connection could not be establised");
                return;
            }
            Log.i(tag, "TCP connection successful");
            tcpConnection = new TCPConnection(tcpSocket);


            File dir,file;
            if(MainActivity.isExternalStorageWritable()) {
                dir = MainActivity.getTextStorageDir(MainActivity.dirName);
                file = new File(dir, MainActivity.fileName);
                FileOutputStream fout = null;
                //BufferedReader brin = new BufferedReader(new InputStreamReader(tcpConnection.in));
                //BufferedWriter frout = null;
                /*
                    int len;
                    while (true) {
                        len = 0;
                        try {
                            Log.d(tag, "About to begin reading");
                            len = brin.read(readData, 0, 5000);
                            Log.d(tag, "Reading done");
                            Log.d(tag, "Len: " + len);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            break;
                        }
                        if (len == -1) break;
                        Log.d(tag, "Data read length: " + len);
                        if (len > 0)
                            frout.write(readData, 0, len);
                    }
                    //brin.close();
                    tcpConnection.write(new byte[]{FILE_READ});
                    frout.close();
                    */

                //Read file length
                int fileSize;
                byte[] fileLength = tcpConnection.read();
                if(fileLength == null) {
                    Log.e(tag, "Error in file length");
                    fileSize=0;
                }
                else {
                    fileSize = Utils.byteArrayToInt(fileLength);
                }

                MAX_SIZE_FILE = fileSize;

                //Read the file from the connection
                try {
                    //frout = new BufferedWriter(new FileWriter(file));
                    //String readData = null;
                    //char[] readData = new char[5000];
                    fout = new FileOutputStream(file);
                    byte[] readData;
                    int bytes,totalRead=0;
                    while(totalRead < fileSize) {
                        readData = tcpConnection.read();
                        if(readData == null) {
                            Log.e(tag, "readData is null");
                            break;
                        }
                        totalRead += readData.length;
                        fout.write(readData,0,readData.length);
                        fout.flush();
                    }
                    fout.close();
                    tcpConnection.write(new byte[]{FILE_READ});
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            else {
                Log.e(tag, "External storage not writeable");
            }
            //uiHandler.obtainMessage(ResultActivity.FILE_TRANSFER, tcpConnection).sendToTarget();
            cancel();
        }
    }

    void cancel() {
        if(bluetoothConnection != null) {
            bluetoothConnection.cancel();
        }
        if(tcpConnection != null) {
            tcpConnection.cancel();
        }
    }
}
