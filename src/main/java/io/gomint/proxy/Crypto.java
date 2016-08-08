package io.gomint.proxy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author geNAZt
 * @version 1.0
 */
public class Crypto {

    private static KeyFactory KEY_FACTORY;
    private static ECPrivateKey privateKey;
    private static ECPublicKey publicKey;

    static {
        try {
            KEY_FACTORY = java.security.KeyFactory.getInstance( "ECDH", "BC" );
        } catch ( NoSuchAlgorithmException e ) {
            System.out.println( "Cryptography error: could not initialize ECDH keyfactory!" );
            e.printStackTrace();
            System.exit( -1 );
        } catch ( NoSuchProviderException e ) {
            System.out.println( "BC Could not be found. Be sure to have installed org.bouncycastle.bcprov-jdk15on library" );
            e.printStackTrace();
            System.exit( -1 );
        }

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "ECDH", "BC" );
            keyPairGenerator.initialize( new ECGenParameterSpec( "prime256v1" ), new SecureRandom() );

            KeyPair pair = keyPairGenerator.generateKeyPair();
            publicKey = (ECPublicKey) pair.getPublic();
            privateKey = (ECPrivateKey) pair.getPrivate();
        } catch ( NoSuchAlgorithmException | InvalidAlgorithmParameterException | NoSuchProviderException e ) {
            e.printStackTrace();
        }
    }

    public static String getLoginJWT( String username, UUID uuid ) {
        String b64Key = Base64.getEncoder().encodeToString( publicKey.getEncoded() );

        JSONArray jsonArray = new JSONArray();
        jsonArray.add( Jwts.builder()
                .setHeader( new HashMap<String, Object>() {{
                    put( "x5u", b64Key );
                }} )
                .setClaims( new HashMap<String, Object>() {{
                    put( "IdentityPublicKey", b64Key );
                    put( "CertificateAuthority", true );
                    put( "RandomNonce", new Random().nextLong() );
                    put( "ExtraData", new HashMap<String, String>() {{
                        put( "displayName", username );
                        put( "identity", uuid.toString() );
                    }} );
                }} )
                .setIssuer( "self" )
                .setNotBefore( new Date() )
                .setIssuedAt( new Date() )
                .setExpiration( new Date( System.currentTimeMillis() + TimeUnit.DAYS.toMillis( 1 ) ) )
                .signWith( SignatureAlgorithm.ES384, privateKey )
                .compact() );

        JSONObject jsonObject = new JSONObject();
        jsonObject.put( "chain", jsonArray );
        return jsonObject.toJSONString();
    }

    public static CryptoContext createContext( String serverKey, byte[] ivBytes ) {
        try {
            byte[] serverKeyBytes = Base64.getDecoder().decode( serverKey );

            KeyAgreement aliceKeyAgree = KeyAgreement.getInstance( "ECDH", "BC" );
            aliceKeyAgree.init( privateKey );
            aliceKeyAgree.doPhase( getPublicKey( serverKeyBytes ), true );

            byte[] key = aliceKeyAgree.generateSecret();
            IvParameterSpec ivSpec = new IvParameterSpec( ivBytes );

            CFBBlockCipher cfbBlockCipher = new CFBBlockCipher( new RijndaelEngine(), 128 );
            cfbBlockCipher.init( false, new ParametersWithIV( new KeyParameter( key ), ivBytes ) );

            return new CryptoContext( key, ivSpec, cfbBlockCipher );
        } catch ( InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException e ) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] decrypt( CryptoContext context, byte[] encrypted ) {
        // Buffer used to transport the bytes from one stream to another
        byte[] buf = new byte[16];                 //input buffer
        byte[] obuf = new byte[512];            //output buffer

        context.getCipher().reset();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayInputStream in = new ByteArrayInputStream( encrypted );

            int noBytesProcessed = 0;   //number of bytes processed

            while ( in.read( buf ) >= 0 ) {
                noBytesProcessed = context.getCipher().decryptBlock( buf, 0, obuf, 0 );
                out.write( obuf, 0, noBytesProcessed );
            }

            out.flush();

            byte[] output = out.toByteArray();
            System.out.println( DatatypeConverter.printHexBinary( output ) );
            return output;
        } catch ( java.io.IOException e ) {
            System.out.println( e.getMessage() );
        }

        return null;
    }

    private static ECPublicKey getPublicKey( byte[] publicKeyBlob ) {
        X509EncodedKeySpec ks = new X509EncodedKeySpec( publicKeyBlob );

        try {
            return (ECPublicKey) KEY_FACTORY.generatePublic( ks );
        } catch ( InvalidKeySpecException e ) {
            System.out.println( "Received invalid key specification from client" );
            return null;
        } catch ( ClassCastException e ) {
            System.out.println( "Received valid X.509 key from client but it was not EC Public Key material" );
            return null;
        }
    }

}
