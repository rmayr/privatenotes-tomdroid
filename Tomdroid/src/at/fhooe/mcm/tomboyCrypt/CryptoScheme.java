package at.fhooe.mcm.tomboyCrypt;

import java.io.File;

/**
 * a simple interface for encrypting and decrypting data with direct connection
 * to file access
 * @author Paul Klingelhuber
 */
public interface CryptoScheme {
	
	/**
	 * decrypts a file with the given key
	 * @param file from where to read & decrypt
	 * @param key use this key
	 * @return the decrypted data
	 */
	public byte[] decryptFile(File file, byte[] key);
	
	/**
	 * writes data to a file in encrypted form
	 * @param file where to write it
	 * @param data what to encrypt
	 * @param key the key to use
	 * @return true if successful
	 */
	public boolean writeFile(File file, byte[] data, byte[] key);

}
