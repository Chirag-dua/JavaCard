package eappsk;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.Signature;
import javacard.security.RandomData;
import javacard.security.KeyBuilder;

public class Card extends Applet {
	
	private byte[] ids = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
	private byte[] idp;
	private AESKey key;
	private RandomData rnd;
	private Signature sig;
	
	private byte[] randp, rands;
	private byte[] aeskey;
	
	public static final byte APP_CLA = (byte)0x80;
	public static final byte STEP_ONE = (byte)0x30;
	public static final byte STEP_TWO = (byte)0x31;
	public static final byte STEP_THREE = (byte)0x32;
	
	public static final byte GET_AES_KEY = (byte)0xD0;
	public static final byte RESET = (byte)0xD2;

	private Card() {
		rnd = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
		key = (AESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
		byte[] keyData = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 
				0x0C, (byte)0x0D, 0x0E, 0x0F};
		key.setKey(keyData, (short)0);
		sig = Signature.getInstance(Signature.ALG_AES_MAC_128_NOPAD, false);
		
		aeskey = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, (byte)0x0B,
				(byte)0x0C, 0x0D, 0x0E, 0x0F};
		register();
	}

	public static void install(byte bArray[], short bOffset, byte bLength)
			throws ISOException {
		new Card();
	}

	public void process(APDU apdu) throws ISOException {
		short le, lc, mLen;
        
		if(this.selectingApplet())
			return;
		
		byte[] buffer = apdu.getBuffer();
		
		if(buffer[ISO7816.OFFSET_CLA] != APP_CLA)
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		switch(buffer[ISO7816.OFFSET_INS]) {
		case STEP_ONE:
			rands = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_RESET);
			rnd.generateData(rands, (short)0, (short)rands.length);
			Util.arrayCopy(rands, (short)0, buffer, (short)0, (short)rands.length);
			Util.arrayCopy(ids, (short)0, buffer, (short)rands.length, (short)ids.length);
			apdu.setOutgoingAndSend((short)0, (short)(rands.length+ids.length));
			break;
		case STEP_TWO:
			lc = apdu.setIncomingAndReceive();
			randp = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_RESET);
			idp = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(buffer, (short)(ISO7816.OFFSET_CDATA+8), randp, (short)0, (short)randp.length);
			Util.arrayCopy(buffer, (short)(ISO7816.OFFSET_CDATA+32), idp, (short)0, (short)idp.length);
			/* Verify MAC */
			sig.init(key, Signature.MODE_VERIFY);
			sig.update(idp, (short)0, (short)idp.length);
			sig.update(ids, (short)0, (short)ids.length);
			sig.update(rands, (short)0, (short)rands.length);
			if(!sig.verify(randp, (short)0, (short)randp.length, buffer, (short)(ISO7816.OFFSET_CDATA+16), (short)16))
				ISOException.throwIt(ISO7816.SW_DATA_INVALID);
			break;
		case STEP_THREE:
			Util.arrayCopy(rands, (short)0, buffer, (short)0, (short)rands.length);
			sig = Signature.getInstance(Signature.ALG_AES_MAC_128_NOPAD, false);
			sig.init(key, Signature.MODE_SIGN);
			sig.update(ids, (short)0, (short)ids.length);
			mLen = sig.sign(randp, (short)0, (short)randp.length, buffer, (short)8);
			apdu.setOutgoingAndSend((short)0, (short)(8+mLen));
			break;
		case GET_AES_KEY:
			Util.arrayCopy(aeskey, (short)0, buffer, (short)0, (short)aeskey.length);
			apdu.setOutgoingAndSend((short)0, (short)aeskey.length);
			break;
		case RESET:
			rands = null;
			randp = null;
			sig = Signature.getInstance(Signature.ALG_AES_MAC_128_NOPAD, false);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}

	}

}
