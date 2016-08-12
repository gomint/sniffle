/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.network;

import io.gomint.jraknet.PacketBuffer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * @author geNAZt
 * @version 1.0
 */
public class PacketLogin extends Packet {

    private static final String MOJANG_PUBLIC = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V";
    private static KeyFactory KEY_FACTORY;

    static {
        try {
            KEY_FACTORY = KeyFactory.getInstance( "ECDH", "BC" );
        } catch ( NoSuchAlgorithmException e ) {
            System.out.println( "Cryptography error: could not initialize ECDH keyfactory!" );
            e.printStackTrace();
            System.exit( -1 );
        } catch ( NoSuchProviderException e ) {
            System.out.println( "BC Could not be found. Be sure to have installed org.bouncycastle.bcprov-jdk15on library" );
            e.printStackTrace();
            System.exit( -1 );
        }
    }

    private int protocol;

    // Chain additional data
    private String userName;
    private UUID uuid;
    private long xboxId;

    private byte[] skin;

    // Validation of login
    private String validationKey;
    private boolean valid = true;
    private boolean firstCertAuth = true;
    private String loginJWT;

    /**
     * Construct a new login packet which contains all data to login into a MC:PE server
     */
    public PacketLogin() {
        super( (byte) 0 );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeInt( this.protocol );

        ByteBuffer byteBuffer = ByteBuffer.allocate( this.loginJWT.length() + 4 + this.skin.length + 4 );
        byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
        byteBuffer.putInt( this.loginJWT.length() );
        byteBuffer.put( this.loginJWT.getBytes() );
        byteBuffer.putInt( this.skin.length );
        byteBuffer.put( this.skin );

        Deflater deflater = new Deflater();
        deflater.setInput( byteBuffer.array() );
        deflater.finish();

        ByteArrayOutputStream baos = new ByteArrayOutputStream( byteBuffer.capacity() );
        byte[] intermediate = new byte[256];
        while ( !deflater.finished() ) {
            int read = deflater.deflate( intermediate );
            baos.write( intermediate, 0, read );
        }

        byte[] loginData = baos.toByteArray();
        buffer.writeInt( loginData.length );
        buffer.writeBytes( loginData );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.protocol = buffer.readInt();

        // Decompress inner data (i don't know why you compress inside of a Batched Packet but hey)
        byte[] compressed = new byte[buffer.readInt()];
        buffer.readBytes( compressed );

        Inflater inflater = new Inflater();
        inflater.setInput( compressed );

        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        try {
            byte[] comBuffer = new byte[1024];
            while ( !inflater.finished() ) {
                int read = inflater.inflate( comBuffer );
                bout.write( comBuffer, 0, read );
            }
        } catch ( DataFormatException e ) {
            System.out.println( "Failed to decompress batch packet" + e );
            return;
        }

        // More data please
        ByteBuffer byteBuffer = ByteBuffer.wrap( bout.toByteArray() );
        byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
        byte[] stringBuffer = new byte[byteBuffer.getInt()];
        byteBuffer.get( stringBuffer );
        
        System.out.println( "JSON:" ) ;
        System.out.println( new String( stringBuffer ) );

        // Decode the json stuff
        try {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse( new String( stringBuffer ) );
            JSONArray chainArray = (JSONArray) jsonObject.get( "chain" );
            if ( chainArray != null ) {
                this.validationKey = parseBae64JSON( (String) chainArray.get( chainArray.size() - 1 ) ); // First key in chain is last response in chain #brainfuck :D
                for ( Object chainObj : chainArray ) {
                    decodeBase64JSON( (String) chainObj );
                }
            }
        } catch ( ParseException e ) {
            e.printStackTrace();
        }

        // Skin comes next
        this.skin = new byte[byteBuffer.getInt()];
        byteBuffer.get( this.skin );
    }

