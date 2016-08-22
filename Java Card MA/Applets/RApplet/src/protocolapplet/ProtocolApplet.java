/* Medico */
package protocolapplet;

import java.io.IOException;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.CardException;
import javacard.framework.ISOException;
import javacard.framework.ISO7816;
import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.CryptoException;
import javacard.security.KeyBuilder;
import javacard.security.RSAPrivateKey;
import javacard.security.RSAPublicKey;
import javacard.security.KeyPair;
import javacard.security.RandomData;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

public class ProtocolApplet extends Applet {
	
	/* The public key of the server */
	private RSAPublicKey pkey_server;
	
	/* The RSA keys for this particular secure element */
	private KeyPair rsaKeys;
	
	/* ID of this particular secure element */
	private byte[] id;
	
	/* Location and secure location */
	private byte[] loc, sloc;
	
	/* Signature of this particular element */
	private byte[] signature;
	
	/* Temporary array for operations */
	private byte[] temp;
	
	/* Random number generator */
	private final RandomData rnd;
	
	/* Random numbers */
	private byte[] nc, nr;
	
	/* Public key of the card */
	private RSAPublicKey cardKey;
	
	/* Id of foreign entity */
	private byte[] forId;
	
	/* AES Key stored in a byte array */
	private byte[] aeskey;
	
	private Cipher rsa_cipher, aes_cipher;
	
	private AESKey sessionKey;
	
	public static final byte SET_ID = (byte)0xB0;
	public static final byte GET_ID = (byte)0xB1;
	
	public static final byte SET_KEY = (byte)0xB2;
	public static final byte GET_MODULUS_ID = (byte)0x0B3;
	
	public static final byte SET_SIG = (byte)0xB4;
	public static final byte GET_SIG = (byte)0xB5;
	
	public static final byte TEST_VERIFY = (byte)0xB6;
	public static final byte GET_MODULUS = (byte)0xB7;
	
	public static final byte GET_FOR_SIG = (byte)0xC0;
	public static final byte VERIFY_KEYID = (byte)0xC1;
	public static final byte GET_DATA = (byte)0xC2;
	public static final byte SEND_DATA = (byte)0xC3;
	public static final byte FINAL_CONF = (byte)0xC4;
	
	public static final byte GET_AES_KEY = (byte)0xD0;
	
	public static final byte SET_LOC_KEY = (byte)0xC5;
	public static final byte SET_LOC = (byte)0xC6;
	public static final byte GET_SLOC = (byte)0xC7;
			
