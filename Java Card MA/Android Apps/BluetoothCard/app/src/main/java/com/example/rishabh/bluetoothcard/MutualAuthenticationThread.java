package com.example.rishabh.bluetoothcard;

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

import java.io.IOException;
import java.io.InputStream;
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
    private final BluetoothAdapter adapter;
    private BluetoothDevice device;
    private BluetoothConnection bluetoothConnection;
    private TCPConnection tcpConnection;
    private final JavaCard javaCard;
    private BluetoothSocket socket;

    //Mutual Authentication variables
    private byte[] selectAPDU = {0x00, (byte)0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x05, 0x02};

    //private byte[] getModulusAPDU = {(byte)0xA0, (byte)0xB1, (byte)0x00, (byte)0x00, (byte)0x80};
    //private byte[] getRandomAPDU = {(byte)0xA0, (byte)0xB3, (byte)0x10, (byte)0x00, (byte)0x80};
    //private byte[] testMessageAPDU = {(byte)0xA0, (byte)0xB8, (byte)0x00, (byte)0x00, (byte)0x10};
    private byte[] getRandsIdsFromSE = {(byte)0x80, (byte) 0x30, (byte)0x00, (byte)0x00, (byte)0x10};
    private byte[] getMacsFromSE = {(byte)0x80, (byte) 0x32, (byte)0x00, (byte)0x00, (byte)0x18};

    private byte[] getSignFromSE = {(byte)0xA0, (byte) 0xB5, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] getModulusIDFromSE = {(byte)0xA0, (byte) 0xB3, (byte)0x00,
            (byte) 0x00, (byte)0x88};

    private byte[] askEncStuff = {(byte)0xA0, (byte) 0xC3, (byte)0x00, (byte)0x00, (byte)0x80};
    private byte[] askFinalConf = {(byte)0xA0, (byte) 0xC4, (byte)0x00, (byte)0x00, (byte)0x80};

    private byte[] CERT_FAILED = {(byte)0x69, (byte)0x84};
    private byte ASK_CERT = (byte) 0x31;
    private byte SEND_CERT = (byte) 0x30;
    private byte SEND_CONF = (byte) 0x32;
    private byte ASK_CONF = (byte) 0x33;
    private byte SEND_ENC_STUFF_TO_CARD = (byte)0xC2;

    /* TCP constants */
    private final byte GET_LIST = (byte) 0x05;
    private final byte OK_TO_CONNECT = (byte) 0x02;
    private final byte READ_ACK = (byte) 0x01;

    private static final int DEFAULT_SIZE = 1024;

    /* This buffer is used for retrieving response from the Java Card */
    private byte[] bOutData = new byte[DEFAULT_SIZE];

    private final int option;
    private final int portNo = 4444;
    private final int doctorPortNo = 4123;
    private String serverIp = null;

    MutualAuthenticationThread(String address, int option, JavaCard javaCard) {
        this.javaCard = javaCard;
        this.option = option;
        this.adapter = BluetoothAdapter.getDefaultAdapter();
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

    public Response onReceiveCommand(Command command) {
        long startTime, elapsedTime;
        int len;
        //Log.d(tag, " DATA : " + new String(command.getPayload()));
        Log.d(tag, "DATA array: " + Utils.ByteArrayToHexString(command.getCommandArray()));
        Log.d(tag, "DATA payload: " + Utils.ByteArrayToHexString(command.getPayload()));

        Response res = new Response();
        res.setResponseCode(Response.RESPONSE_SUCCESS_DONE);
        res.setErrorCheck("Sample Error Checkkkk".getBytes());

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
            byte[] inData=null;
            if(option == MainActivity.OPTION_BLUETOOTH)
                inData = bluetoothConnection.read();
            else if(option == MainActivity.OPTION_TCP)
                inData = tcpConnection.read();
            if(inData == null) {
                Log.e(tag, "Error in reading");
                return false;
            }
            Command command = new Command(inData);
            Response res = onReceiveCommand(command);
            if(Arrays.equals(res.getResponseCode(), Response.RESPONSE_STOP_PROCESS)) {
                Log.i(tag, "Mutual Authentication successful");
                return true;
            }
            if(option == MainActivity.OPTION_BLUETOOTH)
                bluetoothConnection.write(res.getResponseBytes());
            else if(option == MainActivity.OPTION_TCP)
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
            long startTime = System.nanoTime();
            boolean success = mutualAuthentication();
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(tag, "Total time taken for mutual authentication: " + elapsedTime/1000000);
            //Closing the bluetooth socket at the end
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
            boolean success = mutualAuthentication();
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
    }
}