    private String parseBae64JSON( String data ) throws ParseException {
        // Be able to "parse" the payload
        String[] tempBase64 = data.split( "\\." );

        String payload = new String( Base64.getDecoder().decode( tempBase64[1] ) );
        JSONObject chainData = (JSONObject) new JSONParser().parse( payload );
        return (String) chainData.get( "identityPublicKey" );
    }

    private void decodeBase64JSON( String data ) throws ParseException {
        try {
            // Get validation key
            Key key = getPublicKey( Base64.getDecoder().decode( this.validationKey ) );
            if ( key == null ) {
                return;
            }

            // Check JWT
            Claims claims = Jwts.parser().setSigningKey( key ).parseClaimsJws( data ).getBody();

            // Only certification authory is allowed to set new validation keys
            Boolean certificateAuthority = (Boolean) claims.get( "certificateAuthority" );
            if ( certificateAuthority != null && certificateAuthority ) {
                this.validationKey = (String) claims.get( "identityPublicKey" );

                // We have to blindy trust this auth when its the first (they send the root cert in 0.15.4+)
                if ( this.firstCertAuth && this.validationKey.equals( MOJANG_PUBLIC ) ) {
                    this.firstCertAuth = false;
                    return;
                }
            }

            // Invalid duration frame ?
            if ( claims.getExpiration().getTime() - claims.getIssuedAt().getTime() != TimeUnit.DAYS.toMillis( 1 ) ) {
                System.out.println( "Certification lifetime is not 1 day." );
                this.valid = false;
            }

            // Invalid Issuer ?
            if ( !"RealmsAuthorization".equals( claims.getIssuer() ) ) {
                System.out.println( "Certification issuer is wrong." );
                this.valid = false;
            }

            // Check for extra data
            Map<String, Object> extraData = (Map<String, Object>) claims.get( "extraData" );
            if ( extraData != null ) {
                // For a valid user we need a XUID (xbox live id)
                String xboxId = (String) extraData.get( "XUID" );
                if ( xboxId == null ) {
                    System.out.println( "Did not find any xbox live id" );
                    this.valid = false;
                } else {
                    this.xboxId = Long.parseLong( xboxId );
                }

                this.userName = (String) extraData.get( "displayName" );
                this.uuid = UUID.fromString( (String) extraData.get( "identity" ) );
            }
        } catch ( Exception e ) {
            // This normally comes when the user is not logged in into xbox live since the payload only sends
            // the self signed cert without a certifaction authory
            this.valid = false;

            // Be able to "parse" the payload
            String[] tempBase64 = data.split( "\\." );

            String payload = new String( Base64.getDecoder().decode( tempBase64[1] ) );
            JSONObject chainData = (JSONObject) new JSONParser().parse( payload );
            if ( chainData.containsKey( "extraData" ) ) {
                JSONObject extraData = (JSONObject) chainData.get( "extraData" );
                this.userName = (String) extraData.get( "displayName" );
                this.uuid = UUID.fromString( (String) extraData.get( "identity" ) );
            }
        }
    }

    private ECPublicKey getPublicKey( byte[] publicKeyBlob ) {
        X509EncodedKeySpec ks = new X509EncodedKeySpec( publicKeyBlob );

        try {
            return (ECPublicKey) KEY_FACTORY.generatePublic( ks );
        } catch ( InvalidKeySpecException e ) {
            System.out.println( "Received invalid key specification from client" );
            this.valid = false;
            return null;
        } catch ( ClassCastException e ) {
            System.out.println( "Received valid X.509 key from client but it was not EC Public Key material" );
            this.valid = false;
            return null;
        }
    }

    /**
     * Get the given Username for this Account
     *
     * @return UserName given by the client
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Get the given UUID for this Account
     *
     * @return UUID given by the client
     */
    public UUID getUUID() {
        return this.uuid;
    }

    /**
     * Check if the login was valid
     *
     * @return true when it was valid, false when it does not
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Return found xbox live id
     *
     * @return the id of the xbox account
     */
    public long getXboxId() {
        return xboxId;
    }

    public void setLoginJWT( String loginJWT ) {
        this.loginJWT = loginJWT;
    }

}