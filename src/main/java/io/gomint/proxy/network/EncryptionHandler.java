package io.gomint.proxy.network;

import io.gomint.proxy.jwt.JwtToken;
import io.gomint.proxy.jwt.MojangChainValidator;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyAgreement;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles all encryption needs of the Minecraft Pocket Edition Protocol (ECDH Key Exchange and
 * shared secret generation).
 *
 * @author BlackyPaw
 * @version 1.0
 */
public class EncryptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger( EncryptionHandler.class );
    private static KeyFactory ECDH_KEY_FACTORY;
    public static KeyPair PROXY_KEY_PAIR;

    static {
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

    private static ECPublicKey createPublicKey( String base64 ) {
        try {
            return (ECPublicKey) ECDH_KEY_FACTORY.generatePublic( new X509EncodedKeySpec( Base64.getDecoder()
                    .decode( base64 ) ) );
        } catch ( InvalidKeySpecException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates a new ECDSA Key Pair using the SEC curve secp384r1 provided by BouncyCastle. This must be invoked
     * before attempting to build a shared secret for the client or the backend server.
     */
    public static void generateEncryptionKeys() {
        // Setup KeyPairGenerator:
        KeyPairGenerator generator;
        try {
            generator = KeyPairGenerator.getInstance( "EC", "BC" );
            generator.initialize( 384 );
        } catch ( NoSuchAlgorithmException | NoSuchProviderException e ) {
            System.err.println( "It seems you have not installed a recent version of BouncyCastle; please ensure that your version supports EC Key-Pair-Generation using the secp384r1 curve" );
            System.exit( -1 );
            return;
        }

        // Generate the keypair:
        PROXY_KEY_PAIR = generator.generateKeyPair();

        try ( BufferedWriter writer = new BufferedWriter( new FileWriter( "server.public.key" ) ) ) {
            writer.write( Base64.getEncoder().encodeToString( PROXY_KEY_PAIR.getPublic().getEncoded() ) );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        try ( BufferedWriter writer = new BufferedWriter( new FileWriter( "server.private.key" ) ) ) {
            writer.write( Base64.getEncoder().encodeToString( PROXY_KEY_PAIR.getPrivate().getEncoded() ) );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    // Client Side:
    private boolean xboxLiveLogin;
    private String xboxUID;
    private String clientUsername;
    private UUID clientUUID;
    private ECPublicKey clientPublicKey;
    private BufferedBlockCipher clientEncryptor;
    private BufferedBlockCipher clientDecryptor;
    private byte[] clientSalt;
    private boolean clientEncryptionEnabled;
    private Map<String, PublicKey> trustedKeys;
    private AtomicLong clientSendCounter = new AtomicLong( 0 );
    private AtomicLong clientReceiveCounter = new AtomicLong( 0 );
    private byte[] clientKey;

    // Server Side:
    private ECPublicKey serverPublicKey;
    private BufferedBlockCipher serverEncryptor;
    private BufferedBlockCipher serverDecryptor;
    private AtomicLong serverSendCounter = new AtomicLong( 0 );
    private AtomicLong serverReceiveCounter = new AtomicLong( 0 );
    private byte[] serverKey;

    // Miscellaneous:

    public EncryptionHandler() {
        this.xboxLiveLogin = false;
        this.xboxUID = null;
        this.clientUsername = null;
        this.clientUUID = null;
        this.clientPublicKey = null;
        this.clientEncryptionEnabled = false;
        this.trustedKeys = new HashMap<>();
    }

    /**
     * Checks whether or not the client previously supplied a JWT token chain which has been signed using the trusted
     * Mojang Private Key and can thus be trusted to be a valid Xbox Live Login.
     * <p>
     * Must be invoked _AFTER_ a client JWT chain has been supplied.
     *
     * @return Whether or not the client previously supplied a valid XBox Live JWT Token Chain
     */
    public boolean isXboxLiveLogin() {
        return this.xboxLiveLogin;
    }

    /**
     * Checks whether or not the client supplied all mandatory fields about itself (PublicKey, Username and UUID)
     * regardless of whether the supplied JWT chain could be validated or not.
     * <p>
     * Must be invoked _AFTER_ a client JWT chain has been supplied.
     *
     * @return Whether or not the client previously supplied all mandatory information in a JWT Token Chain
     */
    public boolean hasObligatoryInformation() {
        return ( this.clientPublicKey != null && this.clientUsername != null && this.clientUUID != null );
    }

    /**
     * If the client supplied a valid XBox Live JWT chain it will also send the user's XBox Live Unique ID called
     * XUID. It appears to be a regular integer but due to the fact that there is not documentation about it we will
     * have to leave it in the form we are being passed it right now: as a string.
     * <p>
     * If the client did not provide a valid XBox Live JWT chain or missed to specify his / her XUID then this method
     * will return null.
     * <p>
     * Must be invoked _AFTER_ a client JWT chain has been supplied.
     *
     * @return The client's XBox Live Unique ID
     */
    public String getXboxUID() {
        return this.xboxUID;
    }

    /**
     * Gets the client's username as encoded inside a previously supplied JWT chain. May be null in case the client did
     * not send any username at all. This value will also be available for JWT chains which have not been authenticated
     * via XBox Live.
     * <p>
     * Must be invoked _AFTER_ a client JWT chain has been supplied.
     *
     * @return The client's username
     */
    public String getClientUsername() {
        return this.clientUsername;
    }

    /**
     * Gets the client's unique ID as encoded inside a previously supplied JWT chain. May be null in case the client did
     * not send any UUID at all. This value will also be available for JWT chains which have not been authenticated via
     * XBox Live.
     * <p>
     * Must be invoked _AFTER_ a client JWT chain has been supplied.
     *
     * @return The client's unique ID
     */
    public UUID getClientUUID() {
        return this.clientUUID;
    }

    /**
     * Gets the client's public ECDH key. This key is the one to be used for further protocol encryption and is
     * specified within the JWT token containing other related client information. If said information is missing or no
     * public key has been specified at all, this method will return null instead.
     * <p>
     * Must be invoked _AFTER_ a client JWT chain has been supplied.
     *
     * @return The client's public ECDH key
     */
    public ECPublicKey getClientPublicKey() {
        return this.clientPublicKey;
    }

    /**
     * Supplies a client-provided JWT chain to the EncryptionHandler.
     * <p>
     * The EncryptionHandler will attempt to validate the supplied token chain by checking whether or not it can
     * validate all included tokens beginning with a known public key provided by Mojang. If it cannot, it will assume
     * that the client has not logged in via XBox Live in which case it will not allow the client to specify a value for
     * the XUID field inside the JWT chain. After this method has completed one may retrieve all further information
     * contained within the supplied chain using other methods of this class. This method may only be invoked once.
     *
     * @param jwt The raw JSON string containing the JWT chain provided by the client
     */
    public void supplyClientJWTChain( String jwt ) {
        JSONObject json;
        try {
            json = this.parseJwtString( jwt );
        } catch ( ParseException e ) {
            e.printStackTrace();
            return;
        }

        Object jsonChainRaw = json.get( "chain" );
        if ( jsonChainRaw == null || !( jsonChainRaw instanceof JSONArray ) ) {
            return;
        }

        MojangChainValidator chainValidator = new MojangChainValidator();
        JSONArray jsonChain = (JSONArray) jsonChainRaw;
        for ( int i = 0; i < jsonChain.size(); ++i ) {
            Object jsonTokenRaw = jsonChain.get( i );
            if ( jsonTokenRaw instanceof String ) {
                try {
                    JwtToken token = JwtToken.parse( (String) jsonTokenRaw );
                    chainValidator.addToken( token );
                } catch ( IllegalArgumentException e ) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        this.trustedKeys = chainValidator.getTrustedKeys();
        this.xboxLiveLogin = chainValidator.validate();
        this.clientUsername = chainValidator.getUsername();
        this.clientUUID = chainValidator.getUUID();
        this.xboxUID = chainValidator.getXboxId();
        this.clientPublicKey = chainValidator.getClientPublicKey();

        try ( BufferedWriter writer = new BufferedWriter( new FileWriter( "client.public.key" ) ) ) {
            writer.write( Base64.getEncoder().encodeToString( this.clientPublicKey.getEncoded() ) );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        LOGGER.info( "Client provided JWT Chain: [authenticated=" + this.xboxLiveLogin + ", username=" + this.clientUsername + ", uuid=" + this.clientUUID + ", xuid=" + this.xboxUID + "]" );
        LOGGER.debug( "Client public key: " + Base64.getEncoder().encodeToString( this.clientPublicKey.getEncoded() ) );
    }

    /**
     * Sets the server's public ECDH key which is required for decoding packets received from the proxied server and
     * encoding packets to be sent to the proxied server.
     *
     * @param base64 The base64 string containing the encoded public key data
     */
    public void setServerPublicKey( String base64 ) {
        this.serverPublicKey = createPublicKey( base64 );
    }

    /**
     * Sets up everything required for encrypting and decrypting networking data received from the proxied server.
     *
     * @param salt The salt to prepend in front of the ECDH derived shared secret before hashing it (sent to us from the
     *             proxied server in a 0x03 packet)
     */
    public boolean beginServersideEncryption( byte[] salt ) {
        if ( this.isEncryptionFromServerEnabled() ) {
            // Already initialized:
            return true;
        }

        System.out.println( "Size of salt: " + salt.length );

        // Generate shared secret from ECDH keys:
        byte[] secret = this.generateECDHSecret( PROXY_KEY_PAIR.getPrivate(), this.serverPublicKey );
        if ( secret == null ) {
            return false;
        }

        // Derive key as salted SHA-256 hash digest:
        this.serverKey = this.hashSHA256( salt, secret );
        byte[] iv = this.takeBytesFromArray( this.serverKey, 0, 16 );

        // Initialize BlockCiphers:
        this.serverEncryptor = this.createCipher( true, this.serverKey, iv );
        this.serverDecryptor = this.createCipher( false, this.serverKey, iv );
        return true;
    }

    /**
     * Sets up everything required to begin encrypting network data sent to or received from the client.
     *
     * @return Whether or not the setup completed successfully
     */
    public boolean beginClientsideEncryption() {
        if ( this.isEncryptionFromClientEnabled() ) {
            // Already initialized:
            return true;
        }

        // Generate a random salt:
        SecureRandom random = new SecureRandom();
        this.clientSalt = new byte[16];
        random.nextBytes( this.clientSalt );

        // Generate shared secret from ECDH keys:
        byte[] secret = this.generateECDHSecret( PROXY_KEY_PAIR.getPrivate(), this.clientPublicKey );
        if ( secret == null ) {
            return false;
        }

        // Derive key as salted SHA-256 hash digest:
        this.clientKey = this.hashSHA256( this.clientSalt, secret );
        byte[] iv = this.takeBytesFromArray( this.clientKey, 0, 16 );

        // Initialize BlockCiphers:
        this.clientEncryptor = this.createCipher( true, this.clientKey, iv );
        this.clientDecryptor = this.createCipher( false, this.clientKey, iv );
        return true;
    }

    public byte[] getClientSalt() {
        return this.clientSalt;
    }

    public boolean isEncryptionFromClientEnabled() {
        return ( this.clientEncryptor != null && this.clientDecryptor != null );
    }

    public boolean isEncryptionToClientEnabled() {
        return ( this.clientEncryptionEnabled );
    }

    public boolean isEncryptionFromServerEnabled() {
        return ( this.serverEncryptor != null && this.serverDecryptor != null );
    }

    public boolean isEncryptionToServerEnabled() {
        return this.isEncryptionFromServerEnabled();
    }

    public void setEncryptionToClientEnabled( boolean enabled ) {
        this.clientEncryptionEnabled = enabled;
    }

    public byte[] decryptInputFromClient( byte[] input ) {
        byte[] output = this.processCipher( this.clientDecryptor, input );
        if ( output == null ) {
            return null;
        }

        byte[] outputChunked = new byte[input.length - 8];
        System.arraycopy( output, 0, outputChunked, 0, outputChunked.length );

        byte[] hashBytes = calcHash( outputChunked, this.clientKey, this.clientReceiveCounter );
        for ( int i = output.length - 8; i < output.length; i++ ) {
            if ( hashBytes[i - ( output.length - 8 )] != output[i] ) {
                return null;
            }
        }

        return outputChunked;
    }

    public byte[] decryptInputFromServer( byte[] input ) {
        byte[] output = this.processCipher( this.serverDecryptor, input );
        if ( output == null ) {
            return null;
        }

        byte[] outputChunked = new byte[input.length - 8];

        System.arraycopy( output, 0, outputChunked, 0, outputChunked.length );

        byte[] hashBytes = calcHash( outputChunked, this.serverKey, this.serverReceiveCounter );
        for ( int i = output.length - 8; i < output.length; i++ ) {
            if ( hashBytes[i - ( output.length - 8 )] != output[i] ) {
                return null;
            }
        }

        return outputChunked;
    }

    public byte[] encryptInputForClient( byte[] input ) {
        byte[] hashBytes = calcHash( input, this.clientKey, this.clientSendCounter );
        byte[] finalInput = new byte[hashBytes.length + input.length];

        System.arraycopy( input, 0, finalInput, 0, input.length );
        System.arraycopy( hashBytes, 0, finalInput, input.length, 8 );

        return this.processCipher( this.clientEncryptor, finalInput );
    }

    public byte[] encryptInputForServer( byte[] input ) {
        byte[] hashBytes = calcHash( input, this.serverKey, this.serverSendCounter );
        byte[] finalInput = new byte[hashBytes.length + input.length];

        System.arraycopy( input, 0, finalInput, 0, input.length );
        System.arraycopy( hashBytes, 0, finalInput, input.length, 8 );

        return this.processCipher( this.serverEncryptor, finalInput );
    }

    private byte[] processCipher( BufferedBlockCipher cipher, byte[] input ) {
        byte[] output = new byte[cipher.getOutputSize( input.length )];
        int cursor = cipher.processBytes( input, 0, input.length, output, 0 );

        try {
            // cursor += cipher.doFinal( output, cursor );
            if ( cursor != output.length ) {
                throw new InvalidCipherTextException( "Output size did not match cursor" );
            }
        } catch ( InvalidCipherTextException e ) {
            LOGGER.error( "Could not encrypt/decrypt to/from cipher-text", e );
            return null;
        }

        return output;
    }

    // ========================================== Utility Methods

    private byte[] calcHash( byte[] input, byte[] key, AtomicLong counter ) {
        SHA256Digest digest = new SHA256Digest();

        byte[] result = new byte[digest.getDigestSize()];
        digest.update( ByteBuffer.allocate( 8 ).order( ByteOrder.LITTLE_ENDIAN ).putLong( counter.getAndIncrement() ).array(), 0, 8 );
        digest.update( input, 0, input.length );
        digest.update( key, 0, key.length );
        digest.doFinal( result, 0 );

        return Arrays.copyOf( result, 8 );
    }

    private byte[] generateECDHSecret( PrivateKey privateKey, PublicKey publicKey ) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance( "ECDH", "BC" );
            ka.init( privateKey );
            ka.doPhase( publicKey, true );
            return ka.generateSecret();
        } catch ( NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException e ) {
            // LOGGER.error( "Failed to generate Elliptic-Curve-Diffie-Hellman Shared Secret for clientside encryption", e );
            return null;
        }
    }

    private byte[] takeBytesFromArray( byte[] buffer, int offset, int length ) {
        byte[] result = new byte[length];
        System.arraycopy( buffer, offset, result, 0, length );
        return result;
    }

    private byte[] hashSHA256( byte[]... message ) {
        SHA256Digest digest = new SHA256Digest();

        byte[] result = new byte[digest.getDigestSize()];
        for ( byte[] bytes : message ) {
            digest.update( bytes, 0, bytes.length );
        }

        digest.doFinal( result, 0 );

        return result;
    }

    private BufferedBlockCipher createCipher( boolean encryptor, byte[] key, byte[] iv ) {
        BufferedBlockCipher cipher = new BufferedBlockCipher( new CFBBlockCipher( new RijndaelEngine( 128 ), 8 ) );
        cipher.init( encryptor, new ParametersWithIV( new KeyParameter( key ), iv ) );
        return cipher;
    }

    /**
     * Parses the specified JSON string and ensures it is a JSONObject.
     *
     * @param jwt The string to parse
     * @return The parsed JSON object on success
     * @throws ParseException Thrown if the given JSON string is invalid or does not start with a JSONObject
     */
    private JSONObject parseJwtString( String jwt ) throws ParseException {
        Object jsonParsed = new JSONParser().parse( jwt );
        if ( jsonParsed instanceof JSONObject ) {
            return (JSONObject) jsonParsed;
        } else {
            throw new ParseException( ParseException.ERROR_UNEXPECTED_TOKEN );
        }
    }

    public Map<String, PublicKey> getTrustedKeys() {
        return this.trustedKeys;
    }

    public String getProxyPublic() {
        return Base64.getEncoder().encodeToString( PROXY_KEY_PAIR.getPublic().getEncoded() );
    }

    public Key getProxyPrivate() {
        return PROXY_KEY_PAIR.getPrivate();
    }
}
