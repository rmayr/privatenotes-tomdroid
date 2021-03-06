package at.fhooe.mcm.tomboyCrypt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CryptoSchemeV0 implements CryptoScheme {
	
	public byte[] decryptFile(File _file, byte[] _key) {
		FileInputStream fin = null;
		byte[] referenceKeyHash = CryptWrapper.hashData(_key);
		try {
			System.out.println();
			System.out.println("checking file " + _file.getAbsolutePath());
			fin = new FileInputStream(_file);
			byte[] datetime = new byte[8];
			byte[] keyhash = new byte[32];
			byte[] controlHash = new byte[32];
			
			// read "header"
			if (fin.read(datetime) != datetime.length)
				throw new Exception("file may be corrupt");
			if (fin.read(keyhash) != keyhash.length)
				throw new Exception("file may be corrupt");
			// no longer, because control hash is now part of the encrypted data
//			if (fin.read(controlHash) != controlHash.length)
//				throw new Exception("file may be corrupt");
			
			if (!Util.byteArrayEquals(referenceKeyHash, keyhash)) {
				System.err.println("wrong password");
				return null;
			}
			
			// read rest of file
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int data = fin.read();
			while (data>=0) {
				bout.write(data);
				data = fin.read();
			}
			byte[] encryptedData = bout.toByteArray();
			
			// decrypt:
			byte[] decryptedData = CryptWrapper.decrypt(encryptedData, _key);
			// get security hash:
			System.arraycopy(decryptedData, 0, controlHash, 0, controlHash.length);
			// put rest of data:
			byte[] fullDecrypt = decryptedData;
			 // new byte array for padded data without security hash
			decryptedData = new byte[fullDecrypt.length - 32];
			System.arraycopy(fullDecrypt, 32, decryptedData, 0, decryptedData.length);			
			
			// check security hash (of content etc)
			bout = new ByteArrayOutputStream();
			byte[] fileNameBytes = _file.getName().getBytes();
			bout.write(datetime);
			bout.write(fileNameBytes);
			bout.write(decryptedData);
			byte[] dataHash = CryptWrapper.hashData(bout.toByteArray());
			if (!Util.byteArrayEquals(controlHash, dataHash)) {
				System.err.println("hashes don't match! maybe somebody tampered with the file!");
				return null;
			}
			
			System.out.println("note ok");
			
			long dateTimeLong = Util.arr2long(datetime, 0);
			@SuppressWarnings("unused")
			byte[] temp = Util.longToByteArray(dateTimeLong);
			System.out.println("long value: " + dateTimeLong);
			Date date = new Date(dateTimeLong * 1000);
			
			System.out.println("proven file date: " + SimpleDateFormat.getInstance().format(date)); // because it's stored in utc
			
			// get file contents:
			byte[] actualData = Util.getUnpaddedData(decryptedData);
			//String note = new String(actualData);
			//System.out.println("---==== NOTE ====---");
			//System.out.println(note);
			//System.out.println(" END OF NOTE ====---");
			
			
			return actualData;
		} catch (Exception _e) {
			System.err.println("error");
			_e.printStackTrace();
			return null;
		} finally {
			Util.tryClose(fin);
		}
	}
	
	public boolean writeFile(File _file, byte[] _data, byte[] _key) {
		FileOutputStream fout = null;
		try {
			System.out.println();
			System.out.println("writing file " + _file.getAbsolutePath());
			fout = new FileOutputStream(_file);
			byte[] datetime = Util.longToByteArray(new Date().getTime() / 1000);
			assert(datetime.length == 8);
			byte[] keyhash = CryptWrapper.hashData(_key);
			
			// add padding to data
			_data = Util.padDataToMultipleOf(_data, 16);
			
			// calculate security hash (of content etc)
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			byte[] fileNameBytes = _file.getName().getBytes();
			bout.write(datetime);
			bout.write(fileNameBytes);
			bout.write(_data);
			byte[] controlHash = CryptWrapper.hashData(bout.toByteArray());
			
			// compose byte-array to be encrypted:
			byte[] encryptMe = new byte[controlHash.length + _data.length];
			System.arraycopy(controlHash, 0, encryptMe, 0, controlHash.length);
			System.arraycopy(_data, 0, encryptMe, controlHash.length, _data.length);
			
			byte[] encryptedData = CryptWrapper.encrypt(encryptMe, _key);
			
			// read "header"
			fout.write(datetime);
			fout.write(keyhash);
			fout.write(encryptedData);
			System.out.println("file successfully written");
			
			return true;
		} catch (Exception _e) {
			System.err.println("error");
			_e.printStackTrace();
			return false;
		} finally {
			Util.tryClose(fout);
		}
	}

}
