package com.gotrust.testrsa;

import javacard.security.*;
import javacardx.crypto.*;
import javacard.framework.*;

public class TestRSA extends Applet {
	Cipher rsaCipherPKCS1, rsaCipherNopad;
	Signature rsaSigPKCS1;
	
	RSAPrivateKey rsaPrvKey;
	RSAPrivateCrtKey privateKey, rsaCRTPrvKey;
	RSAPublicKey rsaPubKey, publicKey;
	KeyPair rsaKeyPair;
	RandomData random;

	byte bTestData[] = { (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
			(byte) 0x01, (byte) 0x01, (byte) 0x01 };
	
	byte bTransientArr[];

	/**
	 * Constructor of TestRSA.
	 */
	public TestRSA() {
		rsaCipherPKCS1 = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
		rsaSigPKCS1 = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
		rsaPrvKey = (RSAPrivateKey)KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, KeyBuilder.LENGTH_RSA_1024, false);
		rsaPubKey = (RSAPublicKey)KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, KeyBuilder.LENGTH_RSA_1024, false);
		rsaKeyPair = new KeyPair(rsaPubKey, rsaPrvKey);
		random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
		bTransientArr = JCSystem.makeTransientByteArray((short)128, JCSystem.CLEAR_ON_RESET);
	}
 
	/**
	 * Install method.
	 *
	 * @param array the array containing installation parameters
	 * @param offset the starting offset in array
	 * @param length the length in bytes of the parameter data in array
	 */
	public static void install(byte array[], short offset, byte length) throws ISOException {

		// create and register applet
		(new TestRSA()).register(array, (short)(offset + 1), array[offset]);
	}

	public boolean select(){
	
		//add your method call to be launched at the card power on
		return true;
	}

	public void deselect(){
	
		// add your code here
	}


	/**
	 * Processes an incoming APDU command.
	 *
	 * @param apdu the incoming <code>APDU</code> object
	 */
	public void process(APDU theApdu) {

		byte [] buffer=theApdu.getBuffer();
		short sLen, i;

		if (selectingApplet())
			return;

		try {
			switch (buffer[ISO7816.OFFSET_INS]){
			case (byte)0x00:	// Generate Key Pair
				rsaKeyPair.genKeyPair();
				rsaPrvKey = (RSAPrivateKey)rsaKeyPair.getPrivate();
				rsaPubKey = (RSAPublicKey)rsaKeyPair.getPublic();
				break;
			
			case (byte)0x01: // Make RSA Encryption. Take P1 as length of encryption data length.
				sLen = (short)(buffer[ISO7816.OFFSET_P1] & 0xFF);
				random.generateData(bTransientArr, (short)0, sLen);
			
				rsaCipherPKCS1.init(rsaPubKey, Cipher.MODE_ENCRYPT);
				sLen = rsaCipherPKCS1.doFinal(bTransientArr, (short)0, sLen, buffer, (short)0);
				theApdu.setOutgoingAndSend((short)0, sLen);
				
				break;

			case (byte)0x02: // Make RSA Encryption & Decryption. Take P1 as length of encryption data length.
				sLen = (short)(buffer[ISO7816.OFFSET_P1] & 0xFF);
				random.generateData(bTransientArr, (short)0, sLen);
			
				rsaCipherPKCS1.init(rsaPubKey, Cipher.MODE_ENCRYPT);
				sLen = rsaCipherPKCS1.doFinal(bTransientArr, (short)0, sLen, buffer, (short)0);
				
				rsaCipherPKCS1.init(rsaPrvKey, Cipher.MODE_DECRYPT);
				sLen = rsaCipherPKCS1.doFinal(buffer, (short)0, sLen, buffer, (short)0);
				theApdu.setOutgoingAndSend((short)0, sLen);
				
				break;
			
			case (byte)0x03:
				generateKeyPair((short)1024);
				break;
			default:
					ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
			}
			
		} catch (CryptoException ex) {
			ISOException.throwIt((short)0x6901);
		} catch (ArrayIndexOutOfBoundsException ex) {
			ISOException.throwIt((short)0x6902);
		} catch (NullPointerException ex) {
			ISOException.throwIt((short)0x6903);
		} catch (Exception ex) {
			ISOException.throwIt((short)0x69FF);
		}
	}
	
	public void generateKeyPair(short keyLength) {
		privateKey = (RSAPrivateCrtKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_CRT_PRIVATE, keyLength, false);
		publicKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, keyLength, false);
		KeyPair keyPair = new KeyPair(publicKey, privateKey);
		keyPair.genKeyPair();
	}

} //end class

