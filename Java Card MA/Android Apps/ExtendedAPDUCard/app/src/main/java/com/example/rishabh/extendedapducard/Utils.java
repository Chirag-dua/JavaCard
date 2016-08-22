package com.example.rishabh.extendedapducard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

//import edu.dce.nfc.cardemulator.entities.OBX;

import android.util.Log;

public class Utils {






    private static final int PACKET_SIZE = 225;
    private static final String tag = "Utils";



    /**
     * Utility method to convert a byte array to a hexadecimal string.
     *
     * @param bytes
     *            Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String ByteArrayToHexString(byte[] bytes) {
        final char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String digits = "0123456789abcdef";

    public static String toHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i != data.length; i++) {
            int v = data[i] & 0xff;
            buf.append(digits.charAt(v >> 4));
            buf.append(digits.charAt(v & 0xf));
        }
        return buf.toString();
    }

    /**
     * Utility method to convert a hexadecimal string to a byte string.
     * <p/>
     * <p>
     * Behavior with input strings containing non-hexadecimal characters is
     * undefined.
     *
     * @param s
     *            String containing hexadecimal characters to convert
     * @return Byte array generated from input
     */
    public static byte[] HexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character
                    .digit(s.charAt(i + 1), 16));
        }
        return data;

    }

    /**
     * Utility method to concatenate two byte arrays.
     *
     * @param first
     *            First array
     * @param rest
     *            Any remaining arrays
     * @return Concatenated copy of input arrays
     */
    public static byte[] ConcatArrays(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    public static byte[][] ByteArraySplit(byte[] text) {

        if(text==null)
        {
            return new byte[1][1];
        }

        // Give the list the right capacity to start with. You could use an
        // array
        // instead if you wanted.
        Log.d(tag, "Found text to split : " + ByteArrayToHexString(text));
        // Log.d(tag, "Found text to split : " + text);
        byte[][] ret = new byte[((text.length + PACKET_SIZE - 1) / PACKET_SIZE)][];
        // ;
        int retcounter = 0;

        for (int start = 0; start < text.length; start += PACKET_SIZE) {
            ret[retcounter] = Arrays.copyOfRange(text, start,
                    Math.min(text.length, start + PACKET_SIZE));
            retcounter += 1;
        }
        return ret;
    }

    public static String[] StringSplit255(String text) {
        // Give the list the right capacity to start with. You could use an
        // array
        // instead if you wanted.
        Log.d(tag, "Found text to split : " + text);

        String[] ret = new String[(text.length() + PACKET_SIZE - 1)
                / PACKET_SIZE];
        int retcounter = 0;

        for (int start = 0; start < text.length(); start += PACKET_SIZE) {
            ret[retcounter] = (text.substring(start,
                    Math.min(text.length(), start + PACKET_SIZE)));
            // ret[retcounter] = (text.substring(start, Math.min(text.length(),
            // start + PACKET_SIZE)));
            retcounter += 1;
        }
        return ret;
    }

}
