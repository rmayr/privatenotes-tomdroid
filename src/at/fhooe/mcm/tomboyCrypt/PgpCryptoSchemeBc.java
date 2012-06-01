package at.fhooe.mcm.tomboyCrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.List;

import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.privatenotes.util.SecurityUtil;

import android.os.Environment;

/**
 * An imlementation of an asymmetric crypto scheme using the BouncyCastle API
 * what this can/can't do: - doesn't need APG installed, no weird
 * activity-switching etc! - because of that, we can't handle keys that well, so
 * we only support one key (the owner's one) - therefore (last point) we only
 * support asym decryption, no encryption for multiple recipients (we don't have
 * their keys!)
 * 
 * @author Paul Klingelhuber
 * 
 */
public class PgpCryptoSchemeBc implements AsymmetricCryptoScheme {
	
	public PgpCryptoSchemeBc() {
		if (Security.getProvider(Util.SECURITY_PROVIDER_ID) == null) {
			Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		}
	}

	public byte[] decryptFile(File file, byte[] key) {
		try {
			InputStream fin = new FileInputStream(file);
			InputStream clear = PgpSymmetric.decrypt(fin,
					new String(key).toCharArray());
			System.out.println("file successfully decryptd");
			return Util.readStreamFully(clear);
		} catch (Exception e) {
			System.err.println("error while decrypting: "
					+ e.getClass().getSimpleName() + " " + e.getMessage() + " ");
			e.printStackTrace(System.err);
			return null;
		}
	}

	public boolean writeFile(File file, byte[] data, byte[] key) {
		FileOutputStream fout = null;
		try {
			fout = new FileOutputStream(file);
			byte[] encrypted = PgpSymmetric.encrypt(data,
					new String(key).toCharArray(), PGPLiteralData.CONSOLE,
					PGPEncryptedDataGenerator.AES_256, false);
			fout.write(encrypted);
			fout.close();
			System.out.println("successfully encrypted file");
			return true;
		} catch (Exception e) {
			tryClose(fout);
			tryRemove(file);
			System.err.println("error while encrypting: "
					+ e.getClass().getSimpleName() + " " + e.getMessage());
			return false;
		}
	}
	
	private static void tryClose(OutputStream out) {
		if (out != null) {
			try {
				out.close();
			}
			catch (Exception e) {
				// ignored
			}
		}
	}
	
	/**
	 * try to remove/delete a file
	 * @param file
	 */
	private static void tryRemove(File file) {
		try {
			file.delete();
		} catch (Exception e) {
			// ignored
		}
	}
	
	public boolean isKeyFileExisting() {
		File extStore = Environment.getExternalStorageDirectory();
		return new File(extStore, "/pgp/secret.key").exists();
	}
	
	private FileInputStream getKeyFile() throws FileNotFoundException {
		File extStore = Environment.getExternalStorageDirectory();
		return new FileInputStream(new File(extStore, "/pgp/secret.key"));
	}

	public byte[] decryptAsymFile(File file, byte[] key) {
		try {
			FileInputStream fin = new FileInputStream(file);
			FileInputStream keyin = getKeyFile();
			byte[] clear = PgpEncryption.decrypt(fin, keyin,
					new String(SecurityUtil.getInstance().getGpgPassword()).toCharArray());
			System.out.println("file successfully decryptd");
			return clear;
		} catch (Exception e) {
			System.err.println("error while decrypting: "
					+ e.getClass().getSimpleName() + " " + e.getMessage());
			return null;
		}
	}

	public boolean writeAsymFile(File file, byte[] data, byte[] key,
			List<String> recipients) {
		throw new UnsupportedOperationException("this is not supported by BC backend");
	}

	// this is what encryption would look like:
//	public static void enc() throws Exception {
//		FileOutputStream fout = new FileOutputStream("testenc.gpg");
//		FileInputStream keyin = new FileInputStream("pub.keys");
//		PGPPublicKey pubkey = PgpEncryption.readPublicKey(keyin);
//		byte[] data = PgpEncryption.encrypt(
//				"if you read this it worked!".getBytes(), pubkey,
//				PGPLiteralData.CONSOLE, true, false);
//		fout.write(data);
//		fout.close();
//	}

}
