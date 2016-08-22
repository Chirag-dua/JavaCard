package com.example.rishabh.bluetoothreaderasymm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.nfc.tech.IsoDep;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.gotrust.sesdapi.SDSCException;
import com.gotrust.sesdapi.SESDAPI;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by rishabh on 4/4/16.
 */
public class MutualAuthenticationThread extends Thread {
    private static final int OPTION_BLUETOOTH = 0;
    private static final int OPTION_WIFI = 1;
    private static final String tag = "MAThread";
    private static final String TimeTag = "Time";
    private final BluetoothAdapter adapter;
    private TCPConnection tcpConnection;
    private final int option;

    JavaCard javaCard;

    private static final int DEFAULT_SIZE = 1024;

    /* This buffer is used for retrieving response from the Java Card */
    private byte[] bOutData = new byte[DEFAULT_SIZE];

    /* Mutual Authentication constants*/

    private byte[] getRandpmacp = {(byte)0x80, (byte)0x31, (byte) 0x00, (byte) 0x00, (byte)0x28};

    private byte[] selectAPDU = {0x00, (byte)0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03,
            0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x08, 0x01};

    private byte[] getSignFromSE = {(byte)0xA0, (byte) 0xB5, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] getModulusIDFromSE = {(byte)0xA0, (byte) 0xB3, (byte)0x00,
            (byte) 0x00, (byte)0x88};
    private byte[] getSLOCMFromSE = {(byte) 0xA0, (byte) 0xC7, (byte)0x00, (byte)0x00, (byte)0x80};

    private byte[] verifyCertFromSE = new byte[133];
    private byte[] askEncStuff = {(byte)0xA0, (byte) 0xC3, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] aes_key = {0x01, (byte)0xe5, 0x66, 0x42, 0x6a, 0x10, (byte)0xca, 0x21, 0x7e,
            (byte) 0x83, 0x26, (byte)0xb4, 0x37, (byte)0xa8, (byte)0xc9, (byte)0xf4};
    private byte ASK_RANDS = (byte) 0x30;
    private byte SEND_MACP = (byte) 0x31;
    private byte ASK_MACS = (byte) 0x32;
    private byte SEND_FINAL_CONF = (byte) 0x33;

    private byte ASK_CERT = (byte) 0x32;
    private byte SEND_SIGN = (byte) 0x30;
    private byte SEND_KEYID = (byte) 0x31;
    private byte SEND_CONF = (byte) 0x33;
    private byte ASK_CONF = (byte) 0x34;
    private byte ASK_SLOCP = (byte) 0x35;
    private byte SEND_LOC_RESPONSE = (byte)0x36;

    /* For interaction with SE */
    private byte SEND_LOCM = (byte) 0xC6;
    private byte SEND_FINAL_CONF_CARD = (byte)0xC4;
    private byte SEND_ENC_STUFF_TO_CARD = (byte)0xC2;

    private byte[] CERT_FAILED = {(byte)0x69, (byte)0x84};
    private byte EVERYTHING_OK = (byte)0x01;

    Response response = null;

    BluetoothConnection bluetoothConnection;
    TCPConnection serverTCPConnection = null;
    BluetoothSocket socket;
    Handler uiHandler;

    //TCP Constants
    private final int portNo = 4445;
    private final int doctorPortNo = 4123;
    private String serverIp = null;
    private String locationServerAddress = null;
    private final byte MAKE_SERVER = (byte) 0x03;
    private final byte OK_TO_CONNECT = (byte) 0x02;
    private final byte FILE_READ= (byte) 0x03;

