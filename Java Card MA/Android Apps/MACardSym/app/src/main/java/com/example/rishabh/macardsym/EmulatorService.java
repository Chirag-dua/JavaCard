package com.example.rishabh.macardsym;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

import java.util.Arrays;

import android.content.Context;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public abstract class EmulatorService extends HostApduService {

    private static final String tag = "EmulatorService";
    private Command commandFinal = new Command();

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

    byte[][] finalResponsePayloads;
    int cur = 0;

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras)
    {

        Command cmd = new Command(commandApdu);

        if (cmd == null)
        {
            Log.d(tag, "Got a command Null ");
        }

        if (Arrays.equals(commandApdu, BuildSelectApdu(CARD_AID))) {
            Log.d(tag, "Applet selected");
            return Response.RESPONSE_SUCCESS_DONE;
        }

//		Log.d(tag, "Got a command " + Utils.ByteArrayToHexString(commandApdu));
//		Log.d(tag, "Got a command with Payload " + new String(cmd.getPayload()));
//		Log.d(tag, "Got a command with Read " + cmd.isRead());
//		Log.d(tag, "Got a command with P2 " + cmd.getP2());

        if (cmd.isRead()) {

//			new String().length();
            //Response res = new Response();
            Response res;
            res = onReceiveCommand(cmd);
            finalResponsePayloads = Utils.ByteArraySplit(res.getPayload());
            Log.d(tag,"Got a response array with length: "
                    + finalResponsePayloads.length);
            if (cur < finalResponsePayloads.length - 1) {
                res.setResponseCode(Response.RESPONSE_SUCCESS_MORE_FRAGMENTS);
            } else {
                res.setResponseCode(Response.RESPONSE_SUCCESS_DONE);
            }
            if (finalResponsePayloads.length == cur) {
                cur = 0;
            }
            byte[] curPayLoad = finalResponsePayloads[cur];
            Log.d(tag, "Current response array: " + Utils.ByteArrayToHexString(curPayLoad));
            // Log.d(tag, "Current response array " + new String(curPayLoad));


            res.setPayload(curPayLoad);
            res.setLength((byte) curPayLoad.length);
            cur++;
            /*
            res.setErrorCheck(getStringOfLength(
                    cur + "|" + finalResponsePayloads.length, 20).getBytes());
            */
            byte[] errArr = new byte[21];
            for(int w = 0;w<21;w++)
                errArr[w] = (byte)0x01;
            res.setErrorCheck(errArr);

            if (cur == finalResponsePayloads.length) {
                cur = 0;
            }

            return res.getResponseBytes();




            //Command is write
        } else {


            commandFinal.setCla(cmd.getCla());
            commandFinal.setIns(cmd.getIns());
            commandFinal.setP1(cmd.getP1());
            commandFinal.setP2(cmd.getP2());
            commandFinal.setLength(cmd.getLength());
            commandFinal.setErrorCheck(cmd.getErrorCheck());

            /*
            Log.d(tag,
                    "Got a command with payload : "
                            + new String(cmd.getPayload()));
            */
            Log.d(tag, "Got a command with payload : " +
                    Utils.ByteArrayToHexString(cmd.getPayload()));

            if (cmd.areMoreFragments()) {
                commandFinal.addPayload(cmd.getPayload());
                Log.d(tag, "final command with payload : "
                        + new String(commandFinal.getPayload()));
                return (Response.RESPONSE_SUCCESS_DONE);
            } else {
                commandFinal.addPayload(cmd.getPayload());
                /*
                Log.d(tag,
                        "No More Fragments Final commmand is with payload : "
                                + new String(commandFinal.getPayload()));
                */
                Log.d(tag, "No More Fragments Final commmand is with payload : " +
                        Utils.ByteArrayToHexString(commandFinal.getPayload()));
                Response res = onReceiveCommand(commandFinal);
                commandFinal.setPayload(new byte[0]);
                // Log.d(tag,
                // "Got a response with payload : "
                // + new String(res.getPayload()));

                return res.getResponseBytes();
            }
        }

    }

    private String getStringOfLength(String string, int len) {
        if (string.length() > len)
            return string.substring(0, len);

        for (int i = string.length(); i <= len; i++)
            string += " ";
        return string;
    }

    @Override
    public void onDeactivated(int reason) {
    }

    public abstract Response onReceiveCommand(Command command);

}
