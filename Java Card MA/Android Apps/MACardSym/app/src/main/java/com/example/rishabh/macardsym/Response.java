package com.example.rishabh.macardsym;

import java.util.Arrays;

public class Response {

    private static final String tag = "Response";
    public static byte[] RESPONSE_SUCCESS_DONE = { (byte) 0x90, (byte) 0x00 };
    public static byte[] RESPONSE_SUCCESS_MORE_FRAGMENTS = { (byte) 0x90,
            (byte) 0x01 };
    public static byte[] RESPONSE_UNKNOWN_COMMAND = { (byte) 0x91, (byte) 0x00 };
    public static byte[] RESPONSE_ERROR = { (byte) 0x91, (byte) 0x01 };

    private byte length;
    private byte[] responseCode;
    private byte[] errorCheck;
    private byte[] payload;

    public Response(byte[] command) {
        super();
        this.responseCode = Arrays.copyOfRange(command, 0, 2);

        if (command.length > 2)
            this.length = command[2];
        else
            this.length = (byte) 0x00;

        if (command.length > 24) {
            this.errorCheck = Arrays.copyOfRange(command, 3, 24);
            this.payload = Arrays.copyOfRange(command, 24, command.length);
        } else {
            this.errorCheck = "Sample Errror Check ".getBytes();
            this.payload = "No Data".getBytes();
        }
    }

    public Response() {
    }

    public byte getLength() {
        return length;
    }

    public void setLength(byte length) {
        this.length = length;
    }

    public byte[] getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(byte[] responseCode) {
        this.responseCode = responseCode;
    }

    public byte[] getErrorCheck() {
        return errorCheck;
    }

    public void setErrorCheck(byte[] errorCheck) {
        this.errorCheck = errorCheck;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
        this.length = (byte) payload.length;
    }

    public byte[] getResponseBytes() {

        if(payload==null)
            payload=new byte[10];
        if(errorCheck==null)
            errorCheck=new byte[10];

        return Utils.ConcatArrays(responseCode, new byte[] { length },
                errorCheck, payload);
    }

}