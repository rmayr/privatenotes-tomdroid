package at.fhooe.mcm.tomboyCrypt;

import java.io.File;
import java.io.FileInputStream;

public interface CryptoScheme {
	
	public byte[] decryptFile(File _file, byte[] _key);
	
	public boolean writeFile(File _file, byte[] _data, byte[] _key);

}
