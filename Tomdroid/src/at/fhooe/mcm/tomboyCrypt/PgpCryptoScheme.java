package at.fhooe.mcm.tomboyCrypt;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

public class PgpCryptoScheme implements AsymmetricCryptoScheme {
	
    private static final String m_ApgPackageName = "org.thialfihar.android.apg";
    private static final int m_MinRequiredVersion = 16;
    private static final String NO_APG_ERROR = "no APG installed";
    
    private static Context m_context = null;
	private static Boolean available = null;
	private static Activity m_activity = null;
	
	/**
	 * the different intents
	 */
    public static class Intents
    {
        public static final String DECRYPT = "org.thialfihar.android.apg.intent.DECRYPT";
        public static final String ENCRYPT = "org.thialfihar.android.apg.intent.ENCRYPT";
        public static final String DECRYPT_FILE = "org.thialfihar.android.apg.intent.DECRYPT_FILE";
        public static final String ENCRYPT_FILE = "org.thialfihar.android.apg.intent.ENCRYPT_FILE";
        public static final String DECRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.DECRYPT_AND_RETURN";
        public static final String ENCRYPT_AND_RETURN = "org.thialfihar.android.apg.intent.ENCRYPT_AND_RETURN";
        public static final String SELECT_PUBLIC_KEYS = "org.thialfihar.android.apg.intent.SELECT_PUBLIC_KEYS";
        public static final String SELECT_SECRET_KEY = "org.thialfihar.android.apg.intent.SELECT_SECRET_KEY";
    }
    
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_DATA = "data";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_DECRYPTED_MESSAGE = "decryptedMessage";
    public static final String EXTRA_DECRYPTED_DATA = "decryptedData";
    public static final String EXTRA_ENCRYPTED_MESSAGE = "encryptedMessage";
    public static final String EXTRA_ENCRYPTED_DATA = "encryptedData";
    public static final String EXTRA_RESULT_URI = "resultUri";
    public static final String EXTRA_SIGNATURE = "signature";
    public static final String EXTRA_SIGNATURE_KEY_ID = "signatureKeyId";
    public static final String EXTRA_SIGNATURE_USER_ID = "signatureUserId";
    public static final String EXTRA_SIGNATURE_SUCCESS = "signatureSuccess";
    public static final String EXTRA_SIGNATURE_UNKNOWN = "signatureUnknown";
    public static final String EXTRA_SIGNATURE_DATA = "signatureData";
    public static final String EXTRA_SIGNATURE_TEXT = "signatureText";
    public static final String EXTRA_USER_ID = "userId";
    public static final String EXTRA_USER_IDS = "userIds";
    public static final String EXTRA_KEY_ID = "keyId";
    public static final String EXTRA_REPLY_TO = "replyTo";
    public static final String EXTRA_SEND_TO = "sendTo";
    public static final String EXTRA_SUBJECT = "subject";
    public static final String EXTRA_ENCRYPTION_KEY_IDS = "encryptionKeyIds";
    public static final String EXTRA_SELECTION = "selection";
    public static final String EXTRA_ASCII_ARMOUR = "asciiArmour";
    public static final String EXTRA_BINARY = "binary";
    public static final String EXTRA_KEY_SERVERS = "keyServers";

    
    public static final int DECRYPT_MESSAGE = 0x21070001;
    public static final int ENCRYPT_MESSAGE = 0x21070002;
    public static final int SELECT_PUBLIC_KEYS = 0x21070003;
    public static final int SELECT_SECRET_KEY = 0x21070004;

    
    private String signatureUserId = null;
    private long signatureKeyId = -1;
    private boolean signatureSuccess = false;
    private boolean signatureUnknown = true;
    private byte[] encryptedBinary = null;
    private byte[] decryptedBinary = null;
    private long[] encryptionKeyIds = null;
    
    private Object lockObj = new Object();
    private boolean cryptoDone = false;
    
    public byte[] getDecryptedBinary() {
		return decryptedBinary;
	}


	public void setDecryptedBinary(byte[] decryptedBinary) {
		this.decryptedBinary = decryptedBinary;
	}

	public byte[] getEncryptedBinary() {
		return encryptedBinary;
	}


	public void setEncryptedBinary(byte[] encryptedData) {
		this.encryptedBinary = encryptedData;
	}


	public String getSignatureUserId() {
		return signatureUserId;
	}


	public void setSignatureUserId(String signatureUserId) {
		this.signatureUserId = signatureUserId;
	}


	public long getSignatureKeyId() {
		return signatureKeyId;
	}


	public void setSignatureKeyId(long signatureKeyId) {
		this.signatureKeyId = signatureKeyId;
	}


	public boolean isSignatureSuccess() {
		return signatureSuccess;
	}


	public void setSignatureSuccess(boolean signatureSuccess) {
		this.signatureSuccess = signatureSuccess;
	}


