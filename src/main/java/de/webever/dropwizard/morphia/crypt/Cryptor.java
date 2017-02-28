package de.webever.dropwizard.morphia.crypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.crypto.stream.CryptoInputStream;
import org.apache.commons.crypto.stream.CryptoOutputStream;

public class Cryptor {
	static SecretKeySpec key = new SecretKeySpec(getUTF8Bytes("1234567890123456"), "AES");
	static IvParameterSpec iv = new IvParameterSpec(getUTF8Bytes("1234567890123456"));
	static Properties properties = new Properties();
	static String transform = "AES/CBC/PKCS5Padding";

	/**
	 * Initiated the encrypter with a new seed.
	 * 
	 * @param seed
	 *            the seed.
	 */
	public static void initCrypt(String seed) {
		key = new SecretKeySpec(getUTF8Bytes(seed), "AES");
		iv = new IvParameterSpec(getUTF8Bytes(seed));
	}

	/**
	 * Encrypts a UTF-8 string. The resulting encrypted string uses ISO 8859-1
	 * (ISO-LATIN-1).
	 * 
	 * @param input
	 *            the string to encrypt
	 * @return the encrypted string
	 * @throws IOException
	 *             could be thrown by writing buffer.
	 */
	public static String encrypt(String input) throws IOException {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (CryptoOutputStream cos = new CryptoOutputStream(transform, properties, outputStream, key, iv)) {
			cos.write(getUTF8Bytes(input));
			cos.flush();
		}
		return new String(outputStream.toByteArray(), StandardCharsets.ISO_8859_1);
	}

	/**
	 * Decrypts a ISO 8859-1 (ISO-LATIN-1) string. The resulting decryted string
	 * uses UTF-8.
	 * 
	 * @param input
	 *            the string to decrypt
	 * @return the decrypted string
	 * @throws IOException
	 *             could be thrown by writing buffer.
	 */
	public static String decrypt(String input) throws IOException {
		InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.ISO_8859_1));

		try (CryptoInputStream cis = new CryptoInputStream(transform, properties, inputStream, key, iv)) {
			byte[] decryptedData = new byte[1024];
			int decryptedLen = 0;
			int i;
			while ((i = cis.read(decryptedData, decryptedLen, decryptedData.length - decryptedLen)) > -1) {
				decryptedLen += i;
			}
			return new String(decryptedData, 0, decryptedLen, StandardCharsets.UTF_8);
		}

	}

	private static byte[] getUTF8Bytes(String input) {
		return input.getBytes(StandardCharsets.UTF_8);
	}
}
