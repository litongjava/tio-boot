package com.litongjava.tio.utils.digest;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1Utils {
	
	private final static String ALGORITHM = "SHA-1";
	
	public static byte[] digest(byte[] decript) {
		try {
			MessageDigest digest = java.security.MessageDigest.getInstance(ALGORITHM);
			digest.update(decript);
			return digest.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static byte[] digest(String decript) {
		return digest(decript.getBytes());
	}

	public static String digest(String decript, Charset encoding) {
		byte[] array = digest(decript);
		return new String(array, encoding);
	}
}