	private ProtocolApplet() {
		rnd = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
		rsa_cipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
		aes_cipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		
		/* The keys will be initialized here */
		RSAPublicKey pkey_card = (RSAPublicKey)KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		RSAPrivateKey skey_card = (RSAPrivateKey)KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_1024, false);
		rsaKeys = new KeyPair(pkey_card, skey_card);
		rsaKeys.genKeyPair();
		pkey_server = (RSAPublicKey)KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		pkey_server.setExponent(new byte[]{(byte)0x01, 0x00, (byte)0x01},  (short)0, (short)3);
		cardKey = (RSAPublicKey)KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		cardKey.setExponent(new byte[]{(byte)0x01, 0x00, (byte)0x01},  (short)0, (short)3);
		aeskey = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, (byte)0x0B,
				(byte)0x0C, 0x0D, 0x0E, 0x0F};
		
		sessionKey = (AESKey)KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_RESET, KeyBuilder.LENGTH_AES_128, false);
		
		register();
	}

	public static void install(byte bArray[], short bOffset, byte bLength)
			throws ISOException {
		new ProtocolApplet();
	}

	public void process(APDU apdu) throws ISOException {
		short lc, mLen;
		RSAPublicKey pkey_card;
		RSAPrivateKey skey;
		Signature sig;
		byte[] buffer = apdu.getBuffer();
		
		if(this.selectingApplet())
			return;
		if(buffer[ISO7816.OFFSET_CLA] != (byte)0xA0)
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		switch(buffer[ISO7816.OFFSET_INS]) {
		case SET_ID:
			apdu.setIncomingAndReceive();
			lc = (short)(0x00FF & buffer[ISO7816.OFFSET_LC]);
			id = new byte[lc];
			Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, id, (short)0, lc);
			break;
		case GET_ID:
			Util.arrayCopy(id, (short)0, buffer, (short)0, (short)id.length);
			apdu.setOutgoingAndSend((short)0, (short)id.length);
			break;
		case SET_KEY:
			apdu.setIncomingAndReceive();
			lc = (short)(0x00FF & buffer[ISO7816.OFFSET_LC]);
			pkey_server.setModulus(buffer, ISO7816.OFFSET_CDATA, lc);
			break;
		case GET_MODULUS:
			pkey_card = (RSAPublicKey)rsaKeys.getPublic();
			mLen = pkey_card.getModulus(buffer, (short)0);
			apdu.setOutgoingAndSend((short)0, mLen);
			break;
		case SET_SIG:
			apdu.setIncomingAndReceive();
			lc = (short)(0x00FF & buffer[ISO7816.OFFSET_LC]);
			if(lc != 0x0080) {
				ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
			}
			signature = new byte[lc];
			Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, signature, (short)0, lc);
			break;
		case GET_SIG:
			Util.arrayCopy(signature, (short)0, buffer, (short)0, (short)signature.length);
			apdu.setOutgoingAndSend((short)0, (short)signature.length);
			break;
		case TEST_VERIFY:
			sig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
			sig.init(pkey_server, Signature.MODE_VERIFY);
			pkey_card = (RSAPublicKey)rsaKeys.getPublic();
			temp = JCSystem.makeTransientByteArray((short)128, JCSystem.CLEAR_ON_RESET);
			mLen = pkey_card.getModulus(temp, (short)0);
			sig.update(temp, (short)0, mLen);
			try {
				boolean flag = sig.verify(id, (short)0, (short)id.length, signature, (short)0, (short)signature.length);
				if(!flag)
					ISOException.throwIt(ISO7816.SW_DATA_INVALID);
			}
			catch(CryptoException ex) {
				ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
			}
			break;
		case GET_MODULUS_ID:
			pkey_card = (RSAPublicKey)rsaKeys.getPublic();
			mLen = pkey_card.getModulus(buffer, (short)0);
			Util.arrayCopy(id, (short)0, buffer, (short)128, (short)id.length);
			apdu.setOutgoingAndSend((short)0, (short)(mLen+id.length));
			break;
		case GET_FOR_SIG:
			/* This apdu command is the first command.
			   Make sure that the next command which is sent is a VERIFY_KEYID command */
			apdu.setIncomingAndReceive();
			lc = (short)(0x00FF & buffer[ISO7816.OFFSET_LC]);
			temp = JCSystem.makeTransientByteArray(lc, JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, temp, (short)0, lc);
			break;
		case VERIFY_KEYID:
			apdu.setIncomingAndReceive();
			lc = (short)(0x00FF & buffer[ISO7816.OFFSET_LC]);
			sig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
			sig.init(pkey_server, Signature.MODE_VERIFY);
			try {
				boolean flag = sig.verify(buffer, ISO7816.OFFSET_CDATA, lc, temp, (short)0, (short)temp.length);
				if(!flag)
					ISOException.throwIt(ISO7816.SW_DATA_INVALID);
			}
			catch(CryptoException ex) {
				ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
			}
			/* Setup the public key of the card */
			cardKey.setModulus(buffer, ISO7816.OFFSET_CDATA, (short)128);
			forId = JCSystem.makeTransientByteArray((short)(lc-128), JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(buffer, (short)133, forId, (short)0, (short)(lc-128));
			break;
		case GET_DATA:
			apdu.setIncomingAndReceive();
			lc = (short)(0x00FF & buffer[ISO7816.OFFSET_LC]);
			temp = JCSystem.makeTransientByteArray(lc, JCSystem.CLEAR_ON_RESET);
			skey = (RSAPrivateKey)rsaKeys.getPrivate();
			rsa_cipher.init(skey, Cipher.MODE_DECRYPT);
			rsa_cipher.doFinal(buffer, ISO7816.OFFSET_CDATA, lc, temp, (short)0);
			if(Util.arrayCompare(forId, (short)0, temp, (short)8, (short)forId.length) != 0) {
				ISOException.throwIt(ISO7816.SW_DATA_INVALID);
			}
			nc = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(temp, (short)0, nc, (short)0, (short)8);
			break;
		case GET_SLOC:
			temp = JCSystem.makeTransientByteArray((short)25, JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(id, (short)0, temp, (short)0, (short)8);
			Util.arrayCopy(forId, (short)0, temp, (short)8, (short)8);
			Util.arrayCopy(loc, (short)0, temp, (short)16, (short)1);
			Util.arrayCopy(nr, (short)0, temp, (short)17, (short)8);
			rsa_cipher.init(pkey_server, Cipher.MODE_ENCRYPT);
			mLen = rsa_cipher.doFinal(temp, (short)0, (short)25, buffer, (short)0);
			apdu.setOutgoingAndSend((short)0, mLen);
		case SEND_DATA:
			nr = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_RESET);
			rnd.generateData(nr, (short)0, (short)nr.length);
			temp = JCSystem.makeTransientByteArray((short)32, JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(nc, (short)0, temp, (short)0, (short)nc.length);
			Util.arrayCopy(forId, (short)0, temp, (short)8, (short)forId.length);
			Util.arrayCopy(nr, (short)0, temp, (short)16, (short)nr.length);
			Util.arrayCopy(id, (short)0, temp, (short)24, (short)id.length);
			rsa_cipher.init(cardKey, Cipher.MODE_ENCRYPT);
			mLen = rsa_cipher.doFinal(temp, (short)0, (short)temp.length, buffer, (short)0);
			apdu.setOutgoingAndSend((short)0, mLen);
			break;
		case FINAL_CONF:
			apdu.setIncomingAndReceive();
			lc = (short)(0x00FF & buffer[ISO7816.OFFSET_LC]);
			temp = JCSystem.makeTransientByteArray(lc, JCSystem.CLEAR_ON_RESET);
			skey = (RSAPrivateKey)rsaKeys.getPrivate();
			rsa_cipher.init(skey, Cipher.MODE_DECRYPT);
			try {
				mLen = rsa_cipher.doFinal(buffer, ISO7816.OFFSET_CDATA, lc, temp, (short)0);
				if(mLen != (short)32)
					ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
				if(Util.arrayCompare(nr, (short)0, temp, (short)16, (short)nr.length) != 0) {
					ISOException.throwIt(ISO7816.SW_DATA_INVALID);
				}
			}
			catch(ArrayIndexOutOfBoundsException ex) {
				ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
			}
			try {
				sessionKey.setKey(temp, (short)0);
			}
			catch(ArrayIndexOutOfBoundsException ex) {
				ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
			}
			break;
		case GET_AES_KEY:
			Util.arrayCopy(aeskey, (short)0, buffer, (short)0, (short)aeskey.length);
			apdu.setOutgoingAndSend((short)0, (short)aeskey.length);
			break;
		case SET_LOC:
			apdu.setIncomingAndReceive();
			lc = (short)(0x00FF & buffer[ISO7816.OFFSET_LC]);
			loc = JCSystem.makeTransientByteArray(lc, JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, loc, (short)0, lc);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
		
	}

}
