package at.fhooe.mcm.tomboyCrypt;

import java.io.ByteArrayOutputStream;

import net.sf.microlog.Logger;

import org.openuat.util.Hash;
import org.openuat.util.SimpleBlockCipher;

/**
 * a wrapper (or in essance more a util) that provides some typical
 * crypto related functionality like hashing
 *
 */
public class CryptWrapper {
	
	/**
	 * creates the SHA256 hash of a byte array
	 * @param _data
	 * @return
	 */
	public static byte[] hashData(byte[] _data) {
		try {
			return Hash.normalSHA256(_data, true);
		} catch (Exception _e) {
			Logger.getLogger("Crypto").warn("could not hash data!");
			return null;
		}
	}
	
	/**
	 * creates the SHA256 hash of a byte array
	 * 
	 * the data is salted before getting hashed where the salt is appended to the
	 * original data
	 * 
	 * @param _data
	 * @param _salt the salt data (typically something random)
	 * @return
	 */
	public static byte[] hashDataWithSalt(byte[] _data, byte[] _salt) {
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream(_data.length + _salt.length);
			bout.write(_data);
			bout.write(_salt);
			byte[] buffer = bout.toByteArray();
			
			byte[] result = Hash.normalSHA256(buffer, true);

			// overwrite buffer data
			bout.reset();
			for (int i=0; i<bout.size(); i++) {
				bout.write(0);
				if (buffer.length > i)
					buffer[i] = 0;
			}
			
			return result;
		} catch (Exception _e) {
			Logger.getLogger("Crypto").warn("could not hash data!");
			return null;
		}
	}

	public static byte[] decrypt(byte[] _data, byte[] _pw) throws Exception {
		assert (_data.length % 16 == 0);
		SimpleBlockCipher cyph = new SimpleBlockCipher(false);
		if (_data.length == 16) {
			return cyph.decrypt(_data, -1, _pw);
		} else {
			return cyph.decrypt(_data, (_data.length - 16) * 8, _pw);
		}
	}

	public static byte[] encrypt(byte[] _data, byte[] _pw) throws Exception {
		SimpleBlockCipher cyph = new SimpleBlockCipher(false);
		return cyph.encrypt(_data, -1, _pw);
	}
}
