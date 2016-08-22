package com.example.rishabh.mareaderc;

import java.util.Arrays;

public class Command {

    private static final String tag = "libHCEReader";

    private byte cla, ins, p1, p2, length;
    private byte[] errorCheck;
    private byte[] payload;

    // static int curPointer = -1;
    // private byte[][] payloadArray;

    public Command(byte[] command) {
        super();
        this.cla = command[0];
        this.ins = command[1];
        this.p1 = command[2];
        this.p2 = command[3];
        this.length = command[4];
        if (command.length > 26) {
            this.errorCheck = Arrays.copyOfRange(command, 5, 26);
            this.payload = Arrays.copyOfRange(command, 26, command.length);
        }
        // this.payloadArray = new byte[0][];
    }

    public Command(Command command) {
        super();
        this.cla = command.getCla();
        this.ins = command.getIns();
        this.p1 = command.getP1();
        this.p2 = command.getP2();
        this.length = command.getLength();
        this.errorCheck = command.getErrorCheck();
        this.payload = command.getPayload();
        // this.payloadArray = Utils.ByteArraySplit(command.getPayload());
    }

    // public Command(Command command, byte[] payload) {
    // super();
    // this.cla = command.getCla();
    // this.ins = command.getIns();
    // this.p1 = command.getP1();
    // this.p2 = command.getP2();
    // this.length = command.getLength();
    // this.errorCheck = command.getErrorCheck();
    // this.payload = payload;
    // // this.payloadArray = Utils.ByteArraySplit(command.getPayload());
    // }

    public Command() {
    }

    public byte getCla() {
        return cla;
    }

    public void setCla(byte cla) {
        this.cla = cla;
    }

    public byte getIns() {
        return ins;
    }

    public void setIns(byte ins) {
        this.ins = ins;
    }

    public byte getP1() {
        return p1;
    }

    public void setP1(byte p1) {
        this.p1 = p1;
    }

    public byte getP2() {
        return p2;
    }

    public void setP2(byte p2) {
        this.p2 = p2;
    }

    public boolean isRead() {
        return (p2 & 1) >= 1;
    }

    public boolean areMoreFragments() {
        return (p2 & 2) > 1;
    }

    public void setRead() {
        this.p2 = (byte) (this.p2 | 1);
    }

    public void setMoreFragments() {
        this.p2 = (byte) (this.p2 | 2);
    }

    public void setMoreFragments(boolean more) {
        this.p2 = (byte) (more ? (byte) (this.p2 | 2) : (byte) (this.p2 & 1));
    }

    public byte getLength() {
        return length;
    }

    public void setLength(byte length) {
        this.length = length;
    }

    public byte[] getErrorCheck() {
        return errorCheck;
    }

    public void setErrorCheck(byte[] errorCheck)
    {
        this.errorCheck = errorCheck;
    }

    public byte[] getPayload()
    {
        return payload;
    }

    public void setPayload(byte[] payload)
    {
        this.payload = payload;
    }

    public void addPayload(byte[] payload2) {
        this.payload = Utils.ConcatArrays(payload, payload2);
    }

    //
    // public byte[] getNextPayloadFromArray() {
    // curPointer++;
    // return payloadArray[curPointer];
    // }
    //
    // public boolean nextPayloadFromArrayExists() {
    // curPointer++;
    // Log.d(tag, "Current Pointer " + curPointer);
    // return curPointer < payloadArray.length;
    // }

    // public Command getNextCommandToSend() {
    // // curPointer++;
    // Command cmd = new Command(this);
    // // cmd.setPayload(payloadArray[curPointer], false);
    // Log.d(tag, "Next Command Payload: " + new String(cmd.getPayload()));
    // return cmd;
    // }

    public byte[] getCommandArray() {

//		if (length != (byte) 0)
        return Utils.ConcatArrays(new byte[] { cla }, new byte[] { ins },
                new byte[] { p1 }, new byte[] { p2 },
                new byte[] { length }, errorCheck, payload);
        // else
        // return Utils
        // .ConcatArrays(new byte[] { cla }, new byte[] { ins },
        // new byte[] { p1 }, new byte[] { p2 },
        //						new byte[] { length });
    }
}