	public boolean isSignatureUnknown() {
		return signatureUnknown;
	}


	public void setSignatureUnknown(boolean signatureUnknown) {
		this.signatureUnknown = signatureUnknown;
	}


	public long[] getEncryptionKeyIds() {
		return encryptionKeyIds;
	}


	public void setEncryptionKeyIds(long[] encryptionKeyIds) {
		this.encryptionKeyIds = encryptionKeyIds;
	}


	private void reset() {
    	signatureUserId = null;
    	signatureKeyId = -1;
    	signatureSuccess = false;
    	signatureUnknown = true;
    	decryptedBinary = null;
    	encryptedBinary = null;
    	cryptoDone = false;
    	encryptionKeyIds = null;
    }
	
	public static void setActivity(Activity activity) {
		m_activity = activity;
	}
	
	public boolean isAvailable() {
		if (available == null) {
			if (m_activity == null) {
				throw new IllegalStateException("you have to call 'setActivity' before you can use PgpCryptoScheme");
			}
			m_context = (m_activity != null) ? m_activity.getApplicationContext() : null;
	        try {
	            PackageInfo pi = m_context.getPackageManager().getPackageInfo(m_ApgPackageName, 0);
	            if (pi.versionCode >= m_MinRequiredVersion) {
	            	available = Boolean.TRUE;
	            } else {
	                System.err.println("APG version too old");
	                available = Boolean.FALSE;
	            }
	        } catch (NameNotFoundException e) {
	        	System.err.println("Error accessing package info");
	            available = Boolean.FALSE;
	        }
		}
		m_context = (m_activity != null) ? m_activity.getApplicationContext() : null;
        return available.booleanValue();
	}
	
