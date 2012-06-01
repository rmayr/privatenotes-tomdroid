package at.fhooe.mcm.tomboyCrypt;

import java.io.File;
import java.util.List;

/**
 * a simple interface for encrypting and decrypting data with direct connection
 * to file access
 * @author Paul Klingelhuber
 */
public interface AsymmetricCryptoScheme extends CryptoScheme {
	
	/**
	 * decrypts a file with the given key
	 * @param file from where to read & decrypt
	 * @param key use this key
	 * @return the decrypted data
	 */
	byte[] decryptAsymFile(File file, byte[] key);
	
	/**
	 * writes data to a file in encrypted form
	 * @param file where to write it
	 * @param data what to encrypt
	 * @param key the key to use
	 * @param recipients a list of people for which to encrypt
	 * @return true if successful
	 */
	boolean writeAsymFile(File file, byte[] data, byte[] key, List<String> recipients);

}