    MutualAuthenticationThread(String locationAddress, String address, int option,
                               JavaCard javaCard,
                               Handler handler) {
        uiHandler = handler;
        //see what it needs here
        locationServerAddress = locationAddress;
        Log.d(tag, "Location server address: " + locationServerAddress);
        serverIp = address;
        this.javaCard = javaCard;
        this.option = option;
        adapter = BluetoothAdapter.getDefaultAdapter();
        Log.d(tag, "MA thread created");
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

    void setCommandForRead(Command cmd, byte code)
    {
        cmd.setCla((byte) 0x80);
        cmd.setIns(code);
        cmd.setP1((byte) 0x00);
        cmd.setP2((byte) 0x00);
        byte[] errArr = new byte[21];
        for(int i=0;i<21;i++)
            errArr[i] = (byte)0x01;
        cmd.setErrorCheck(errArr);
        byte[] payArr = {(byte)0x00, (byte)0x00};
        //  cmd.setPayload("Not really needed for reading".getBytes());
        cmd.setPayload(payArr);
        cmd.setLength((byte) (cmd.getPayload().length + cmd.getErrorCheck().length));
        cmd.setRead();
    }


    Response sendData(Command commandAuth) {
        byte[] data = commandAuth.getCommandArray();
        if(option == OPTION_BLUETOOTH) {
            bluetoothConnection.write(data);
            byte[] res = bluetoothConnection.read();
            if (res == null) {
                Log.e(tag, "Error in reading");
                return (new Response());
            } else {
                return (new Response(res));
            }
        }
        else {
            tcpConnection.write(data);
            byte[] res = tcpConnection.read();
            if (res == null) {
                Log.e(tag, "Error in reading");
                return (new Response());
            } else {
                return (new Response(res));
            }
        }
    }

    boolean mutualAuthenticationAsymmWithLocation() {
        javaCard.selectDeviceAndApplet();
        /**
         *  Take certificate from card
         */
        try
        {
            long startTime = System.nanoTime();
            int len = 0;
            try
            {
                len = javaCard.SDSCTransmitEx(getSignFromSE, 0, getSignFromSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Time for retrieving signature from the SE: " + elapsedTime / 1000000);
            //str.append("Time for retrieving signature from the SE: " + elapsedTime / 1000000 + "\n");
            Log.d(tag, "Signature received from the card");
            Log.d(tag, "Length of the received signature: " + len);

            byte[] message = new byte[136 + 2];

            startTime = System.nanoTime();
            int lenMod = 0;
            try {
                lenMod = javaCard.SDSCTransmitEx(getModulusIDFromSE, 0, getModulusIDFromSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, message);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Time for retrieving keyr||IDr from the SE: " + elapsedTime / 1000000);
            //str.append("Time for retrieving keyr||IDr from the SE: " + elapsedTime / 1000000 + "\n");
            Log.d(tag, "Key received from the card");
            Log.d(tag, "Length of the received key || id: " + lenMod);

            /** Send this to Card */

            startTime = System.nanoTime();

            byte[] buf = new byte[128 + 5 + 21];
            int j = 0;
            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_SIGN;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x95;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(bOutData, 0, buf, j, len-2);
            Command commandAuth = new Command(buf);
            Log.d(tag, "Sending signature to the Card");
            //Response res = transactNfc(isoDep, commandAuth);
            Response res = sendData(commandAuth);
            Log.d(tag, "Response received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Signature sending to Card failed");
                return false;
            }
            else
            {
                Log.d(tag, "Signature successfully sent to the Card");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending signature to the Card: " + elapsedTime / 1000000);
            //str.append("Time for sending signature to the Card: " + elapsedTime / 1000000 + "\n");
            /** Signature sent to the Card */

            /** Sending key ID to the Card */

            startTime = System.nanoTime();

            j = 0;
            buf = new byte[136 + 5 + 21];

            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_KEYID;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x9D;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(message, 0, buf, j, lenMod-2);
            commandAuth = new Command(buf);
            Log.d(tag, "KeyID being sent to the card");
            Log.d(tag, "The command apdu send to Card: " + Utils.ByteArrayToHexString(buf));
            //res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Response for key received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Key sending failed");
                return false;
            }
            else
            {
                Log.d(tag, "Key successfully sent to the Card");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending Keyr || IDr to the Card: " + elapsedTime / 1000000);
            //str.append("Time for sending Keyr || IDr to the Card: " + elapsedTime / 1000000 + "\n");

            startTime = System.nanoTime();
            setCommandForRead(commandAuth, this.ASK_CERT);
            Log.d(tag, "Commmand set for reading");
            Log.d(tag, "Command being sent: " +
                    Utils.ByteArrayToHexString(commandAuth.getCommandArray()));

            //res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for receiving Certc || E(KPbr, nc||IDc) from Card: " + elapsedTime/1000000);
            //str.append("Time for receiving Certc || E(KPbr, nc||IDc) from Card: " +
                    //elapsedTime/1000000 + "\n");
            Log.d(tag, "Transaction of ASK_CERT request done");

            Log.d(tag, "Length of response payload: " + res.getPayload().length);
            Log.d(tag, "Response received: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));



            startTime = System.nanoTime();

            byte[] payload = res.getPayload();
            Log.d(tag, "Payload received: " + Utils.ByteArrayToHexString(payload));
            byte[] signC = Arrays.copyOfRange(payload, 0, 128);
            Log.d(tag, "SignC received from Card: " + Utils.ByteArrayToHexString(signC));
            byte[] keyCID = Arrays.copyOfRange(payload, 128, 264);
            Log.d(tag, "keyCID received from Card: " + Utils.ByteArrayToHexString(keyCID));
            byte[] encStuff = Arrays.copyOfRange(payload, 264, payload.length);
            Log.d(tag, "encStuff received from Card: " + Utils.ByteArrayToHexString(encStuff));

            boolean succ = verifyCert(signC, keyCID);
            if(!succ)
            {
                Log.e(tag, "Certificate verification failed");
                return false;
            }
            else
            {
                Log.d(tag, "Certificate verified");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending Certc to SE: " + elapsedTime/1000000);
            //str.append("Time for sending Certc to SE: " + elapsedTime / 1000000 + "\n");
            startTime = System.nanoTime();
            succ = sendEncStuff(encStuff, this.SEND_ENC_STUFF_TO_CARD);
            if(!succ)
            {
                Log.e(tag, "Encrypted stuff sending to the card failed");
                return false;
            }
            else
            {
                Log.d(tag, "Encrypted stuff sent to the card successfully");
            }

            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending E(KPbr, nc||IDc) to SE: " + elapsedTime / 1000000);
            //str.append("Time for sending E(KPbr, nc||IDc) to SE: " + elapsedTime/1000000 + "\n");

            /** Receive SLocP from Patient( Card ) */
            startTime = System.nanoTime();
            setCommandForRead(commandAuth, ASK_SLOCP);
            Log.d(tag, "Commmand set for reading");
            Log.d(tag, "Command being sent: " +
                    Utils.ByteArrayToHexString(commandAuth.getCommandArray()));
            res = sendData(commandAuth);
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for receiving SLocP from Patient: " + elapsedTime / 1000000);
            //str.append("Time for receiving SLocP from Patient: " + elapsedTime/1000000);
            Log.d(tag, "Transaction of ASK_SLocP request done");

            Log.d(tag, "Length of response payload: " + res.getPayload().length);
            Log.d(tag, "Response received: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));

            byte[] SLocP = res.getPayload();

            /** Sending LOCM to the SE */

            startTime = System.nanoTime();
            byte[] locm = new byte[1];
            locm[0]= (byte) 0x60;

            byte[] sendLocmToSE = new byte[6];
            j=0;
            sendLocmToSE[j++] = (byte) 0xA0;
            sendLocmToSE[j++] = SEND_LOCM;
            sendLocmToSE[j++] = (byte) 0x00;
            sendLocmToSE[j++] = (byte) 0x00;
            sendLocmToSE[j++] = (byte) 0x01;
            System.arraycopy(locm, 0, sendLocmToSE, j, locm.length);
            Log.d(tag, "LOCM being sent to the card");
            len = 0;
            try {
                len = javaCard.SDSCTransmitEx(sendLocmToSE, 0, sendLocmToSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            }
            catch(SDSCException e) {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            Log.d(tag, "Length of the received response from card: " + len);
            if(!Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Error in sending LocM to the card");
                Log.e(tag, "Status code received: " +
                        Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len-2, len)));
                return false;
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Time for sending LOCM to the SE: " + elapsedTime / 1000000);
            //str.append("Time for sending LOCM to the SE: " + elapsedTime/1000000);
            /** Asking for SLOCM */
            startTime = System.nanoTime();
            len = 0;
            byte[] SLocM = new byte[128+2];
            try
            {
                len = javaCard.SDSCTransmitEx(getSLOCMFromSE, 0, getSLOCMFromSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, SLocM);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Time for retrieving SLocM from the SE: " + elapsedTime / 1000000);
            //str.append("Time for retrieving signature from the SE: " + elapsedTime / 1000000 + "\n");
            Log.d(tag, "SLocM received from the card");
            Log.d(tag, "Length of the received SLocM: " + len);

            byte[] dataToSendToLocationServer = new byte[256];
            System.arraycopy(SLocP,0,dataToSendToLocationServer,0,SLocP.length-2);
            System.arraycopy(SLocM,0,dataToSendToLocationServer,SLocP.length-2,len-2);
            startTime = System.nanoTime();
            byte[] responseFromLocationServer = sendToLocationServer(dataToSendToLocationServer);
            elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Time for getting back response from the location server: " + elapsedTime / 1000000);
            //str.append("Time for getting back response from the location server: " + elapsedTime/1000000);
            Log.d(tag, "Response received from location server: " +
                    Utils.ByteArrayToHexString(responseFromLocationServer));
            // TODO: This should be sent to the card to be decrypted and the response should be checked
            if(responseFromLocationServer[0] != EVERYTHING_OK) {
                Log.e(tag, "Locations are not same");
                return false;
            }

            /** Sending the received response from the location server to the patient */

            startTime = System.nanoTime();

            buf = new byte[1 + 5 + 21];
            j = 0;
            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_LOC_RESPONSE;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x16;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(responseFromLocationServer, 1, buf, j, 1);
            commandAuth = new Command(buf);
            Log.d(tag, "Sending response from Location server to the Patient");
            //Response res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Response received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Location response sending to patient failed");
                return false;
            }
            else
            {
                Log.d(tag, "Location response successfully sent to the patient");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Time for sending location response to the patient: " + elapsedTime / 1000000);
            //str.append("Time for sending signature to the patient: " + elapsedTime / 1000000 + "\n");

            /** The protocol continues */
            startTime = System.nanoTime();
            byte[] nextMessageToBeSent = readEncStuff();
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for reading E(KPbc, nc||IDc||nr||IDr) from the SE: " + elapsedTime/1000000);
            //str.append("Time for reading E(KPbc, nc||IDc||nr||IDr) from the SE: " + elapsedTime / 1000000 + "\n");

            Log.d(tag, "Enc stuff received from the card: " +
                    Utils.ByteArrayToHexString(nextMessageToBeSent));

            Log.d(tag, "Length of the encrypted stuff received from the card: " +
                    nextMessageToBeSent.length);
            startTime = System.nanoTime();
            buf = new byte[128 + 5 + 21];
            j = 0;
            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_CONF;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x9A;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(nextMessageToBeSent, 0, buf, j, nextMessageToBeSent.length-2);
            commandAuth = new Command(buf);
            //res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Response received: " + Utils.ByteArrayToHexString(res.getResponseBytes()));
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Confirmation sending failed");
                return false;
            }
            else
            {
                Log.d(tag, "Confirmation sent successfully");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending E(KPbc, nc||IDc||nr||IDr) to the Card: " + elapsedTime / 1000000);
            //str.append("Time for sending E(KPbc, nc||IDc||nr||IDr) to the Card: " + elapsedTime / 1000000 + "\n");
            startTime = System.nanoTime();
            setCommandForRead(commandAuth, this.ASK_CONF);
            //res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for receiving E(KPbr, Ks||nr||IDr) from the Card: " + elapsedTime/1000000);
            //str.append("Time for receiving E(KPbr, Ks||nr||IDr) from the Card: " + elapsedTime / 1000000 + "\n");
            Log.d(tag, "Length of response payload received from the Card: " + res.getPayload().length);

            Log.d(tag, "Received confirmation from the Card: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));

            byte[] encSt = res.getPayload();
            startTime = System.nanoTime();
            succ = sendEncStuff(encSt, this.SEND_FINAL_CONF_CARD);
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending  E(KPbr, Ks||nr||IDr) to the SE: " + elapsedTime / 1000000);
            //str.append("Time for sending  E(KPbr, Ks||nr||IDr) to the SE: " + elapsedTime / 1000000 + "\n");
            if(!succ)
            {
                Log.e(tag, "Final confirmation sending to the card failed");
                return false;
            }
            else
            {
                Log.d(tag, "Final confirmation sent to the card successfully");
            }
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    boolean mutualAuthenticationAsymmWithoutLocation()
    {
        javaCard.selectDeviceAndApplet();
        /**
         *  Take certificate from card
         */
        try
        {
            long startTime = System.nanoTime();
            int len = 0;
            try
            {
                len = javaCard.SDSCTransmitEx(getSignFromSE, 0, getSignFromSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Time for retrieving signature from the SE: " + elapsedTime / 1000000);
            //str.append("Time for retrieving signature from the SE: " + elapsedTime / 1000000 + "\n");
            Log.d(tag, "Signature received from the card");
            Log.d(tag, "Length of the received signature: " + len);

            byte[] message = new byte[136 + 2];

            startTime = System.nanoTime();
            int lenMod = 0;
            try {
                lenMod = javaCard.SDSCTransmitEx(getModulusIDFromSE, 0, getModulusIDFromSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, message);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Time for retrieving keyr||IDr from the SE: " + elapsedTime / 1000000);
            //str.append("Time for retrieving keyr||IDr from the SE: " + elapsedTime / 1000000 + "\n");
            Log.d(tag, "Key received from the card");
            Log.d(tag, "Length of the received key || id: " + lenMod);

            /** Send this to Card */

            startTime = System.nanoTime();

            byte[] buf = new byte[128 + 5 + 21];
            int j = 0;
            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_SIGN;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x95;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(bOutData, 0, buf, j, len-2);
            Command commandAuth = new Command(buf);
            Log.d(tag, "Sending signature to the Card");
            //Response res = transactNfc(isoDep, commandAuth);
            Response res = sendData(commandAuth);
            Log.d(tag, "Response received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Signature sending to Card failed");
                return false;
            }
            else
            {
                Log.d(tag, "Signature successfully sent to the Card");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending signature to the Card: " + elapsedTime / 1000000);
            //str.append("Time for sending signature to the Card: " + elapsedTime / 1000000 + "\n");
            /** Signature sent to the Card */

            /** Sending key ID to the Card */

            startTime = System.nanoTime();

            j = 0;
            buf = new byte[136 + 5 + 21];

            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_KEYID;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x9D;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(message, 0, buf, j, lenMod-2);
            commandAuth = new Command(buf);
            Log.d(tag, "KeyID being sent to the card");
            Log.d(tag, "The command apdu send to Card: " + Utils.ByteArrayToHexString(buf));
            //res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Response for key received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Key sending failed");
                return false;
            }
            else
            {
                Log.d(tag, "Key successfully sent to the Card");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending Keyr || IDr to the Card: " + elapsedTime / 1000000);
            //str.append("Time for sending Keyr || IDr to the Card: " + elapsedTime/1000000 + "\n");

            startTime = System.nanoTime();
            setCommandForRead(commandAuth, this.ASK_CERT);
            Log.d(tag, "Commmand set for reading");
            Log.d(tag, "Command being sent: " +
                    Utils.ByteArrayToHexString(commandAuth.getCommandArray()));

            //res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for receiving Certc || E(KPbr, nc||IDc) from Card: " + elapsedTime/1000000);

            //str.append("Time for receiving Certc || E(KPbr, nc||IDc) from Card: " +
                    //elapsedTime/1000000 + "\n");

            Log.d(tag, "Transaction of ASK_CERT request done");

            Log.d(tag, "Length of response payload: " + res.getPayload().length);
            Log.d(tag, "Response received: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));



            startTime = System.nanoTime();

            byte[] payload = res.getPayload();
            Log.d(tag, "Payload received: " + Utils.ByteArrayToHexString(payload));
            byte[] signC = Arrays.copyOfRange(payload, 0, 128);
            Log.d(tag, "SignC received from Card: " + Utils.ByteArrayToHexString(signC));
            byte[] keyCID = Arrays.copyOfRange(payload, 128, 264);
            Log.d(tag, "keyCID received from Card: " + Utils.ByteArrayToHexString(keyCID));
            byte[] encStuff = Arrays.copyOfRange(payload, 264, payload.length);
            Log.d(tag, "encStuff received from Card: " + Utils.ByteArrayToHexString(encStuff));

            boolean succ = verifyCert(signC, keyCID);
            if(!succ)
            {
                Log.e(tag, "Certificate verification failed");
                return false;
            }
            else
            {
                Log.d(tag, "Certificate verified");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending Certc to SE: " + elapsedTime/1000000);
            //str.append("Time for sending Certc to SE: " + elapsedTime/1000000 + "\n");
            startTime = System.nanoTime();
            succ = sendEncStuff(encStuff, this.SEND_ENC_STUFF_TO_CARD);
            if(!succ)
            {
                Log.e(tag, "Encrypted stuff sending to the card failed");
                return false;
            }
            else
            {
                Log.d(tag, "Encrypted stuff sent to the card successfully");
            }

            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending E(KPbr, nc||IDc) to SE: " + elapsedTime / 1000000);
            //str.append("Time for sending E(KPbr, nc||IDc) to SE: " + elapsedTime/1000000 + "\n");

            /** Receive SLocP from Patient( Card ) */
            /*
            startTime = System.nanoTime();
            setCommandForRead(commandAuth, ASK_SLOCP);
            Log.d(tag, "Commmand set for reading");
            Log.d(tag, "Command being sent: " +
                    Utils.ByteArrayToHexString(commandAuth.getCommandArray()));
            res = sendData(commandAuth);
            elapsedTime = System.nanoTime() - startTime;
            Log.d(tag, "Time for receiving SLocP from Patient: " + elapsedTime/1000000);
            Log.d(tag, "Transaction of ASK_SLocP request done");

            Log.d(tag, "Length of response payload: " + res.getPayload().length);
            Log.d(tag, "Response received: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));

            byte[] SLocP = res.getPayload();
            */
            /** Sending LOCM to the SE */
        /*
            startTime = System.nanoTime();
            byte[] locm = new byte[1];
            locm[0]= (byte) 0x60;

            byte[] sendLocmToSE = new byte[6];
            j=0;
            sendLocmToSE[j++] = (byte) 0xA0;
            sendLocmToSE[j++] = SEND_LOCM;
            sendLocmToSE[j++] = (byte) 0x00;
            sendLocmToSE[j++] = (byte) 0x00;
            sendLocmToSE[j++] = (byte) 0x01;
            System.arraycopy(locm, 0, sendLocmToSE, j, locm.length);
            Log.d(tag, "LOCM being sent to the card");
            len = 0;
            try {
                len = javaCard.SDSCTransmitEx(sendLocmToSE, 0, sendLocmToSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            }
            catch(SDSCException e) {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            Log.d(tag, "Length of the received response from card: " + len);
            if(!Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Error in sending LocM to the card");
                Log.e(tag, "Status code received: " +
                        Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len-2, len)));
                return false;
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.i(tag, "Time for sending LOCM to the SE: " + elapsedTime/1000000);
            */
            /** Asking for SLOCM */
        /*
            len = 0;
            byte[] SLocM = new byte[128+2];
            try
            {
                len = javaCard.SDSCTransmitEx(getSLOCMFromSE, 0, getSLOCMFromSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, SLocM);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.i(tag, "Time for retrieving SLocM from the SE: " + elapsedTime / 1000000);
            //str.append("Time for retrieving signature from the SE: " + elapsedTime / 1000000 + "\n");
            Log.d(tag, "SLocM received from the card");
            Log.d(tag, "Length of the received SLocM: " + len);

            byte[] dataToSendToLocationServer = new byte[256];
            System.arraycopy(SLocP,0,dataToSendToLocationServer,0,SLocP.length-2);
            System.arraycopy(SLocM,0,dataToSendToLocationServer,SLocP.length-2,len-2);
            byte[] responseFromLocationServer = sendToLocationServer(dataToSendToLocationServer);
            Log.d(tag, "Response received from location server: " +
                    Utils.ByteArrayToHexString(responseFromLocationServer));
            // TODO: This should be sent to the card to be decrypted and the response should be checked
            if(responseFromLocationServer[0] != EVERYTHING_OK) {
                Log.e(tag, "Locations are not same");
                return false;
            }
            */
            /** Sending the received response from the location server to the patient */
        /*
            startTime = System.nanoTime();

            buf = new byte[1 + 5 + 21];
            j = 0;
            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_LOC_RESPONSE;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x16;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(responseFromLocationServer, 1, buf, j, 1);
            commandAuth = new Command(buf);
            Log.d(tag, "Sending response from Location server to the Patient");
            //Response res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Response received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Location response sending to patient failed");
                return false;
            }
            else
            {
                Log.d(tag, "Location response successfully sent to the patient");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.d(tag, "Time for sending location response to the patient: " + elapsedTime/1000000);
            //str.append("Time for sending signature to the Card: " + elapsedTime/1000000 + "\n");
            */
            /** The protocol continues */
            startTime = System.nanoTime();
            byte[] nextMessageToBeSent = readEncStuff();
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for reading E(KPbc, nc||IDc||nr||IDr) from the SE: " + elapsedTime/1000000);
            //str.append("Time for reading E(KPbc, nc||IDc||nr||IDr) from the SE: " + elapsedTime/1000000 + "\n");

            Log.d(tag, "Enc stuff received from the card: " +
                    Utils.ByteArrayToHexString(nextMessageToBeSent));

            Log.d(tag, "Length of the encrypted stuff received from the card: " +
                    nextMessageToBeSent.length);
            startTime = System.nanoTime();
            buf = new byte[128 + 5 + 21];
            j = 0;
            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_CONF;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x9A;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(nextMessageToBeSent, 0, buf, j, nextMessageToBeSent.length-2);
            commandAuth = new Command(buf);
            //res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Response received: " + Utils.ByteArrayToHexString(res.getResponseBytes()));
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Confirmation sending failed");
                return false;
            }
            else
            {
                Log.d(tag, "Confirmation sent successfully");
            }
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending E(KPbc, nc||IDc||nr||IDr) to the Card: " + elapsedTime / 1000000);
            //str.append("Time for sending E(KPbc, nc||IDc||nr||IDr) to the Card: " + elapsedTime/1000000 + "\n");
            startTime = System.nanoTime();
            setCommandForRead(commandAuth, this.ASK_CONF);
            //res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for receiving E(KPbr, Ks||nr||IDr) from the Card: " + elapsedTime/1000000);
            //str.append("Time for receiving E(KPbr, Ks||nr||IDr) from the Card: " + elapsedTime/1000000 + "\n");
            Log.d(tag, "Length of response payload received from the Card: " + res.getPayload().length);

            Log.d(tag, "Received confirmation from the Card: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));

            byte[] encSt = res.getPayload();
            startTime = System.nanoTime();
            succ = sendEncStuff(encSt, this.SEND_FINAL_CONF_CARD);
            elapsedTime = System.nanoTime() - startTime;
            Log.d(TimeTag, "Time for sending  E(KPbr, Ks||nr||IDr) to the SE: " + elapsedTime / 1000000);
            //str.append("Time for sending  E(KPbr, Ks||nr||IDr) to the SE: " + elapsedTime/1000000 + "\n");
            if(!succ)
            {
                Log.e(tag, "Final confirmation sending to the card failed");
                return false;
            }
            else
            {
                Log.d(tag, "Final confirmation sent to the card successfully");
            }
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    byte[] readEncStuff()
    {
        int len = javaCard.SDSCTransmitEx( askEncStuff, 0, askEncStuff.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        byte[] ret = Arrays.copyOf(bOutData, len);
        return ret;
    }

    /** TODO : Complete this function */
    byte[] sendToLocationServer(byte[] data) {
        //Use the class TCPConnection here
        if(locationServerAddress == null) {
            byte[] ret = new byte[2];
            ret[0]=ret[1]=EVERYTHING_OK;
            return ret;
        }
        TCPClient tcpClient = new TCPClient(locationServerAddress);
        byte ret[] = tcpClient.readResponse();
        return ret;
        /*
        byte[] ret = new byte[2];
        for(int i=0;i<ret.length;i++) {
            ret[i] = EVERYTHING_OK;
        }
        return ret;
        */
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
        Log.d(tag, "The APDU being sent to the SE: " + Utils.ByteArrayToHexString(sendingEncStuff));
        int len = 0;
        try {
            len = javaCard.SDSCTransmitEx(sendingEncStuff, 0, sendingEncStuff.length,
                    SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        }
        catch(SDSCException e) {
            Log.e(tag, "SDSCException occured: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        Log.d(tag, "Length of response received from the SE: " + len);
        //return true;

        if(Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
            return true;
        if(Arrays.equals(Arrays.copyOf(bOutData, len), CERT_FAILED))
            Log.e(tag, "Data verification failed");
        else
        {
            Log.e(tag, "Error in data verification sending");
            Log.e(tag, "Error code: " + Utils.ByteArrayToHexString(Arrays.copyOf(bOutData,len)));
        }
        return false;

    }

    boolean verifyCert(byte[] signC, byte[] keyCID)
    {
        int j = 0;
        verifyCertFromSE[j++] = (byte)0xA0;
        verifyCertFromSE[j++] = (byte)0xC0;
        verifyCertFromSE[j++] = (byte)0x00;
        verifyCertFromSE[j++] = (byte)0x00;
        verifyCertFromSE[j++] = (byte)0x80;
        System.arraycopy(signC, 0, verifyCertFromSE, j, signC.length);
        Log.d(tag, "Signature being sent to the card for verification");
        int len = 0;
        try {
            len = javaCard.SDSCTransmitEx(verifyCertFromSE, 0, verifyCertFromSE.length,
                    SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        }
        catch(SDSCException e) {
            Log.e(tag, "SDSCException occured: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        Log.d(tag, "Length of the received response from card: " + len);
        if(!Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
        {
            Log.e(tag, "Error in sending signature to the card");
            Log.e(tag, "Status code received: " +
                    Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len-2, len)));
            return false;
        }
        verifyCertFromSE = new byte[5+128+8];
        j = 0;
        verifyCertFromSE[j++] = (byte)0xA0;
        verifyCertFromSE[j++] = (byte)0xC1;
        verifyCertFromSE[j++] = (byte)0x00;
        verifyCertFromSE[j++] = (byte)0x00;
        verifyCertFromSE[j++] = (byte)0x88;
        System.arraycopy(keyCID, 0, verifyCertFromSE, j, keyCID.length);
        Log.d(tag, "Key || id being sent to the card for verification");
        try {
            len = javaCard.SDSCTransmitEx(verifyCertFromSE, 0, verifyCertFromSE.length,
                    SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        }
        catch(SDSCException e) {
            Log.e(tag, "SDSCException occured: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        Log.d(tag, "Length of the received response from card: " + len);
        if(!Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
        {
            if(Arrays.equals(Arrays.copyOf(bOutData, len), CERT_FAILED))
                Log.e(tag, "Certificate verification failed");
            else
                Log.e(tag, "Error in certificate verification");
            return false;
        }
        Log.d(tag, "Certificate verified");
        return true;
    }

    boolean mutualAuthentication(StringBuffer str)
    {
        //TODO: Add progress bar
        javaCard.selectDeviceAndApplet();
        //showMaProgressDialog();
        try
        {
            long startTime = System.nanoTime();
            Command commandAuth = new Command();
            setCommandForRead(commandAuth, ASK_RANDS);
            Log.d(tag, "Command array: " +
                    Utils.ByteArrayToHexString(commandAuth.getCommandArray()));
            //Response res = transactNfc(isoDep, commandAuth);
            Response res = sendData(commandAuth);
            Log.d(tag, "Length of response payload received from the Card: " +
                    res.getPayload().length);
            Log.d(tag, "Received randomsIds from the Card: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));
            // Checking length of received response
            if (res.getPayload().length != 16) {
                throw new Exception("RANDS IDS length incorrect");
            }
            long timeElapsed = System.nanoTime() - startTime;
            Log.d(tag, "Time For Receiving rand_s || id_s from server: " + timeElapsed/1000000);
            ////str.append("Time For Receiving rand_s || id_s from server: " + timeElapsed / 1000000 + "\n");
            startTime = System.nanoTime();
            byte[] randsIds = res.getPayload();
            byte[] dataToSE = new byte[5 + 16];
            int j = 0;
            dataToSE[j++] = (byte)0x80;
            dataToSE[j++] = (byte)0x30;
            dataToSE[j++] = (byte)0x00;
            dataToSE[j++] = (byte)0x00;
            dataToSE[j++] = (byte)0x10;
            System.arraycopy(randsIds, 0, dataToSE, j, randsIds.length);
            Log.d(tag, "randsIds being sent to the SE");
            int len = 0;
            try
            {
                len = javaCard.SDSCTransmitEx(dataToSE, 0, dataToSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            } catch(SDSCException e) {
                Log.e(tag, "SDSCException occured: " + "Error sending randsIds to the SE");
                throw e;
            }
            Log.d(tag, "Length of the received response from card: " + len);
            if(!Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Error in sending rands to the card");
                Log.e(tag, "Status code received: " +
                        Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len-2, len)));
                throw new SDSCException("Error in sending rands to the card");
            }
            timeElapsed = System.nanoTime() - startTime;
            Log.d(tag, "Time For sending rand_s || id_s to SE: " + timeElapsed / 1000000);
            // str.append("Time For sending rand_s || id_s to SE: " + timeElapsed/1000000 + "\n");
            /** Step 1 over */
            /** Step 2 begin */
            startTime = System.nanoTime();
            try {
                len = javaCard.SDSCTransmitEx(getRandpmacp, 0, getRandpmacp.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            } catch(SDSCException e) {
                Log.e(tag, "SDSCException occured: " + "Error sending apdu to receive randpmacp");
                throw e;
            }
            Log.d(tag, "randpmacp received from the SE: " +
                    Utils.ByteArrayToHexString(Arrays.copyOf(bOutData,len)));
            Log.d(tag, "Length of the received data: " + (len-2));
            // Checking length of received response from SE
            if (len != 42) {
                throw new Exception("RANDPMACP length incorrect");
            }
            timeElapsed = System.nanoTime() - startTime;
            Log.d(tag, "Time for reading RAND_S || RAND_P || MAC_P || ID_P from the SE: " +
                    timeElapsed / 1000000);
            /*
            str.append("Time for reading RAND_S || RAND_P || MAC_P || ID_P from the SE: " +
                    timeElapsed/1000000 + "\n");
                    */
            startTime = System.nanoTime();
            byte[] buf = new byte[40 + 5 + 21];
            j = 0;
            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_MACP;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x42;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(bOutData, 0, buf, j, len-2);
            Log.d(tag, "Buf: " + Utils.ByteArrayToHexString(buf));
            commandAuth = new Command(buf);
            Log.d(tag, "CommandAuth.payload: " + Utils.ByteArrayToHexString(commandAuth.getPayload()));
            Log.d(tag, "Sending MACPIDP to the Card");
            // res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Response received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "MACPIDP sending to Card failed");
                throw new SDSCException("MACPIDP sending to Card failed");
            }
            else
            {
                Log.d(tag, "MACPIDP successfully sent to the Card");
            }
            timeElapsed = System.nanoTime() - startTime;
            Log.d(tag, "Time for sending RAND_S || RAND_P || MAC_P || ID_P to Card : " +
                    timeElapsed / 1000000);
            /*
            str.append("Time for sending RAND_S || RAND_P || MAC_P || ID_P to Card : " +
                    timeElapsed/1000000 + "\n");
                    */
            /** End of step 2 */
            /** Step 3 begins */
            startTime = System.nanoTime();
            setCommandForRead(commandAuth, ASK_MACS);
            // res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Length of response payload received from the Card: " +
                    res.getPayload().length);
            Log.d(tag, "Received RANDS||MACS from the Card: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));
            // Checking length of received response
            if (res.getPayload().length != 24) {
                throw new Exception("RANDSMACS length incorrect");
            }
            timeElapsed = System.nanoTime() - startTime;
            Log.d(tag, "Time for receiving RAND_S || MAC_S from the Card: " + timeElapsed / 1000000);
            //str.append("Time for receiving RAND_S || MAC_S from the Card: " + timeElapsed / 1000000 + "\n");
            startTime = System.nanoTime();
            byte[] randsmacs = res.getPayload();

            dataToSE = new byte[5 + 24];
            j = 0;
            dataToSE[j++] = (byte)0x80;
            dataToSE[j++] = (byte) 0x32;
            dataToSE[j++] = (byte) 0x00;
            dataToSE[j++] = (byte)0x00;
            dataToSE[j++] = (byte)0x18;
            System.arraycopy(randsmacs, 0, dataToSE, j, randsmacs.length);
            Log.d(tag, "randsmacs being sent to the SE");
            len = 0;
            try
            {
                len = javaCard.SDSCTransmitEx(dataToSE, 0, dataToSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            } catch(SDSCException e) {
                Log.e(tag, "SDSCException occured: " + "Error sending randsmacs to SE");
                throw e;
            }
            Log.d(tag, "Length of the received response from card: " + len);
            if(!Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Error in sending rands to the card");
                Log.e(tag, "Status code received: " +
                        Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len-2, len)));
                throw new SDSCException("Error in sending rands to the card");
            }
            timeElapsed = System.nanoTime() - startTime;
            Log.d(tag, "Time for sending RAND_S || MAC_S to the SE: " + timeElapsed / 1000000);
            /** Send the final confirmation to the card */
            j = 0;
            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_FINAL_CONF;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x02; //This is practically useless here
            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            buf[j++] = (byte) 0x90;
            buf[j++] = (byte) 0x00;
            Log.d(tag, "Buf: " + Utils.ByteArrayToHexString(buf));
            commandAuth = new Command(buf);
            Log.d(tag, "CommandAuth.payload: " + Utils.ByteArrayToHexString(commandAuth.getPayload()));
            Log.d(tag, "Sending FINAL_CONF to the CARD");
            // res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Response received");
            Log.d(tag, "Mutual Authentication done");
            // str.append("Time for sending RAND_S || MAC_S to the SE: " + timeElapsed / 1000000 + "\n");

            //dismissMaProgressDialog();
            return true;

        } catch(Exception e) {
            e.printStackTrace();
        }
        //dismissMaProgressDialog();
        return false;
    }


    private BluetoothSocket establishBluetoothConnection() {
        BluetoothServerSocket serverSocket = null;
        try {
            serverSocket =
                    adapter.listenUsingInsecureRfcommWithServiceRecord("com.example.bluetoothreader",
                            UUID.fromString("851506f2-e9c2-4fa0-9584-467c3c107122"));
        }
        catch(IOException ex) {
            Log.e(tag, "Error in getting a server socket");
            return null;
        }
        socket = null;
        while(true) {
            try {
                Log.i(tag, "Listening for a client");
                socket = serverSocket.accept();
                //This is a blocking call
                Log.i(tag, "Client found");
            }
            catch(IOException ex) {
                Log.e(tag, "Error in accepting connection: " + ex.getMessage());
                break;
            }
            if(socket != null) break;
        }
        try {
            serverSocket.close();
        }
        catch(IOException ex) {
            Log.e(tag, "Error in closing serverSocket: " + ex.getMessage());
            ex.printStackTrace();
        }
        return socket;
    }

    private Socket establishTCPConnection() {
        Socket commSocket = null;
        try {
            InetAddress serverAddress = InetAddress.getByName(serverIp);
            commSocket = new Socket(serverAddress, portNo);
            Log.d(tag, "Connection to server established");
        }
        catch(IOException ex) {
            Log.e(tag, "Error in establishing TCP Connection: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
        serverTCPConnection = new TCPConnection(commSocket);
        byte[] serverReply = serverTCPConnection.read();
        if(serverReply == null) {
            Log.e(tag, "Error in reading serverReply");
            return null;
        }
        if(serverReply.length != 1) {
            Log.e(tag, "Incorrect length of response");
            Log.e(tag, "Reply received: " + Utils.ByteArrayToHexString(serverReply));
            return null;
        }
        if(serverReply[0] != MAKE_SERVER) {
            Log.e(tag, "Wrong reply from server");
            Log.e(tag, "Reply received: " + serverReply[0]);
            return null;
        }
        Log.d(tag, "ServerReply received: " + Utils.ByteArrayToHexString(serverReply));
        ServerSocket tcpSocket = null;
        Socket ret = null;
        try {
            tcpSocket = new ServerSocket(doctorPortNo);
        }
        catch(IOException ex) {
            Log.e(tag, "Error in making server socket: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
        Log.d(tag, "ServerSocket formed");
        byte[] buffer = new byte[1];
        buffer[0] = OK_TO_CONNECT;
        serverTCPConnection.write(buffer);
        Log.d(tag, "Sent OK_TO_CONNECT");
        try {
            ret = tcpSocket.accept();
            if (ret == null) {
                Log.e(tag, "Error in accepting connection");
                return null;
            }
        }
        catch(IOException ex) {
            Log.e(tag, "Error in accepting connection");
            return null;
        }
        Log.d(tag, "Connection established with the patient");
        return ret;
    }

    void changeFile(int size) {
        File dir,file;
        if(MainActivity.isExternalStorageWritable()) {
            dir = MainActivity.getTextStorageDir(MainActivity.dirName);
            file = new File(dir, MainActivity.fileName);
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(file);
                byte[] data;
                StringBuffer str = new StringBuffer("");
                for(int i=0;i<size;i++) str.append("a");
                data = str.toString().getBytes();
                fout.write(data, 0, data.length);
                fout.flush();
                fout.close();
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }
        else {
            Log.e(tag, "External storage not writable");
        }
    }

    @Override
    public void run() {
        super.run();
        Log.d(tag, "run on Mutual Authentication thread");
        if(option == OPTION_BLUETOOTH) {
            if(adapter == null) {
                Log.e(tag, "Bluetooth not supported");
                return;
            }
            if(!adapter.isEnabled()) {
                adapter.enable();
                waitTillBtEnable();
            }
            BluetoothSocket socket = establishBluetoothConnection();
            if(socket == null) {
                Log.e(tag, "Bluetooth connection could not be established");
                return;
            }
            bluetoothConnection = new BluetoothConnection(socket);

            long startTime = System.nanoTime();

            //boolean success = mutualAuthenticationAsymmWithLocation();
            boolean success = true;
            if(success) {
                Log.i(tag, "Mutual Authentication successful");
            }
            else {
                Log.e(tag, "Mutual Authentication failed");
            }
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Total time taken for mutual authentication: " + elapsedTime / 1000000);
            String str = "Total time taken for mutual authentication: " + elapsedTime / 1000000;
            if(success) {
                uiHandler.obtainMessage(ResultActivity.PRINT_RESULT_SUCCESS, str).sendToTarget();
            }
            else {
                uiHandler.obtainMessage(ResultActivity.PRINT_RESULT_FAILURE, str).sendToTarget();
            }
            File dir,file;
            //changeFile(1048576);
            if(MainActivity.isExternalStorageReadable()) {
                dir = MainActivity.getTextStorageDir(MainActivity.dirName);
                file = new File(dir, MainActivity.fileName);
                FileInputStream fin = null;


                //Sending the length of the file
                int fileSize = (int) file.length();
                Log.d(tag, "Length of the file: " + fileSize);
                byte[] lengthArray = Utils.intToByteArray(fileSize);
                bluetoothConnection.write(lengthArray);

                //Sending the file
                try {
                    fin = new FileInputStream(file);
                    byte[] readData = new byte[fileSize];
                    startTime = System.nanoTime();
                    int sendBytes = 0;
                    int bytesRead;
                    while(sendBytes<fileSize) {
                        bytesRead = fin.read(readData,0,fileSize);
                        Log.d(tag, "bytesRead: " + bytesRead);
                        sendBytes+=bytesRead;
                        bluetoothConnection.write(Arrays.copyOf(readData,bytesRead));
                    }
                    byte[] tempData = bluetoothConnection.read();
                    if(tempData != null && tempData.length == 1 && tempData[0]==FILE_READ) {
                        elapsedTime = System.nanoTime() - startTime;
                        Log.d(tag, "File sent successfully");
                        Log.d(TimeTag, "Time for file transfer: " + elapsedTime/1000000);
                    }
                    else {
                        Log.e(tag, "Error in file transfer");
                    }

                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
            else {
                Log.e(tag, "External storage is unreadable");
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

            long startTime = System.nanoTime();
            //boolean success = mutualAuthenticationAsymmWithLocation();
            boolean success = true;
            if(success) {
                Log.i(tag, "Mutual Authentication successful");
            }
            else {
                Log.e(tag, "Mutual Authentication failed");
            }
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(TimeTag, "Total time taken for mutual authentication: " + elapsedTime / 1000000);
            String str = "Total time taken for mutual authentication: " + elapsedTime / 1000000;
            if(success) {
                uiHandler.obtainMessage(ResultActivity.PRINT_RESULT_SUCCESS, str).sendToTarget();
            }
            else {
                uiHandler.obtainMessage(ResultActivity.PRINT_RESULT_FAILURE, str).sendToTarget();
            }
            //changeFile(15360);
            File dir,file;
            if(MainActivity.isExternalStorageReadable()) {
                dir = MainActivity.getTextStorageDir(MainActivity.dirName);
                file = new File(dir, MainActivity.fileName);
                FileInputStream fin = null;


                //BufferedReader frin = null;
                //BufferedWriter brout = new BufferedWriter(new OutputStreamWriter(tcpConnection.out));
                //BufferedInputStream fin = null;
                //BufferedOutputStream bout = new BufferedOutputStream(tcpConnection.out);
                //frin = new BufferedReader(new FileReader(file));
                /*
                    while((readData = frin.readLine()) != null) {
                        Log.d(tag, "Data read length: " + readData.length());
                        brout.write(readData, 0, readData.length());
                        brout.flush();
                        Log.d(tag, "Data written to the card");
                    }
                    */
                //fin = new BufferedInputStream(new FileInputStream(file));
                //brout.close();
                //////

                //Sending the length of the file
                int fileSize = (int) file.length();
                Log.d(tag, "Length of the file: " + fileSize);
                byte[] lengthArray = Utils.intToByteArray(fileSize);
                tcpConnection.write(lengthArray);

                //Sending the file
                try {
                    fin = new FileInputStream(file);
                    byte[] readData = new byte[fileSize];
                    startTime = System.nanoTime();
                    int sendBytes = 0;
                    int bytesRead;
                    while(sendBytes<fileSize) {
                        bytesRead = fin.read(readData,0,fileSize);
                        Log.d(tag, "bytesRead: " + bytesRead);
                        sendBytes+=bytesRead;
                        tcpConnection.write(Arrays.copyOf(readData,bytesRead));
                    }
                    byte[] tempData = tcpConnection.read();
                    if(tempData != null && tempData.length == 1 && tempData[0]==FILE_READ) {
                        elapsedTime = System.nanoTime() - startTime;
                        Log.d(tag, "File sent successfully");
                        Log.d(TimeTag, "Time for file transfer: " + elapsedTime/1000000);
                    }
                    else {
                        Log.e(tag, "Error in file transfer");
                    }

                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
            else {
                Log.e(tag, "External storage is unreadable");
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
