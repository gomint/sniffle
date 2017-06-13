package io.gomint.proxy;

import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;

import javax.crypto.KeyAgreement;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Handles all encryption needs of the Minecraft Pocket Edition Protocol (ECDH Key Exchange and
 * shared secret generation).
 *
 * @author BlackyPaw
 * @version 1.0
 */
public class CryptoTest {

    private static KeyFactory ECDH_KEY_FACTORY;
    static {
        Security.addProvider( new org.bouncycastle.jce.provider.BouncyCastleProvider() );

        // Initialize KeyFactory:
        try {
            ECDH_KEY_FACTORY = KeyFactory.getInstance( "ECDH", "BC" );
        } catch ( NoSuchAlgorithmException e ) {
            e.printStackTrace();
            System.err.println( "Could not find ECDH Key Factory - please ensure that you have installed the latest version of BouncyCastle" );
            System.exit( -1 );
        } catch ( NoSuchProviderException e ) {
            e.printStackTrace();
            System.err.println( "Could not find BouncyCastle Key Provider - please ensure that you have installed BouncyCastle properly" );
            System.exit( -1 );
        }
    }

    public static void main( String[] args ) throws InvalidKeySpecException, InvalidKeyException, IOException {
        ECPublicKey clientKey = (ECPublicKey) ECDH_KEY_FACTORY.generatePublic( new X509EncodedKeySpec( Base64.getDecoder().decode( "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEDEKneqEvcqUqqFMM1HM1A4zWjJC+I8Y+aKzG5dl+6wNOHHQ4NmG2PEXRJYhujyodFH+wO0dEr4GM1WoaWog8xsYQ6mQJAC0eVpBM96spUB1eMN56+BwlJ4H3Qx4TAvAs" ) ) );
        ECPrivateKey privateKey = (ECPrivateKey) ECDH_KEY_FACTORY.generatePrivate( new PKCS8EncodedKeySpec( Base64.getDecoder().decode( "MB8CAQAwEAYHKoZIzj0CAQYFK4EEACIECDAGAgEBBAEB" ) ) );

        ECDHBasicAgreement agreement = new ECDHBasicAgreement();
        agreement.init( PrivateKeyFactory.createKey( privateKey.getEncoded() ) );
        byte[] secret = agreement.calculateAgreement( PublicKeyFactory.createKey( clientKey.getEncoded() ) ).toByteArray();

        System.out.println( Util.toHexString( secret ) );
        System.out.println( Util.toHexString( Base64.getDecoder().decode( "DEKneqEvcqUqqFMM1HM1A4zWjJC+I8Y+aKzG5dl+6wNOHHQ4NmG2PEXRJYhujyod" ) ) );
    }

}
