package com.example.rishabh.mareadersym;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

public class CardReader implements NfcAdapter.ReaderCallback {
    private static final String TAG = "libHCEReader";

    IsoDep isoDep;
    /**
     * AID for our loyalty card service.
     */
    public static final String CARD_AID = "F222223456";

    public static byte[] BuildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH |
        // DATA]
        return Utils.HexStringToByteArray(HEADER_SELECT
                + String.format("%02X", aid.length() / 2) + aid);
    }

    /**
     * ISO-DEP command HEADER for selecting an AID. Format: [Class | Instruction
     * | Parameter 1 | Parameter 2]
     */
    public static final String HEADER_SELECT = "00A40400";

    /**
     * "OK" status word sent in response to SELECT AID command (0x9000)
     */
    public static final byte[] RESPONSE_SELECT_OK = { (byte) 0x90, (byte) 0x00 };

    // Weak reference to prevent retain loop. mAccountCallback is responsible
    // for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or
    // onStop()).
    private WeakReference<ReadCallBack> mAccountCallback;

    public CardReader(ReadCallBack readCallBack)
    {
        mAccountCallback = new WeakReference<ReadCallBack>(readCallBack);
    }

    /**
     * Callback when a new tag is discovered by the system.
     * <p/>
     * <p>
     * Communication with the card should take place here.
     *
     * @param tag
     *            Discovered tag
     */
    @Override
    public void onTagDiscovered(Tag tag) {
        Log.i(TAG, "onTagDiscovered");
        isoDep = IsoDep.get(tag);
        if (isoDep != null) {

            try {
                // Connect to the remote NFC device
                isoDep.connect();
                Log.i(TAG, "isoDep connected");

                // Select the card
                byte[] selCommand = BuildSelectApdu(CARD_AID);
                // byte[] mResult = isoDep.transceive(selCommand);
                //Log.d(TAG, "res received for the select commmand");
                Log.i(TAG, "selCommand: " + Utils.ByteArrayToHexString(selCommand));
                byte[] resArr = isoDep.transceive(selCommand);

                Log.d(TAG, "resArr is generated");
                Response res = new Response(resArr);
                Log.d(TAG, "res received for the select commmand");
                // mResult = TransceiveResult.get(isoDep, selCommand);
                Log.d(TAG, "" + Utils.ByteArrayToHexString(selCommand));
                // if (Arrays.equals(RESPONSE_SELECT_OK,
                // mResult.getStatusword())) {
                if (Arrays.equals(res.getResponseCode(),
                        Response.RESPONSE_SUCCESS_DONE)) {
                    Log.d(TAG, "response = OK");
                    mAccountCallback.get().onHceStarted(isoDep);
                }// }

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error communicating with card: " + e.toString());
            }
        }
    }

    public interface ReadCallBack
    {
        public void onHceStarted(IsoDep isoDep);

        public Response transactNfc(IsoDep isoDep, Command command)
                throws IOException;
    }

}