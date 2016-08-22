package com.example.rishabh.bluetoothreader;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.gotrust.sesdapi.SDSCException;
import com.gotrust.sesdapi.SESDAPI;

import java.io.IOException;
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
            0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x05, 0x01};

    private byte[] getSignFromSE = {(byte)0xA0, (byte) 0xB5, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] getModulusIDFromSE = {(byte)0xA0, (byte) 0xB3, (byte)0x00,
            (byte) 0x00, (byte)0x88};

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
    private byte SEND_FINAL_CONF_CARD = (byte)0xC4;
    private byte SEND_ENC_STUFF_TO_CARD = (byte)0xC2;
    private byte[] CERT_FAILED = {(byte)0x69, (byte)0x84};

    Response response = null;

    BluetoothConnection bluetoothConnection = null;
    TCPConnection serverTCPConnection = null;
    BluetoothSocket socket;

    //TCP Constants
    private final int portNo = 4445;
    private final int doctorPortNo = 4123;
    private String serverIp = null;
    private final byte MAKE_SERVER = (byte) 0x03;
    private final byte OK_TO_CONNECT = (byte) 0x02;

    MutualAuthenticationThread(String address, int option, JavaCard javaCard) {
        //see what it needs here
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
            if(commandAuth.getIns() != SEND_FINAL_CONF) {
                byte[] res = bluetoothConnection.read();
                if (res == null) {
                    Log.e(tag, "Error in reading");
                    return (new Response());
                } else {
                    return (new Response(res));
                }
            }
            else {
                return (new Response());
            }
        }
        else {
            tcpConnection.write(data);
            if(commandAuth.getIns() != SEND_FINAL_CONF) {
                byte[] res = tcpConnection.read();
                if (res == null) {
                    Log.e(tag, "Error in reading");
                    return (new Response());
                } else {
                    return (new Response(res));
                }
            }
            else {
                return (new Response());
            }
        }
    }

    boolean mutualAuthentication()
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
            Log.d(tag, "Length of response payload received from the HCE: " +
                    res.getPayload().length);
            Log.d(tag, "Received randomsIds from the HCE: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));
            // Checking length of received response
            if (res.getPayload().length != 16) {
                throw new Exception("RANDS IDS length incorrect");
            }
            long timeElapsed = System.nanoTime() - startTime;
            Log.d(tag, "Time For Receiving rand_s || id_s from server: " + timeElapsed/1000000);
            //str.append("Time For Receiving rand_s || id_s from server: " + timeElapsed / 1000000 + "\n");
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
            Log.d(tag, "Sending MACPIDP to the HCE");
            // res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Response received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "MACPIDP sending to HCE failed");
                throw new SDSCException("MACPIDP sending to HCE failed");
            }
            else
            {
                Log.d(tag, "MACPIDP successfully sent to the HCE");
            }
            timeElapsed = System.nanoTime() - startTime;
            Log.d(tag, "Time for sending RAND_S || RAND_P || MAC_P || ID_P to HCE : " +
                    timeElapsed / 1000000);
            /*
            str.append("Time for sending RAND_S || RAND_P || MAC_P || ID_P to HCE : " +
                    timeElapsed/1000000 + "\n");
                    */
            /** End of step 2 */
            /** Step 3 begins */
            startTime = System.nanoTime();
            setCommandForRead(commandAuth, ASK_MACS);
            // res = transactNfc(isoDep, commandAuth);
            res = sendData(commandAuth);
            Log.d(tag, "Length of response payload received from the HCE: " +
                    res.getPayload().length);
            Log.d(tag, "Received RANDS||MACS from the HCE: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));
            // Checking length of received response
            if (res.getPayload().length != 24) {
                throw new Exception("RANDSMACS length incorrect");
            }
            timeElapsed = System.nanoTime() - startTime;
            Log.d(tag, "Time for receiving RAND_S || MAC_S from the HCE: " + timeElapsed / 1000000);
            //str.append("Time for receiving RAND_S || MAC_S from the HCE: " + timeElapsed / 1000000 + "\n");
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

    void startTheActivityToShowResults() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Create intent to run a new Activity
            }
        });
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
        // TODO
        //startTheActivityToShowResults();
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

    @Override
    public void run() {
        super.run();
        Log.d(tag,"run on Mutual Authentication thread");
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
            boolean success = mutualAuthentication();
            if(success) {
                Log.i(tag, "Mutual Authentication successful");
                //Print on UI thread
            }
            else {
                Log.e(tag, "Mutual Authentication failed");
                //Print on the UI thread
            }
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(tag, "Total time taken for mutual authentication: " + elapsedTime/1000000);
            cancel();
            //Closing the bluetoothSocket and BluetoothThread
            //btThread.cancel();
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
            boolean success = mutualAuthentication();
            if(success) {
                Log.i(tag, "Mutual Authentication successful");
                //Print on UI thread
            }
            else {
                Log.e(tag, "Mutual Authentication failed");
                //Print on the UI thread
            }
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(tag, "Total time taken for mutual authentication: " + elapsedTime/1000000);
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
        if(serverTCPConnection != null) {
            serverTCPConnection.cancel();
        }
    }
}