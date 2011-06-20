package at.fhooe.mcm.tomboyCrypt;

import java.io.File;
import java.io.FileFilter;

public class Test {
	
	private static CryptoScheme schemeToTest = null;
	
	public static void main(String[] args) {
		//schemeToTest = new CryptoSchemeV0();
		schemeToTest = new CryptoSchemeV1();
		
		byte[] key = "test".getBytes();
		
		try {
			System.out.println("running tests");
			testWriteAndRead("./resources/test.note", "<xml>hello, i am a test note<someXmlStuff>bla</someXmlStuff></xml>", key);
		
			testTomboyNotes(key);
			System.out.println("all tests finished");
		} catch (Exception _e) {
			_e.printStackTrace();
		}
	}
	
	private static void testWriteAndRead(String _targetFile, String _testData, byte[] _testKey) throws Exception{
		File f = new File(_targetFile);
		if (f.exists())
			f.delete();
			
		if (!schemeToTest.writeFile(f, _testData.getBytes(), _testKey))
			throw new Exception("error while writing file");
		
		byte[] decrypted = schemeToTest.decryptFile(f, _testKey);
		if (decrypted == null)
			throw new Exception("error while decrypting file");
		
		if (!new String(decrypted).equals(_testData)) {
			throw new Exception("decrypted version differs from oroginal");
		}
		System.out.println("write read test successful");
	}
	
	private static void testTomboyNotes(byte[] _password) {
		int count = checkDirectory("./resources/fromTomboy/", _password);
		System.out.println();
		System.out.println("checked directory, " + count + " files were successfully decrypted");
	
	}
	
	private static int checkDirectory(String _path, byte[] _key) {
		File dir = new File(_path);
		if (!dir.isDirectory())
			return -1;
		
		File[] noteFiles = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".note") || pathname.getName().endsWith(".xml");
			}});
		
		int okCount = 0;
		for (File note : noteFiles) {
			if (schemeToTest.decryptFile(note, _key) != null)
				okCount++;
		}
		return okCount;
	}

}
