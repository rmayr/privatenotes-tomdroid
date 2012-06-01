/**
 * adapted from Bouncycastles ByteArrayHandler (some small changes)
 */
package at.fhooe.mcm.tomboyCrypt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Date;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPUtil;

public class PgpSymmetric {
	/**
     * decrypt the passed in message stream
     * 
     * @param encrypted  The message to be decrypted.
     * @param passPhrase Pass phrase (key)
     * 
     * @return Clear text as a byte array.  I18N considerations are
     *         not handled by this routine
     * @exception IOException
     * @exception PGPException
     * @exception NoSuchProviderException
     */
    public static InputStream decrypt(
        InputStream encrypted,
        char[] passPhrase)
        throws IOException, PGPException, NoSuchProviderException
    {
        InputStream in = encrypted;

        in = PGPUtil.getDecoderStream(in);

        PGPObjectFactory         pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList   enc = null;
        Object                          o = pgpF.nextObject();
        
        //
        // the first object might be a PGP marker packet.
        //
        if (o instanceof PGPEncryptedDataList)
        {
            enc = (PGPEncryptedDataList)o;
        }
        else
        {
            enc = (PGPEncryptedDataList)pgpF.nextObject();
        }

        PGPPBEEncryptedData pbe = (PGPPBEEncryptedData)enc.get(0);

        InputStream clear = pbe.getDataStream(passPhrase, Util.SECURITY_PROVIDER_ID);

        PGPObjectFactory        pgpFact = new PGPObjectFactory(clear);

        PGPCompressedData   cData = (PGPCompressedData)pgpFact.nextObject();

        pgpFact = new PGPObjectFactory(cData.getDataStream());

        PGPLiteralData ld = (PGPLiteralData)pgpFact.nextObject();

        return ld.getInputStream();
    }

    /**
     * Simple PGP encryptor between byte[].
     * 
     * @param clearData  The test to be encrypted
     * @param passPhrase The pass phrase (key).  This method assumes that the
     *                   key is a simple pass phrase, and does not yet support
     *                   RSA or more sophisiticated keying.
     * @param fileName   File name. This is used in the Literal Data Packet (tag 11)
     *                   which is really inly important if the data is to be
     *                   related to a file to be recovered later.  Because this
     *                   routine does not know the source of the information, the
     *                   caller can set something here for file name use that
     *                   will be carried.  If this routine is being used to
     *                   encrypt SOAP MIME bodies, for example, use the file name from the
     *                   MIME type, if applicable. Or anything else appropriate.
     *                             
     * @param armor
     * 
     * @return encrypted data.
     * @exception IOException
     * @exception PGPException
     * @exception NoSuchProviderException
     */
    public static byte[] encrypt(
        byte[]  clearData,
        char[]  passPhrase,
        String  fileName,
        int     algorithm,
        boolean armor)
        throws IOException, PGPException, NoSuchProviderException
    {
        if (fileName == null)
        {
            fileName= PGPLiteralData.CONSOLE;
        }

        byte[] compressedData = compress(clearData, fileName, CompressionAlgorithmTags.ZIP);

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        OutputStream out = bOut;
        if (armor)
        {
            out = new ArmoredOutputStream(out);
        }

        PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(algorithm, new SecureRandom(), Util.SECURITY_PROVIDER_ID);
        encGen.addMethod(passPhrase);

        OutputStream encOut = encGen.open(out, compressedData.length);

        encOut.write(compressedData);
        encOut.close();

        if (armor)
        {
            out.close();
        }

        return bOut.toByteArray();
    }

    private static byte[] compress(byte[] clearData, String fileName, int algorithm) throws IOException
    {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(algorithm);
        OutputStream cos = comData.open(bOut); // open it with the final destination

        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();

        // we want to generate compressed data. This might be a user option later,
        // in which case we would pass in bOut.
        OutputStream  pOut = lData.open(cos, // the compressed output stream
                                        PGPLiteralData.BINARY,
                                        fileName,  // "filename" to store
                                        clearData.length, // length of clear data
                                        new Date()  // current time
                                      );

        pOut.write(clearData);
        pOut.close();

        comData.close();

        return bOut.toByteArray();
    }
}
