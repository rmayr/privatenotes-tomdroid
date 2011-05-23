package at.fhooe.mcm.tomboyCrypt;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public class Util {

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

	public static byte[] longToByteArray(long l) {
		byte[] b = new byte[8];
		for (int i = 0; i < 8; i++) {
			b[i] = (byte) (l >>> (i * 8));
		}
		return b;
	}

	public static byte[] intToByteArray(int n) {
		byte[] b = new byte[4];
		for (int i = 0; i < 4; i++) {
			b[i] = (byte) (n >>> (i * 8));
		}
		return b;
	}

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
	
	public static void tryClose(InputStream _is) {
		if (_is != null)
			try {
				_is.close();
			} catch (Exception _e) {}
	}
	
	public static void tryClose(OutputStream _is) {
		if (_is != null)
			try {
				_is.close();
			} catch (Exception _e) {}
	}

}
