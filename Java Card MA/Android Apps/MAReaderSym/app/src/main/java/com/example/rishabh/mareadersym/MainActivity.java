package com.example.rishabh.mareadersym;

import android.annotation.TargetApi;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.gotrust.sesdapi.SESDAPI;
import com.gotrust.sesdapi.SDSCException;

import java.io.File;
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

    /* The name of the IO file which will be binded for Input-Output on the card */
    private final String ioFileName = "SMART_IO.CRD";

    /* The file descriptor of the device */
    private int phDevice = -1;

    /* Mutual Authentication constants*/

    private byte[] getRandpmacp = {(byte)0x80, (byte)0x31, (byte) 0x00, (byte) 0x00, (byte)0x28};

    private byte[] selectAPDU = {0x00, (byte)0xA4, 0x04, 0x00, 0x0B, 0x01, 0x02, 0x03,
            0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x08, 0x01};

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

    /** The SESDAPI Object which will be used for communication */
    private SESDAPI obj = new SESDAPI();

    private byte[] n1;
    //private byte[] n1n2;
    //private  TextView textView;
    /* Mutual Authentication constants end */

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
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
            if(ioFilePath == null) {

            }
            else {
                Thread t1 = new Thread(new Runnable() {
                    public void run() {
                        try {
                            obj.SDSCCreateIoFile(ioFilePath);
                        }
                        catch(SDSCException ex) {

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
        obj.SDSCDisconnectDev(phDevice);
    }

    boolean mutualAuthentication(IsoDep isoDep)
    {
        try
        {
            setCommandForRead(commandAuth, ASK_RANDS);
            Response res = transactNfc(isoDep, commandAuth);
            Log.d(tag, "Length of response payload received from the HCE: " +
                    res.getPayload().length);
            Log.d(tag, "Received randomsIds from the HCE: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));

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
                len = obj.SDSCTransmitEx(phDevice, dataToSE, 0, dataToSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
            }
            Log.d(tag, "Length of the received response from card: " + len);
            if(!Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Error in sending rands to the card");
                Log.e(tag, "Status code received: " +
                        Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len-2, len)));
                return false;
            }
            /** Step 1 over */
            /** Step 2 begin */

            try
            {
                len = obj.SDSCTransmitEx(phDevice, getRandpmacp, 0, getRandpmacp.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
            }
            Log.d(tag, "randpmacp received from the SE");
            Log.d(tag, "Length of the received data: " + len);

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
            commandAuth = new Command(buf);
            Log.d(tag, "Sending MACPIDP to the HCE");
            res = transactNfc(isoDep, commandAuth);
            Log.d(tag, "Response received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "MACPIDP sending to HCE failed");
                return false;
            }
            else
            {
                Log.d(tag, "MACPIDP successfully sent to the HCE");
            }

            /** End of step 2 */
            /** Step 3 begins */
            setCommandForRead(commandAuth, ASK_MACS);
            res = transactNfc(isoDep, commandAuth);
            Log.d(tag, "Length of response payload received from the HCE: " +
                    res.getPayload().length);
            Log.d(tag, "Received RANDS||MACS from the HCE: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));

            byte[] randsmacs = res.getPayload();

            dataToSE = new byte[5 + 24];
            j = 0;
            dataToSE[j++] = (byte)0x80;
            dataToSE[j++] = (byte)0x32;
            dataToSE[j++] = (byte)0x00;
            dataToSE[j++] = (byte)0x00;
            dataToSE[j++] = (byte)0x18;
            System.arraycopy(randsmacs, 0, dataToSE, j, randsmacs.length);
            Log.d(tag, "randsmacs being sent to the SE");
            len = 0;
            try
            {
                len = obj.SDSCTransmitEx(phDevice, dataToSE, 0, dataToSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
            }
            Log.d(tag, "Length of the received response from card: " + len);
            if(!Arrays.equals(Arrays.copyOf(bOutData, len), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Error in sending rands to the card");
                Log.e(tag, "Status code received: " +
                        Utils.ByteArrayToHexString(Arrays.copyOfRange(bOutData, len-2, len)));
                return false;
            }
            Log.d(tag, "Mutual Authentication done");
            return true;
            /** End of step 3 */
            /** This is from the old app */
            /*
            long startTime = System.nanoTime();
            len = 0;
            try
            {
                len = obj.SDSCTransmitEx(phDevice, getSignFromSE, 0, getSignFromSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
            }
            long elapsedTime = System.nanoTime() - startTime;
            Log.i(tag, "Time for retrieving signature from the SE: " + elapsedTime/1000000);
            Log.d(tag, "Signature received from the card");
            Log.d(tag, "Length of the received signature: " + len);

            byte[] message = new byte[136 + 2];

            startTime = System.nanoTime();
            int lenMod = 0;
            try {
                lenMod = obj.SDSCTransmitEx(phDevice, getModulusIDFromSE, 0, getModulusIDFromSE.length,
                        SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, message);
            }
            catch(SDSCException e)
            {
                Log.e(tag, "SDSCException occured: " + e.getMessage());
                e.printStackTrace();
            }

            elapsedTime = System.nanoTime() - startTime;
            Log.i(tag, "Time for retrieving key||ID from the SE: " + elapsedTime/1000000);
            Log.d(tag, "Key received from the card");
            Log.d(tag, "Length of the received key || id: " + lenMod);

            /** Send this to HCE */
            /*
            byte[] buf = new byte[128 + 5 + 21];
            j = 0;
            buf[j++] = (byte) 0x80;
            buf[j++] = SEND_SIGN;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x00;
            buf[j++] = (byte) 0x95;

            while (j < 26) {
                buf[j++] = (byte) 0x01;
            }
            System.arraycopy(bOutData, 0, buf, j, len-2);
            commandAuth = new Command(buf);
            Log.d(tag, "Sending signature to the HCE");
            res = transactNfc(isoDep, commandAuth);
            Log.d(tag, "Response received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Signature sending to HCE failed");
                return false;
            }
            else
            {
                Log.d(tag, "Signature successfully sent to the HCE");
            }

            /** Signature sent to the HCE */

            /** Sending key ID to the HCE */
            /*

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
            Log.d(tag, "The command apdu send to HCE: " + Utils.ByteArrayToHexString(buf));
            res = transactNfc(isoDep, commandAuth);
            Log.d(tag, "Response for key received");

            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Key sending failed");
                return false;
            }
            else
            {
                Log.d(tag, "Key successfully sent to the HCE");
            }

            /** Sending the read command to the HCE for reading the certificate */
            /*
            setCommandForRead(commandAuth, this.ASK_CERT);

            Log.d(tag, "Commmand set for reading");
            Log.d(tag, "Command being sent: " +
                    Utils.ByteArrayToHexString(commandAuth.getCommandArray()));

            res = transactNfc(isoDep, commandAuth);
            Log.d(tag, "Transaction of ASK_CERT request done");

            Log.d(tag, "Length of response payload: " + res.getPayload().length);
            Log.d(tag, "Response received: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));


            byte[] payload = res.getPayload();
            Log.d(tag, "Payload received: " + Utils.ByteArrayToHexString(payload));
            byte[] signC = Arrays.copyOfRange(payload, 0, 128);
            Log.d(tag, "SignC received from HCE: " + Utils.ByteArrayToHexString(signC));
            byte[] keyCID = Arrays.copyOfRange(payload, 128, 264);
            Log.d(tag, "keyCID received from HCE: " + Utils.ByteArrayToHexString(keyCID));
            byte[] encStuff = Arrays.copyOfRange(payload, 264, payload.length);
            Log.d(tag, "encStuff received from HCE: " + Utils.ByteArrayToHexString(encStuff));

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
            byte[] nextMessageToBeSent = readEncStuff();
            Log.d(tag, "Enc stuff received from the card: " +
                    Utils.ByteArrayToHexString(nextMessageToBeSent));

            Log.d(tag, "Length of the encrypted stuff received from the card: " +
                    nextMessageToBeSent.length);
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
            res = transactNfc(isoDep, commandAuth);
            Log.d(tag, "Response received");
            if(!Arrays.equals(res.getResponseCode(), Response.RESPONSE_SUCCESS_DONE))
            {
                Log.e(tag, "Confirmation sending failed");
                return false;
            }
            else
            {
                Log.d(tag, "Confirmation sent successfully");
            }

            setCommandForRead(commandAuth, this.ASK_CONF);
            res = transactNfc(isoDep, commandAuth);

            Log.d(tag, "Length of response payload received from the HCE: " + res.getPayload().length);

            Log.d(tag, "Received confirmation from the HCE: " +
                    Utils.ByteArrayToHexString(res.getResponseBytes()));

            byte[] encSt = res.getPayload();
            succ = sendEncStuff(encSt, this.SEND_FINAL_CONF_CARD);
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
            */
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    byte[] readEncStuff()
    {
        int len = obj.SDSCTransmitEx(phDevice, askEncStuff, 0, askEncStuff.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
        byte[] ret = Arrays.copyOf(bOutData, len);
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
        Log.d(tag, "The APDU being sent to the SE: " + Utils.ByteArrayToHexString(sendingEncStuff));
        int len = obj.SDSCTransmitEx(phDevice, sendingEncStuff, 0, sendingEncStuff.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
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
        int len = obj.SDSCTransmitEx(phDevice, verifyCertFromSE, 0, verifyCertFromSE.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
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
        len = obj.SDSCTransmitEx(phDevice, verifyCertFromSE, 0, verifyCertFromSE.length,
                SESDAPI.SDSC_DEV_DEFAULT_TIME_OUT, bOutData);
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
