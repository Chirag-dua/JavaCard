package com.example.rishabh.mareaderc;

import android.app.ProgressDialog;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import android.app.ProgressDialog;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public abstract class ReaderActivity extends AppCompatActivity implements
        CardReader.ReadCallBack {

    public static final String TAG = "libHCEReader";
    private static final String tag = "libHCEReader";
    public static int READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A
            | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;

    private CardReader mCardReader;
    ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mCardReader = new CardReader(this);
        Log.i(TAG, "onCreate CardReader instance made");

        // pDialog = new ProgressDialog(ReaderActivity.this);
        //pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }

    /**
     * Method to start the reader mode.
     * <p/>
     * If reader mode isn't enabled, devices wont interact, even when tapped
     * together. Reader mode should be enabled before tapping to be able to
     * detect the other device.
     */
    protected void enableReaderMode()
    {
        Log.i(TAG, "Enabling reader mode");

        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        if (nfc != null) {
            nfc.enableReaderMode(this, mCardReader, READER_FLAGS, null);
        }
        // Toast.makeText(getBaseContext(), "Enabled Reader Mode",
        // Toast.LENGTH_SHORT).show();
    }

    /**
     * Method to stop reader mode.
     * <p/>
     * Advisable to free up the reader mode when leaving the activity. If reader
     * mode is engaged, other NFC functions like Beam and other Wallet like apps
     * wont function, so always close reader mode safely before leaving the app.
     */
    protected void disableReaderMode()
    {
        Log.i(TAG, "Disabling reader mode");
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        if (nfc != null) {
            nfc.disableReaderMode(this);
        }
        // Toast.makeText(getBaseContext(), "Disabled Reader Mode",
        // Toast.LENGTH_SHORT).show();
    }

    byte[] result = " ".getBytes();

    int i = 0;

    /**
     * Method to start a transaction with the emulator app on the other device
     * <p/>
     * Send a custom "sendCommand" that will be read by emulator, which will
     * accordingly send back a message, that will be returned by this function.
     * <p/>
     * Usually you do not need to override this function. If data is larger than
     * 255 chars, this method will ideally internally iterate over the calls to
     * send the data.
     *
     * @param command
     *            - A string command to send to card emulator device.
     * @return - The message sent back by emulator device after receiving the
     *         command
     */
    @Override
    public Response transactNfc(IsoDep isoDep, Command command)
            throws IOException {

        /**
         * In the case of read, it assumes that we would not need to send multiple APDUs for the
         * read command.
         *
         * Similarly, in the case of write, it assumes that we would not receive long responses,
         * and never bothers to store the response payloads. It just checks the response codes.
         *
         * In mutual authentication we need to implement that check in both scenarios. See to it
         * that this is taken care of while integrating.
         *
         * Now implemented the mutual authentication using half duplex scheme
         */
        if (command.isRead())
        {
            /*
            runOnUiThread(new Runnable()
            {

                @Override
                public void run() {
                    pDialog.setMessage("Reading Card");
                    pDialog.show();
                }
            });
            */
            Response res;

            this.result = new byte[0];

            do
            {
                Log.d(tag,
                        "Command : "
                                + Utils.ByteArrayToHexString(command
                                .getCommandArray()));

                byte[] responseArray = isoDep.transceive(command        //debug here
                        .getCommandArray());
                res = new Response(responseArray);

                this.result = Utils.ConcatArrays(this.result, res.getPayload());

                Log.d(tag,
                        "Response Result Array : "
                                + Utils.ByteArrayToHexString(responseArray));

            } while (Arrays.equals(res.getResponseCode(),
                    Response.RESPONSE_SUCCESS_MORE_FRAGMENTS));
            res.setPayload(result);
            Log.d(tag, "Final response payload received: " + Utils.ByteArrayToHexString(result));
            return res;


            //Command is write

        }
        else
        {
            /*
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    pDialog.setMessage("Writing Card");
                    pDialog.show();
                }
            });
            */
            Log.d(TAG, "transactNFC started");
            final byte[][] payLoadArray = Utils.ByteArraySplit(command
                    .getPayload());
            i = 0;
            for (; i < payLoadArray.length; i++)
            {
                byte[] curPayLoad = payLoadArray[i];
                command.setPayload(curPayLoad);
                /*
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        pDialog.setMax(payLoadArray.length - 1);
                        pDialog.setProgress(i);
                        if(i == (payLoadArray.length - 1))
                            pDialog.dismiss();

                    }
                });
                */
                /*
                command.setErrorCheck(getStringOfLength(
                        i + "|" + payLoadArray.length, 20).getBytes());
                */
                /*
                Log.d(TAG,
                        "transactNFC Payload String"
                                + new String(command.getPayload()));
                */
                Log.d(TAG,
                        "transactNFC Payload String: "
                                + Utils.ByteArrayToHexString(command.getPayload()));
                Log.d(TAG,
                        "transactNFC Hex Full "
                                + Utils.ByteArrayToHexString(command
                                .getCommandArray()));

                if (i != payLoadArray.length - 1)
                {
                    command.setMoreFragments();
                    Log.d(TAG, "set More Frag " + command.areMoreFragments());
                }
                else
                {
                    command.setMoreFragments(false);
                    Log.d(TAG,
                            "set More Frag to false : "
                                    + command.areMoreFragments());
                    byte[] responseArray = isoDep.transceive(command
                            .getCommandArray());
                    Response res = new Response(responseArray);
                    Log.d(TAG,
                            "transactNFC final Response "
                                    + Utils.ByteArrayToHexString(responseArray));
                    /*
                    Log.d(TAG, "transactNFC final Response payload : "
                            + new String(res.getPayload()));
                    */
                    Log.d(TAG, "transactNFC final Response payload : "
                            + Utils.ByteArrayToHexString(res.getPayload()));
                    return res;
                }
                byte[] responseArray = isoDep.transceive(command
                        .getCommandArray());
                Log.d(TAG,
                        "transactNFC Next Response "
                                + Utils.ByteArrayToHexString(responseArray));
                Response res = new Response(responseArray);

                if (!Arrays.equals(res.getResponseCode(),
                        Response.RESPONSE_SUCCESS_DONE))
                {
                    Log.d(TAG, "Response not Success");
                    return res;
                }
            }
        }
        return new Response();
    }

    private String getStringOfLength(String string, int len) {
        if (string.length() > len)
            return string.substring(0, len);

        for (int i = string.length(); i <= len; i++)
            string += " ";
        return string;
    }

    public abstract void onHceStarted(IsoDep isoDep);

}

