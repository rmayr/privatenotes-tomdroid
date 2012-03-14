package at.fhooe.mcm.tomboyCrypt;

/**
 * provider class, used as a single point of configuration for the crypto classes
 * whenever we need a crypto instance somewhere in our main program, we should get
 * it from here and not create it explicitly.
 * 
 * @author Paul Klingelhuber
 *
 */
public class CryptoSchemeProvider
{
	/**
	 * only for testing
	 */
	public static final boolean DISABLE_ALL_ENCRYPTION = false;
	/**
	 * use apg or bouncycastle crypto backend
	 */
	public static final boolean USE_APG_INTEGRATION = false;
	
	private static AsymmetricCryptoScheme sharedScheme = null;
	
	static {
		if (DISABLE_ALL_ENCRYPTION) {
			sharedScheme = new NullCryptoScheme();
		} else if (USE_APG_INTEGRATION) {
			sharedScheme = new PgpCryptoScheme();
		} else {
			sharedScheme = new PgpCryptoSchemeBc();
		}
	}
	
	public static CryptoScheme getConfiguredCryptoScheme() {
		return sharedScheme;
	}
	
}
