package com.example.rishabh.bluetoothreaderasymm;

import android.nfc.tech.IsoDep;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Utils {

    private static int PACKET_SIZE = 225;
    private static final String tag = "Utils";
    public static String bluetoothAddress;


    //	private static final String accessJSON = "";
    //  public static HashMap<String, String> accessMap = new HashMap<String, String>();
    // public static ArrayList<String> sections = new ArrayList<String>();

/*

    public static String selectedVital = "NA";
    public final static int VITAL_TYPE_WIEGHT = 1;
    public final static int VITAL_TYPE_HEIGHT = 2;
    public final static int VITAL_TYPE_TEMPRATURE = 3;


    public static int totalVisits;
   */
    /*
    public static ArrayList<OBX> weightEntryList = new ArrayList<OBX>();
    public static ArrayList<OBX> heightEntryList = new ArrayList<OBX>();
    public static ArrayList<OBX> tempEntryList = new ArrayList<OBX>();
    public static HashMap<String, ArrayList<OBX>> remainingEntryListMap = new HashMap<String, ArrayList<OBX>>();
    public static String selectedVisit = "NA";

    public static ArrayList<Item> uniqueVitalNames = new ArrayList<Item>();
    public static String selectedDepartment = "NA";
    public static String selectedDeptConcept = "NA";
    public static ArrayList<String> allDepartmentsList = new ArrayList<String>(); //Unique Department Names
    public static ArrayList<String> VisitWiseDepartmentsList = new ArrayList<String>(); // All Department Names in order of Visits
    public static ArrayList<String> deptOfDocVisitsList = new ArrayList<String>(); //Visits of Doctor's Department
    public static HashMap<String, ArrayList<OBX>> visitEntryListMap = new HashMap<String, ArrayList<OBX>>();
    public static String selectedVisitConcept = "NA"; //Concept selected from a particular Visit
    public static String selectedVisitDept = "NA"; //Department of the Visit selected
    public static String doctorDept = "NA"; //Department of the Doctor who has logged in

    */

    /*

    public static JSONObject selectedVisitJSON = new JSONObject();


    public static final int USER_DOCTOR = 1;
    public static final int USER_ADMIN = 2;
    public static final int USER_PHARAMASIST = 3;
    public static final int USER_NURSE = 4;

    public static int USER_PROFILE = 1;


    public static final int DEPT_CARDIOLOGY = 1;
    public static final int DEPT_ONCOLOGY = 2;


    public static int cal;

    */
    // TODO : Following time variables should be removed from here, these are just of testing purpose
    public static long nfcTime, bluetoothTime;

//	public static final String sampleHL7 = "MSH|^~\\&|FORMENTRY|LOCAL|HL7LISTENER|OPENMRS|20140509000000||ORU^R01|ORUMESSAGE|P|2.5|1||||||||1^OPENMRS.FORMID\r"
//			+ "PID|||1991^^^Old Identification Number||Patient^Demo^OldId||\r"
//			+ "PV1||O|1^Unknown Location||||1^Super User (admin)|||||||||||||||||||||||||||||||||||||20140509000000|||||||V\r"
//			+ "ORC|RE||||||||20140509102537|1^Super User\r"
//			+ "OBR|1|||1114^VITAL SIGNS^99DCT\r"
//			+ "OBX|1|NM|5089^WEIGHT (KG)^99DCT||50|kg|20-300|L|||F|||20140509000000\r"
//			+ "OBX|2|NM|5090^HEIGHT (CM)^99DCT||150|cm|10-228|L|||F|||20140509000000\r"
//			+ "OBX|3|NM|5088^TEMPERATURE (C)^99DCT||37|DEG C|25-43|L|||F|||20140509000000\r"
//			+ "OBX|4|NM|5087^PULSE^99DCT||120|rate/min|0-230|L|||F|||20140509000000\r"
//			+ "OBX|5|NM|5089^WEIGHT (KG)^99DCT||30|kg|20-300|L|||F|||20090509000000\r"
//			+ "OBX|6|NM|5090^HEIGHT (CM)^99DCT||120|cm|10-228|L|||F|||20090509000000\r"
//			+ "OBX|7|NM|5088^TEMPERATURE (C)^99DCT||36.5|DEG C|25-43|L|||F|||20090509000000\r"
//			+ "OBX|8|NM|5087^PULSE^99DCT||80|rate/min|0-230|L|||F|||20090509000000\r"
//			+ "OBX|9|NM|5089^WEIGHT (KG)^99DCT||16|kg|20-300|L|||F|||20060821000000\r"
//			+ "OBX|10|NM|5090^HEIGHT (CM)^99DCT||98|cm|10-228|L|||F|||20060821000000\r"
//			+ "OBX|11|NM|5088^TEMPERATURE (C)^99DCT||37.3|DEG C|25-43|L|||F|||20060821000000\r"
//			+ "OBX|12|NM|5087^PULSE^99DCT||100|rate/min|0-230|L|||F|||20060821000000\r"
//			+ "NTE|1|L|GENERAL OBSERVATIONS\r"
//			+ "OBR|2|||6105^CARDIAC TEST^99DCT\r"
//			+ "OBX|1|NM|5087^PULSE^99DCT||120|rate/min|0-230|L|||F|||20140509000000\r"
//			+ "OBX|2|CE|12^X-RAY, CHEST^99DCT||5158^EVIDENCE OF CARDIAC ENLARGEMENT^99DCT||||||F|||20140509000000\r"
//			+ "OBX|3|CE|1071^REVIEW OF SYSTEMS, CARDIOPULMONARY^99DCT||136^CHEST PAIN^99DCT||||||F|||20140509000000\r"
//			+ "OBX|4|NM|5085^SYSTOLIC BLOOD PRESSURE^99DCT||120|mmHg|0-250|L|||F|||20140509000000\r"
//			+ "OBX|5|NM|5086^DIASTOLIC BLOOD PRESSURE^99DCT||89|mmHg|0-150|L|||F|||20140509000000\r"
//			+ "OBX|6|CE|1124^CARDIAC EXAM FINDINGS^99DCT||562^CARDIAC MURMUR^99DCT||||||F|||20140509000000\r"
//			+ "OBX|7|NM|5087^PULSE^99DCT||80|rate/min|0-230|L|||F|||20090509000000\r"
//			+ "OBX|8|NM|5087^PULSE^99DCT||100|rate/min|0-230|L|||F|||20060821000000\r"
//			+ "NTE|2|L|CARDIOLOGY\r"
//			+ "OBR|3|||6106^ONCOLOGICAL TEST^99DCT\r"
//			+ "OBX|1|NM|21^HEMOGLOBIN^99DCT||13.5|g/dL|||||F|||20140509000000\r"
//			+ "OBX|2|NM|678^WHITE BLOOD CELLS^99DCT||5.4|10^3/uL|||||F|||20140509000000\r"
//			+ "OBX|3|NM|679^RED BLOOD CELLS^99DCT||4.0|10^6/uL|||||F|||20140509000000\r"
//			+ "OBX|4|NM|729^PLATELETS^99DCT||250|10^3/uL|||||F|||20140509000000\r"
//			+ "OBX|5|CE|1121^LYMPH NODE EXAM FINDINGS^99DCT||1115^NORMAL^99DCT||||||F|||20140509000000\r"
//			+ "OBX|6|CE|845^ULTRASOUND, ABDOMEN^99DCT||1115^NORMAL^99DCT||||||F|||20140509000000\r"
//			+ "OBX|7|NM|21^HEMOGLOBIN^99DCT||14.6|g/dL|||||F|||20090509000000\r"
//			+ "OBX|8|NM|678^WHITE BLOOD CELLS^99DCT||1.5|10^3/uL|||||F|||20090509000000\r"
//			+ "OBX|9|NM|679^RED BLOOD CELLS^99DCT||2.4|10^6/uL|||||F|||20090509000000\r"
//			+ "OBX|10|NM|729^PLATELETS^99DCT||40|10^3/uL|||||F|||20090509000000\r"
//			+ "OBX|11|CE|1121^LYMPH NODE EXAM FINDINGS^99DCT||1116^ABNORMAL^99DCT||||||F|||20090509000000\r"
//			+ "OBX|12|CE|845^ULTRASOUND, ABDOMEN^99DCT||1116^ABNORMAL^99DCT||||||F|||20090509000000\r"
//			+ "NTE|3|L|ONCOLOGY\r"
//			+ "OBR|4|||6107^ENDOCRINOLOGICAL TEST^99DCT\r"
//			+ "OBX|1|NM|887^SERUM GLUCOSE^99DCT||132|mg/dl|||||F|||20140509000000\r"
//			+ "OBX|2|NM|887^SERUM GLUCOSE^99DCT||138|mg/dl|||||F|||20090509000000\r"
//			+ "OBX|3|CE|56^URINE MICROSCOPY^99DCT||1116^ABNORMAL^99DCT||||||F|||20090509000000\r"
//			+ "OBX|4|CE|1069^REVIEW OF SYSTEMS, GENERAL^99DCT||5544^WEIGHT LOSS^99DCT||||||F|||20090509000000\r"
//			+ "OBX|5|CE|1069^REVIEW OF SYSTEMS, GENERAL^99DCT||5949^FATIGUE^99DCT||||||F|||20090509000000\r"
//			+ "OBX|6|NM|887^SERUM GLUCOSE^99DCT||114|mg/dl|||||F|||20060821000000\r"
//			+ "NTE|4|L|ENDOCRINOLOGY\r"
//			+ "OBR|5|||1281^MEDICATION HISTORY^99DCT\r"
//			+ "OBX|1|ST|6104^DRUG USED^99DCT||ABC DRUG^SOLID^25mg^20140509000000^20140517000000||||||F|||20140509000000\r"
//			+ "OBX|2|ST|6104^DRUG USED^99DCT||XANAX^SOLID^50mg^20090509000000^20090521000000||||||F|||20090521000000\r"
//			+ "OBX|3|ST|6104^DRUG USED^99DCT||ALLEGRA^SOLID^25mg^20090509000000^20090523000000||||||F|||20090521000000\r"
//			+ "OBX|4|ST|6104^DRUG USED^99DCT||XYZ SYRUP^LIQUID^25ml^20090509000000^20090517000000||||||F|||20090509000000\r"
//			+ "OBX|5|ST|6104^DRUG USED^99DCT||PARACETAMOL^SOLID^2g^20060821000000^20060824000000||||||F|||20060821000000\r"
//			+ "NTE|5|L|MEDICINE HISTORY\r"
//			+ "OBR|6|||1284^PROBLEM LIST^99DCT\r"
//			+ "OBX|1|CE|6042^PROBLEM ADDED^99DCT||5016^CARDIOMYOPATHY^99DCT|||||||||20140509000000\r"
//			+ "OBX|2|CE|6097^PROBLEM RESOLVED^99DCT||5041^HIV STAGING - LYMPHOMA^99DCT|||||||||20140509000000\r"
//			+ "OBX|3|CE|6042^PROBLEM ADDED^99DCT||175^DIABETES MELLITUS^99DCT|||||||||20090509000000\r"
//			+ "OBX|4|CE|6042^PROBLEM ADDED^99DCT||5041^HIV STAGING - LYMPHOMA^99DCT|||||||||20090509000000\r"
//			+ "NTE|6|L|LIST OF DISEASES\r";


    /*
    public static String tempJsonFileData = "";
        */
    /**
     * This function determines the packet size to be used according to the device specs
     * and the information received from the other device.
     *
     *  Not used at the moment
     */
    public static void determinePacketSize(IsoDep isodep, int p) {
        if(isodep.isExtendedLengthApduSupported() && (p>0))
            PACKET_SIZE=65000;
        else
            PACKET_SIZE=225;
    }
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

    public static byte[] intToByteArray(int a) {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    public static int byteArrayToInt(byte[] b) {
        return (b[3] & 0xFF) + ((b[2] & 0xFF) << 8) + ((b[1] & 0xFF) << 16) + ((b[0] & 0xFF) << 24);
    }

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
        // Give the list the right capacity to start with. You could use an
        // array
        // instead if you wanted.
//		Log.d(tag, "Found text to split : " + text);
//		Log.d(tag, "Found text to split : String" + new String(text));
//		Log.d(tag, "Found text to split : HexArray" + ByteArrayToHexString(text));

        byte[][] ret = new byte[((text.length + PACKET_SIZE - 1) / PACKET_SIZE)][];
        // ;
        int retcounter = 0;

        for (int start = 0; start < text.length; start += PACKET_SIZE) {
            ret[retcounter] = Arrays.copyOfRange(text, start,
                    Math.min(text.length, start + PACKET_SIZE));


            Log.d(tag, "String : " + Utils.ByteArrayToHexString(ret[retcounter]));
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