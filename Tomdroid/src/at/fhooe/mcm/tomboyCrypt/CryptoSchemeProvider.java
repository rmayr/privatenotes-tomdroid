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
	//private static CryptoScheme defaultScheme = new CryptoSchemeV1();
	private static CryptoScheme defaultScheme = new NullCryptoScheme();
	private static AsymmetricCryptoScheme sharedScheme = new PgpCryptoScheme();
	
	public static CryptoScheme getConfiguredCryptoScheme() {
		//return defaultScheme;
		return sharedScheme;
	}
	
}
