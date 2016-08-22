/* Patient */
package protocolapplet;

import javacard.framework.APDU;
import javacard.framework.Applet;
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
	
	/* Public key of the reader */
	private RSAPublicKey readerKey;
	
	/* Id of foreign entity */
	private byte[] forId;
	
	public Cipher rsa_cipher, aes_cipher;
	
	private AESKey sessionKey;
	
	/* AES Key stored in a byte array */
	private byte[] aeskey;
	
	public static final byte SET_ID = (byte)0xB0;
	public static final byte GET_ID = (byte)0xB1;
	
	public static final byte SET_KEY = (byte)0xB2;
	public static final byte GET_MODULUS_ID = (byte)0xB3;
	
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

	protected ProtocolApplet() {
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
		readerKey = (RSAPublicKey)KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		readerKey.setExponent(new byte[]{(byte)0x01, 0x00, (byte)0x01},  (short)0, (short)3);
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
		// TODO Auto-generated method stub
		short lc, mLen;
		RSAPublicKey pkey_card;
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
			/* This apdu command is the first command,.
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
			/* Setup the public key of the reader */
			readerKey.setModulus(buffer, ISO7816.OFFSET_CDATA, (short)128);
			forId = JCSystem.makeTransientByteArray((short)(lc-128), JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(buffer, (short)133, forId, (short)0, (short)(lc-128));
			break;
		case SEND_DATA:
			nc = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_RESET);
			rnd.generateData(nc, (short)0, (short)nc.length);
			temp = JCSystem.makeTransientByteArray((short)16, JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(nc, (short)0, temp, (short)0, (short)nc.length);
			Util.arrayCopy(id, (short)0, temp, (short)8, (short)id.length);
			rsa_cipher.init(readerKey, Cipher.MODE_ENCRYPT);
			mLen = rsa_cipher.doFinal(temp, (short)0, (short)temp.length, buffer, (short)0);
			apdu.setOutgoingAndSend((short)0, mLen);
			break;
		case GET_SLOC:
			temp = JCSystem.makeTransientByteArray((short)25, JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(id, (short)0, temp, (short)0, (short)8);
			Util.arrayCopy(forId, (short)0, temp, (short)8, (short)8);
			Util.arrayCopy(loc, (short)0, temp, (short)16, (short)1);
			Util.arrayCopy(nc, (short)0, temp, (short)17, (short)8);
			rsa_cipher.init(pkey_server, Cipher.MODE_ENCRYPT);
			mLen = rsa_cipher.doFinal(temp, (short)0, (short)25, buffer, (short)0);
			apdu.setOutgoingAndSend((short)0, mLen);
			break;
		case GET_DATA:
			apdu.setIncomingAndReceive();
			lc = (short)(0x00FF & buffer[ISO7816.OFFSET_LC]);
			temp = JCSystem.makeTransientByteArray(lc, JCSystem.CLEAR_ON_RESET);
			RSAPrivateKey skey = (RSAPrivateKey)rsaKeys.getPrivate();
			rsa_cipher.init(skey, Cipher.MODE_DECRYPT);
			mLen = rsa_cipher.doFinal(buffer, ISO7816.OFFSET_CDATA, lc, temp, (short)0);
			if(Util.arrayCompare(id, (short)0, temp, (short)8, (short)id.length) != 0) {
				ISOException.throwIt(ISO7816.SW_DATA_INVALID);
			}
			if(Util.arrayCompare(nc, (short)0, temp, (short)0, (short)nc.length) != 0) {
				ISOException.throwIt(ISO7816.SW_DATA_INVALID);
			}
			if(Util.arrayCompare(forId, (short)0, temp, (short)24, (short)forId.length) != 0) {
				ISOException.throwIt(ISO7816.SW_DATA_INVALID);
			}
			nr = JCSystem.makeTransientByteArray((short)8, JCSystem.CLEAR_ON_RESET);
			Util.arrayCopy(temp, (short)16, nr, (short)0, (short)8);
			break;
		case FINAL_CONF:
			temp = JCSystem.makeTransientByteArray((short)32, JCSystem.CLEAR_ON_RESET);
			rnd.generateData(temp, (short)0, (short)16);
			Util.arrayCopy(nr, (short)0, temp, (short)16, (short)nr.length);
			Util.arrayCopy(forId, (short)0, temp, (short)24, (short)forId.length);
			try {
				rsa_cipher.init(readerKey, Cipher.MODE_ENCRYPT);
				mLen = rsa_cipher.doFinal(temp, (short)0, (short)32, buffer, (short)0);
				apdu.setOutgoingAndSend((short)0, mLen);
				break;
			}
			catch(CryptoException ex) {
				ISOException.throwIt(ISO7816.SW_DATA_INVALID);
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