	private long[] getKeyIDs(long[] preselected) {
		Intent intent = new Intent(PgpCryptoScheme.Intents.SELECT_PUBLIC_KEYS);
        //intent.setType("text/plain");
        
        try
        {
        	//intent.putExtra(EXTRA_DATA, data);
        	//intent.putExtra(EXTRA_ASCII_ARMOUR, false);
        	intent.putExtra(EXTRA_SELECTION, preselected);
        	
            m_activity.startActivityForResult(intent, SELECT_PUBLIC_KEYS);           
            
            // block while decryption by apg is executed
            try {
            	synchronized (lockObj) {
            		while (!cryptoDone)
                		lockObj.wait();	
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
            return encryptionKeyIds;
        }
        catch (ActivityNotFoundException e)
        {
        	e.printStackTrace();
        	throw new UnsupportedOperationException("activity not found");
        }
	}

	public byte[] decryptFile(File file, byte[] key) {
		if (!isAvailable())
			throw new UnsupportedOperationException(NO_APG_ERROR);
		// TODO use real GPG encryption for symmetric crypo
		// XXX fallback for now: our old crypto scheme...
		return new CryptoSchemeV1().decryptFile(file, key);
	}

	public boolean writeFile(File file, byte[] data, byte[] key) {
		if (!isAvailable())
			throw new UnsupportedOperationException(NO_APG_ERROR);
		// TODO use real GPG encryption for symmetric crypo
		// XXX fallback for now: our old crypto scheme...
		return new CryptoSchemeV1().writeFile(file, data, key);
	}

	public synchronized byte[] decryptAsymFile(File file, byte[] key) {
		if (!isAvailable())
			throw new UnsupportedOperationException(NO_APG_ERROR);
		
		reset();
		
		byte[] data;
		try {
			data = Util.readFile(file);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.err.println("cannot decrypt, input error");
			return null;
		}
		
		Intent intent = new Intent(PgpCryptoScheme.Intents.DECRYPT_AND_RETURN);
        intent.setType("text/plain");
        if (data == null)
        {
            return null;
        }
        
        try
        {
        	intent.putExtra(EXTRA_DATA, data);
        	intent.putExtra(EXTRA_BINARY, true);
            m_activity.startActivityForResult(intent, DECRYPT_MESSAGE);           
            
            // block while decryption by apg is executed
            try {
            	synchronized (lockObj) {
            		while (!cryptoDone)
                		lockObj.wait();	
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
            return decryptedBinary;
        }
        catch (ActivityNotFoundException e)
        {
        	throw new UnsupportedOperationException("activity not found");
        }
	}

	public boolean writeAsymFile(File file, byte[] data, byte[] key,
			List<String> recipients) {
		if (!isAvailable())
			throw new UnsupportedOperationException(NO_APG_ERROR);
		
		reset();
		
		List<Long> preselectedIds = new ArrayList<Long>();
		for (String possibleId : recipients) {
			Long id = parseKeyIdFromString(possibleId);
			if (id != null) {
				preselectedIds.add(id);
			}
		}
		long[] keyIds = new long[preselectedIds.size()];
		for (int i=0; i<preselectedIds.size(); i++) {
			keyIds[i] = preselectedIds.get(i); // why the hell isn't toArray able to convert from Long to long... :(
		}
		
		long[] targets = getKeyIDs(keyIds);
		reset();
			
		Intent intent = new Intent(PgpCryptoScheme.Intents.ENCRYPT_AND_RETURN);
        intent.setType("text/plain");
        
        try
        {
        	intent.putExtra(EXTRA_DATA, data);
        	intent.putExtra(EXTRA_ASCII_ARMOUR, false);
        	intent.putExtra(EXTRA_ENCRYPTION_KEY_IDS, targets);
        	
            m_activity.startActivityForResult(intent, ENCRYPT_MESSAGE);           
            
            // block while decryption by apg is executed
            try {
            	synchronized (lockObj) {
            		while (!cryptoDone)
                		lockObj.wait();	
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			try {
				Util.writeFile(file, encryptedBinary);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
            return true;
        }
        catch (ActivityNotFoundException e)
        {
        	throw new UnsupportedOperationException("activity not found");
        }
	}
	
	/**
     * Handle the activity results that concern us.
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @return handled or not
     */
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode,
                                    android.content.Intent data)
    {
        switch (requestCode)
        {
            case SELECT_SECRET_KEY:
                if (resultCode != Activity.RESULT_OK || data == null)
                {
                    cryptoDone = true;
                    synchronized (lockObj) {
                    	lockObj.notify();
    				}
                    break;
                }
                setSignatureKeyId(data.getLongExtra(EXTRA_KEY_ID, 0));
                setSignatureUserId(data.getStringExtra(EXTRA_USER_ID));

                cryptoDone = true;
                synchronized (lockObj) {
                	lockObj.notify();
				}
                break;

            case SELECT_PUBLIC_KEYS:
                if (resultCode != Activity.RESULT_OK || data == null)
                {
                    encryptionKeyIds = null;
                    cryptoDone = true;
                    synchronized (lockObj) {
                    	lockObj.notify();
    				}
                    break;
                }
                encryptionKeyIds = data.getLongArrayExtra(EXTRA_SELECTION);
                
                cryptoDone = true;
                synchronized (lockObj) {
                	lockObj.notify();
				}
                break;

            case ENCRYPT_MESSAGE:
                if (resultCode != Activity.RESULT_OK || data == null)
                {
                    encryptedBinary = null;
                    
                    cryptoDone = true;
                    synchronized (lockObj) {
                    	lockObj.notify();
    				}
                    break;
                }
                encryptedBinary = data.getByteArrayExtra(EXTRA_ENCRYPTED_DATA);
                // this was a stupid bug in an earlier version, just gonna leave this in for an APG
                // version or two
                if (encryptedBinary == null) {
                	encryptedBinary = data.getByteArrayExtra(EXTRA_ENCRYPTED_DATA);
                }
                
            	cryptoDone = true;
                synchronized (lockObj) {
                	lockObj.notify();
				}
                break;

            case DECRYPT_MESSAGE:
                if (resultCode != Activity.RESULT_OK || data == null)
                {
                	decryptedBinary = null;
                	cryptoDone = true;
                    synchronized (lockObj) {
                    	lockObj.notify();
    				}
                    break;
                }

                signatureUserId = data.getStringExtra(EXTRA_SIGNATURE_USER_ID);
                signatureKeyId = data.getLongExtra(EXTRA_SIGNATURE_KEY_ID, 0);
                signatureSuccess = data.getBooleanExtra(EXTRA_SIGNATURE_SUCCESS, false);
                signatureUnknown = data.getBooleanExtra(EXTRA_SIGNATURE_UNKNOWN, false);

                decryptedBinary = data.getByteArrayExtra(EXTRA_DECRYPTED_DATA);
                
                cryptoDone = true;
                synchronized (lockObj) {
                	lockObj.notify();
				}

                break;

            default:
                return false;
        }

        return true;
    }
    
    /**
     * parses long-ids from key-strings, these key strings (like GPG outputs them) can look
     * sth like this:
     * <pre>BEE6 F545 BBBE BB76 4409  3575 3B8C C634 8D7E 68DD</pre>
     * what we want to have is to get from: 3B8C C634 8D7E 68DD to 4291022473991710941
     * this is done via this method. The id can also have random text prependet, the number
     * will be looked for at the end
     * @param id
     * @return
     */
    public static Long parseKeyIdFromString(String id) {
    	Long result = null;
    	String compacted = (id != null)? id.replaceAll(" ", "") : null;
    	if (compacted != null && compacted.length() >= 16) {
    		compacted = compacted.substring(compacted.length() - 16);
    		try {
    			// we have to parse via bigInteger, because the long value could
    			// also be negative (represented via 2s compliant, that's not handled
    			// by Long.parseLong
    			result = new BigInteger(compacted, 16).longValue();
    		} catch (NumberFormatException nfe) {
    			// invalid key
    		}
    	}
    	return result;
    }

}
