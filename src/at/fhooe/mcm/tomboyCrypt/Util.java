package at.fhooe.mcm.tomboyCrypt;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public class Util {
	
	public static final String SECURITY_PROVIDER_ID = "SC";

	public static void main(String[] args) throws Exception {
		// TESTS
		int count = 0;
		
		for (int i=0; i< 100; i++) {
			StringBuffer s = new StringBuffer();
			// create test string
			for(int j=0; j<i; j++)
				s.append((char)('A' + (Math.random()*20)));
			byte[] padded = padDataToMultipleOf(s.toString().getBytes(), 16);
			byte[] unpadded = getUnpaddedData(padded);
			if (!new String(unpadded).equals(s.toString()))
				throw new Exception("results don't match!");
		}
		
		count=0;
		for (int i = Integer.MIN_VALUE + 10; i < Integer.MAX_VALUE - 10; i += 33333) {
			if (arr2int(intToByteArray(i), 0) != i) {
				throw new Exception(
						"ERROR, doesn't match after converting back!");
			}
			count++;
		}
		System.out.println(count + " int tests done");

		count = 0;
		byte[] a;
		long b;
		for (long i = Long.MIN_VALUE + 10; i < Long.MAX_VALUE - 10; i += 333333333) {
			a = longToByteArray(i);
			b = arr2long(a, 0);
			if (b != i) {
				throw new Exception(
						"ERROR, doesn't match after converting back! " + i
								+ " but got " + String.valueOf(a));
			}
			count++;
		}
		System.out.println(count + " long tests done");
	}
	
	/**
	 * reads a files contents into a byte array
	 * @param f
	 * @return
	 * @throws IOException
	 */
	public static byte[] readFile(File f) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
		byte[] buf = new byte[1024];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int read = 0;
		do {
			read = in.read(buf);
			if (read > -1) {
				out.write(buf, 0, read);
			}
		} while (read >= 0);
		
		return out.toByteArray();
	}
	
	/**
	 * writes a byte array as the contents of a file
	 * @param f
	 * @param data
	 * @return
	 * @throws IOException
	 */
	public static boolean writeFile(File f, byte[] data) throws IOException {
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(f);
			fout.write(data);
			return true;
		} catch (IOException ioe) {
			throw ioe;
		} finally {
			tryClose(fout);
		}
	}

	/**
	 * removes the padding of a byte array, where the first 4 bytes are the length info
	 * (an encoded int), followed by that much useful bytes, followed by some padding
	 * this method returns ONLY the real (useful) data
	 * @param _data
	 * @return
	 */
	public static byte[] getUnpaddedData(byte[] _data) {
		if (_data.length < 4)
			throw new InvalidParameterException(
					"array too short for length information");
		int length = arr2int(_data, 0);
		if (_data.length < length + 4)
			throw new InvalidParameterException(
					"array not long enough to contain that much data");
		byte[] result = new byte[length];
		System.arraycopy(_data, 4, result, 0, length);
		return result;
	}

	/**
	 * writes the given data into a byte array which has a length which is a multiple of something
	 * the first 4 bytes of the result array are always the length info, but they are also
	 * included in the length, so the length of the _whole_ byte array is %_multipleof == 0, not
	 * only the data + padding (but lengthinfo + data + padding)
	 * @param _data
	 * @param _multipleOf
	 * @return
	 */
	public static byte[] padDataToMultipleOf(byte[] _data, int _multipleOf) {
		int tooMuch = (_data.length + 4) % _multipleOf;
		int length = 4 + _data.length + (_multipleOf - tooMuch);
		assert (length % _multipleOf == 0);
		byte[] result = new byte[length];
		byte[] lengthInfo = intToByteArray(_data.length);

		System.arraycopy(lengthInfo, 0, result, 0, 4);
		System.arraycopy(_data, 0, result, 4, _data.length);

		return result;
	}

	public static long arr2long(byte[] arr, int start) {
		int i = 0;
		int len = 4;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++) {
			tmp[cnt] = arr[i];
			cnt++;
		}
		long accum = 0;
		i = 0;
		for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {
			accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
			i++;
		}
		return accum;
	}

	/**
	 * reads an integer from a byte array
	 * @param arr
	 * @param start
	 * @return
	 */
	public static int arr2int(byte[] arr, int start) {
		int i = 0;
		int len = 4;
		int cnt = 0;
		byte[] tmp = new byte[len];
		for (i = start; i < (start + len); i++) {
			tmp[cnt] = arr[i];
			cnt++;
		}
		int accum = 0;
		i = 0;
		for (int shiftBy = 0; shiftBy < 32; shiftBy += 8) {
			accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
			i++;
		}
		return accum;
	}

	/**
	 * writes a long value to a byte array
	 * @param l
	 * @return
	 */
	public static byte[] longToByteArray(long l) {
		byte[] b = new byte[8];
		for (int i = 0; i < 8; i++) {
			b[i] = (byte) (l >>> (i * 8));
		}
		return b;
	}

	/**
	 * writes an int into a byte array (encodes it)
	 * @param n
	 * @return
	 */
	public static byte[] intToByteArray(int n) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			b[i] = (byte) (n >>> (i * 8));
		}
		return b;
	}

	/**
	 * checks if the contents of two byte arrays are equal
	 * @param _b1
	 * @param _b2
	 * @return
	 */
	public static boolean byteArrayEquals(byte[] _b1, byte[] _b2) {
		if (_b1 == null || _b2 == null)
			return false;
		if (_b1.length != _b2.length)
			return false;

		for (int i = 0; i < _b1.length; i++)
			if (_b1[i] != _b2[i])
				return false;

		return true;
	}
	
	/**
	 * tries to close an inputstream and ignores exceptions
	 * @param _is
	 */
	public static void tryClose(InputStream _is) {
		if (_is != null)
			try {
				_is.close();
			} catch (Exception _e) {}
	}
	
	/**
	 * tries to close an outputstreams and ignores exceptions
	 * @param _is
	 */
	public static void tryClose(OutputStream _is) {
		if (_is != null)
			try {
				_is.close();
			} catch (Exception _e) {}
	}
	
	public static byte[] readStreamFully(InputStream is) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[1024];
			int read = is.read(buf);
			while (read >= 0) {
				bout.write(buf, 0, read);
				read = is.read(buf);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return bout.toByteArray();
	}

}
