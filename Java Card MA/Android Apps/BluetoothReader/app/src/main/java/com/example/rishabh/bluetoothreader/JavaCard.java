package com.example.rishabh.bluetoothreader;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.Log;

import com.gotrust.sesdapi.SDSCException;
import com.gotrust.sesdapi.SESDAPI;

import java.io.File;
import java.util.Vector;

public class JavaCard {

    private static JavaCard instance;
    public static final String TAG = "Javacard";
    private Context context;

    public static int state=0;

    private static final int DEFAULT_SIZE = 256;

    /* The name of the IO file which will be binded for Input-Output on the card */
    private final String ioFileName = "SMART_IO.CRD";

    /* The file descriptor of the device */
    private int phDevice = -1;

    /* APDU for selecting applet */
    private byte[] selectAPDU = {0x00, (byte)0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x05, 0x01};
    private byte[] getAesAPDU = {(byte) 0xA0, (byte) 0XD0, 0X00, 0X00, 0X10};

    /* This buffer is used for retrieving response from the Java Card */
    private byte[] bOutData = new byte[DEFAULT_SIZE];

    /* The SESDAPI Object which will be used for communication */
    private SESDAPI obj;

    private byte[] aesKey;

    private JavaCard(Context context) {
        this.context = context;
        this.obj = new SESDAPI();
//		this.aesKey = "0123456789abcdef".getBytes();
    }

    /* Never pass activity context in this, pass application context to avoid memory leak */
    public static JavaCard getInstance(Context context) {
        if (instance == null) {
            instance = new JavaCard(context);
        }
        return instance;
    }

    public SESDAPI getSesdapi() {
        return obj;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String getIOFilePath() {
        StringBuffer name = new StringBuffer();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] accessible = context.getExternalFilesDirs(null);
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

    public Vector<String> goListDevs() throws SDSCException {
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
                        try {
                            Log.e("JavaCard", "Creating Io file :" + ioFilePath);
                            obj.SDSCCreateIoFile(ioFilePath);
                        }
                        catch(SDSCException ex) {
                            ex.printStackTrace();
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

    private byte[] getAesKeyFromCard() {
        obj.SDSCTransmitEx(phDevice, getAesAPDU, 0, getAesAPDU.length, SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        byte[] key = new byte[16];
        System.arraycopy(bOutData, 0, key, 0, 16);
        return key;
    }

    public byte[] getAesKey() {
        if (aesKey == null) {
			/* This will save time since key will be fetched once and then can be accessed any time */
            aesKey = getAesKeyFromCard();
            // Return this fix key for testing purposed
            // TODO : Write key on java card and retrieve from there
//			aesKey = "0123456789ABCDEF".getBytes();
        }
//		Log.e("Javacard", new String(aesKey));
        return aesKey;
    }

    public void goCreateIOFile() {
        String name = getIOFilePath();
        goCreateIOFile(name);
    }

    public int getPhDevice() {
        return phDevice;
    }

    public void selectDeviceAndApplet() {
        if(state==0) {
            try {
                Vector<String> devices = goListDevs();
                for (String dev : devices) {
                    Log.e("Javacard", dev);
                }
                if (devices.size() == 1)
                    this.phDevice = goConnectDev(devices.elementAt(0));
                obj.SDSCTransmitEx(phDevice, selectAPDU, 0, selectAPDU.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
                state=1;
                Log.d(TAG,"Connected to java card now");
            } catch (SDSCException e) {
                Log.e(TAG, "Error in initializing card");
                e.printStackTrace();
                try {
                    obj.SDSCDisconnectDev(phDevice);
                    state=0;
                }
                catch(SDSCException ee) {
                    Log.e(TAG, "Error in Disconnecting inside onCreate");
                    ee.printStackTrace();
                }
                return;
            }
        }
        else {
            Log.e(TAG,"onCreate on MyService:state is 1, Javacard is already connected");
        }
    }

    public int SDSCTransmitEx(byte 	bCommand[], int off, int intCommandLen, int intTimeOutMode, byte bOutData[]) throws SDSCException {
        int len=0;
        if(state == 1) {
            try {
                len = obj.SDSCTransmitEx(phDevice, bCommand, off, intCommandLen, intTimeOutMode, bOutData);
            }
            catch(SDSCException e) {
                throw e;
            }
        }
        else {
            Log.e(TAG, "State 0: Cannot transmit as the javacard is disconnected");
        }
        return len;
    }

    public void SDSCDisconnectDev() {
        if(state==1) {
            try {
                state = 0;
                obj.SDSCDisconnectDev(phDevice);
                Log.d(TAG, "Successfully connection destroyed");
            } catch (Exception e) {
                Log.e(TAG, "onDestroy- Error in disconnecting");
                e.printStackTrace();
            }
        }
        else {
            Log.e(TAG,"SDSCDisconnectDev: State is 0,card is already disconnected");
        }
    }

}
