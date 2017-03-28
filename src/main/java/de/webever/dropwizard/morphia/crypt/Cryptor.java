package de.webever.dropwizard.morphia.crypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Crypt;

import de.webever.dropwizard.morphia.model.CryptModel;

public class Cryptor {
    private static SecretKeySpec key = new SecretKeySpec(getUTF8Bytes("1234567890123456"), "AES");
    private static IvParameterSpec iv = new IvParameterSpec(getUTF8Bytes("1234567890123456"));
    private static String transform = "AES/CBC/PKCS5Padding";
    private static String seed = "1234";

    /**
     * Initiated the encrypter with a new seed.
     * 
     * @param seed
     *            the seed.
     * @throws IOException
     *             could be thrown by writing buffer.
     */
    public static void initCrypt(String seed, String apiPackageName, boolean enabled) throws IOException {
	key = new SecretKeySpec(getUTF8Bytes(seed), "AES");
	iv = new IvParameterSpec(getUTF8Bytes(seed));
	Cryptor.seed = seed;
	CryptModel.init(apiPackageName, enabled);
	decrypt(encrypt("Text"));
    }

    /**
     * Encrypt a string.
     * 
     * @param value
     *            the string to encrypt
     * @return the encrypted string
     * @throws IOException
     *             something went massively wrong.
     */
    public static String encrypt(String value) throws IOException {
	try {

	    Cipher cipher = Cipher.getInstance(transform);
	    cipher.init(Cipher.ENCRYPT_MODE, key, iv);

	    byte[] encrypted = cipher.doFinal(getUTF8Bytes(value));
	    return Base64.encodeBase64String(encrypted);
	} catch (Exception ex) {
	    throw new IOException(ex);
	}

    }

    /**
     * Descrypts a String
     * 
     * @param encrypted
     *            the encrypted string
     * @return the decrypted string
     * @throws IOException
     *             something went massively wrong.
     */
    public static String decrypt(String encrypted) throws IOException {
	try {

	    Cipher cipher = Cipher.getInstance(transform);
	    cipher.init(Cipher.DECRYPT_MODE, key, iv);

	    byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));
	    return new String(original, StandardCharsets.UTF_8);
	} catch (Exception ex) {
	    throw new IOException(ex);
	}
    }

    private static byte[] getUTF8Bytes(String input) {
	return input.getBytes(StandardCharsets.UTF_8);
    }

    public static String passwordEncrypt(String password) {
	return Crypt.crypt(password, seed);
    }
}
