package at.fhooe.mcm.tomboyCrypt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * a null implementation which does no cryptography at all
 * this is only allowed for testing!
 * @author Paul Klingelhuber
 */
public class NullCryptoScheme implements AsymmetricCryptoScheme
{
	
	/**
	 * {@inheritDoc}
	 */
	public byte[] decryptFile(File file, byte[] key) {
		try {
			FileInputStream fin = new FileInputStream(file);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int b = fin.read(buf);
			while (b >= 0) {
				buffer.write(buf, 0, b);
				b = fin.read(buf);
			}
			fin.close();
			byte[] result = buffer.toByteArray();
			buffer.close();
			return result;
		} catch (Exception _e) {
			_e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean writeFile(File file, byte[] data, byte[] key) {
		try {
			FileOutputStream fout = new FileOutputStream(file, false);
			fout.write(data);
			fout.flush();
			fout.close();
			return true;
		} catch (Exception _e) {
			return false;
		}
		
	}

	/**
	 * {@inheritDoc}
	 * reads file contents
	 */
	public byte[] decryptAsymFile(File file, byte[] key) {
		return decryptFile(file, key);
	}

	/**
	 * {@inheritDoc}
	 * writes plain file
	 */
	public boolean writeAsymFile(File file, byte[] data, byte[] key,
			List<String> recipients) {
		return writeFile(file, data, key);
	}

}
