/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM - Initial implementation
 ******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * <P>Encrypts or decrypts a sequence of bytes. The bytes are decrypted
 * by supplying the same password that was given when the bytes were
 * encrypted.
 * <P>Here is an example showing how to encrypt and then decrypt the
 * string "Hello, world!" using the password "music":
 * <pre>
 *     String password = "music";
 *     byte[] data = "Hello, world!".getBytes("UTF8");
 *
 *     // Encrypt
 *     Cipher cipher = new Cipher(ENCRYPT_MODE, password);
 *     byte[] encrypted = cipher.cipher(data);
 *
 *     // Decrypt
 *     cipher = new Cipher(DECRYPT_MODE, password);
 *     byte[] decrypted = cipher.cipher(encrypted);
 * </pre>
 */
public class Cipher {
	public static final int DECRYPT_MODE = -1;
	public static final int ENCRYPT_MODE = 1;

	private int mode = 0;
	private byte[] password = null;
	
	//the following fields are used for generating a secure byte stream
	//used by the decryption algorithm
	private byte[] byteStream;
	private int byteStreamOffset;
	private long counter = 0;
	private MessageDigest digest;
	private byte[] toDigest;
/**
 * Initializes the cipher with the given mode and password. This method
 * must be called first (before any encryption of decryption takes
 * place) to specify whether the cipher should be in encrypt or decrypt
 * mode and to set the password.
 *
 * @param mode
 * @param password
 */
public Cipher (int mode, String passwordString){
	this.mode = mode;
	try {
		this.password = passwordString.getBytes("UTF8");//$NON-NLS-1$
	} catch (UnsupportedEncodingException e) {
	}
	toDigest = new byte[password.length+8];
	System.arraycopy(password, 0, toDigest, 8, password.length);

}
/**
 * Encrypts or decrypts (depending on which mode the cipher is in) the
 * given data and returns the result.
 *
 * @param data
 * @return     the result of encrypting or decrypting the given data
 */
public byte[] cipher(byte[] data) throws Exception {
	return transform(data, 0, data.length, mode);
}
/**
 * Encrypts or decrypts (depending on which mode the cipher is in) the
 * given data and returns the result.
 *
 * @param data the byte array containg the given data
 * @param off  the index of the first byte in the given byte array
 *             to be transformed
 * @param len  the index after the last byte in the given byte array
 *             to be transformed
 * @return     the result of encrypting or decrypting the given data
 */
public byte[] cipher(byte[] data, int off, int len) throws Exception {
	return transform(data, off, len, mode);
}
/**
 * Encrypts or decrypts (depending on which mode the cipher is in) the
 * given byte and returns the result.
 *
 * @param datum the given byte
 * @return      the result of encrypting or decrypting the given byte
 */
public byte cipher(byte datum) throws Exception {
	byte[] data = { datum };
	return cipher(data)[0];
}
/**
 * Generates a secure stream of bytes based on the input seed.
 * This routine works by combining the input seed with a counter,
 * and then computing the SHA-1 hash of those bytes.
 */
private byte[] generateBytes() throws Exception {
	if (digest == null) {
		digest = MessageDigest.getInstance("SHA"); //$NON-NLS-1$
	}
	//convert counter to bytes
	for (int i = 0; i < 8; i++) {
		toDigest[i] = (byte)((counter >> (8*i)) & 0x00000000000000FFL);
	}
	counter++;
	return digest.digest(toDigest); 
}
/**
 * Returns a stream of cryptographically secure bytes of the given length.
 * The result is deterministically based on the input seed (password).
 */
private byte[] nextRandom(int length) throws Exception {
	byte[] nextRandom = new byte[length];
	int nextRandomOffset = 0;
	while (nextRandomOffset < length) {
		if (byteStream == null || byteStreamOffset >= byteStream.length) {
			byteStream = generateBytes();
			byteStreamOffset = 0;
		}
		nextRandom[nextRandomOffset++] = byteStream[byteStreamOffset++];
	}
	return nextRandom;
}
private byte[] transform(byte[] data, int off, int len, int mode) throws Exception {
	byte[] result = nextRandom(len);
	for (int i = 0; i < len; ++i) {
		result[i] = (byte) (data[i + off] + mode * result[i]);
	}
	return result;
}
}
