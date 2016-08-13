package io.gomint.proxy.network;

import io.gomint.proxy.jwt.JwtToken;
import io.gomint.proxy.jwt.MojangChainValidator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/**
 * Handles all encryption needs of the Minecraft Pocket Edition Protocol (ECDH Key Exchange and
 * shared secret generation).
 *
 * @author BlackyPaw
 * @version 1.0
 */
public class EncryptionHandler {
	
	private static KeyFactory ECDH_KEY_FACTORY;
	public static  KeyPair    PROXY_KEY_PAIR;
	
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
		ECGenParameterSpec spec = new ECGenParameterSpec( "secp384r1" );
		KeyPairGenerator   generator;
		try {
			generator = KeyPairGenerator.getInstance( "ECDSA", "BC" );
		} catch ( NoSuchAlgorithmException | NoSuchProviderException e ) {
			System.err.println( "It seems you have not installed a recent version of BouncyCastle; please ensure that your version supports ECDSA Key-Pair-Generation using the secp384r1 curve" );
			System.exit( -1 );
			return;
		}
		
		try {
			generator.initialize( spec, new SecureRandom() );
		} catch ( InvalidAlgorithmParameterException e ) {
			System.err.println( "It seems you have not installed a recent version of BouncyCastle; please ensure that your version supports ECDSA Key-Pair-Generation using the secp384r1 curve" );
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
	
	private final Logger logger = LoggerFactory.getLogger( EncryptionHandler.class );
	
	// Client Side:
	private boolean     xboxLiveLogin;
	private String      xboxUID;
	private String      clientUsername;
	private UUID        clientUUID;
	private ECPublicKey clientPublicKey;
	private Cipher      clientEncryptCipher;
	private Cipher      clientDecryptCipher;
	private byte[]      clientInitializationVector;
	
	// Server Side:
	private ECPublicKey serverPublicKey;
	private Cipher      serverEncryptCipher;
	private Cipher      serverDecryptCipher;
	
	// Proxy itself:
	private KeyPair encryptionKeyPair;
	
	public EncryptionHandler() {
		this.xboxLiveLogin = false;
		this.xboxUID = null;
		this.clientUsername = null;
		this.clientUUID = null;
		this.clientPublicKey = null;
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
		JSONArray            jsonChain      = (JSONArray) jsonChainRaw;
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
		
		this.logger.info( "Client provided JWT Chain: [authenticated=" + this.xboxLiveLogin + ", username=" + this.clientUsername + ", uuid=" + this.clientUUID + ", xuid=" + this.xboxUID + "]" );
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
	 * @param initializationVector The initialization vector sent to us from the server to set up the AES-128 cipher
	 */
	public boolean beginServersideEncryption( byte[] initializationVector ) {
		SecretKey secret = this.generateSharedSecret( PROXY_KEY_PAIR.getPrivate(), this.serverPublicKey );
		if ( secret == null ) {
			return false;
		}
		
		IvParameterSpec iv = new IvParameterSpec( initializationVector );
		
		try {
			this.serverEncryptCipher = Cipher.getInstance( "AES/CBC/PKCS5Padding" );
			this.serverDecryptCipher = Cipher.getInstance( "AES/CBC/PKCS5Padding" );
		} catch ( NoSuchAlgorithmException | NoSuchPaddingException e ) {
			this.logger.error( "Failed to retrieve AES Cipher instance", e );
			return false;
		}
		
		try {
			this.serverEncryptCipher.init( Cipher.ENCRYPT_MODE, secret, iv );
			this.serverDecryptCipher.init( Cipher.DECRYPT_MODE, secret, iv );
		} catch ( InvalidKeyException | InvalidAlgorithmParameterException e ) {
			this.logger.error( "Failed to setup AES Cipher instances", e );
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sets up everything required to begin encrypting network data sent to or received from the client.
	 *
	 * @return Whether or not the setup completed successfully
	 */
	public boolean beginClientsideEncryption() {
		if ( this.clientEncryptCipher != null || this.clientDecryptCipher != null ) {
			// Already initialized:
			return true;
		}
		
		SecretKey secret = this.generateSharedSecret( PROXY_KEY_PAIR.getPrivate(), this.clientPublicKey );
		if ( secret == null ) {
			return false;
		}
		
		SecureRandom random = new SecureRandom();
		byte[] iv = new byte[16];
		random.nextBytes( iv );
		
		try ( BufferedWriter writer = new BufferedWriter( new FileWriter( "server.iv.bin" ) ) ) {
			writer.write( Base64.getEncoder().encodeToString( iv ) );
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		
		IvParameterSpec spec = new IvParameterSpec( iv );
		
		try {
			this.clientEncryptCipher = Cipher.getInstance( "AES/CFB/NoPadding" );
			this.clientDecryptCipher = Cipher.getInstance( "AES/CFB/NoPadding" );
		} catch ( NoSuchAlgorithmException | NoSuchPaddingException e ) {
			this.logger.error( "Failed to retrieve AES Cipher instance", e );
			return false;
		}
		
		try {
			this.clientEncryptCipher.init( Cipher.ENCRYPT_MODE, secret, spec );
			this.clientDecryptCipher.init( Cipher.DECRYPT_MODE, secret, spec );
		} catch ( InvalidKeyException | InvalidAlgorithmParameterException e ) {
			this.logger.error( "Failed to setup AES Cipher instances", e );
			return false;
		}
		
		this.clientInitializationVector = iv;
		return true;
	}
	
	public byte[] decryptClientside( byte[] input, int offset, int length ) {
		if ( this.clientDecryptCipher == null ) {
			return input;
		}
		
		try ( BufferedWriter writer = new BufferedWriter( new FileWriter( "client.encrypted.bin" ) ) ) {
			writer.write( Base64.getEncoder().encodeToString( Arrays.copyOfRange( input, offset, length ) ) );
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		
		try {
			return this.clientDecryptCipher.doFinal( input, offset, length );
		} catch ( IllegalBlockSizeException | BadPaddingException e ) {
			this.logger.error( "Failed to decrypt client-side packet data", e );
			return new byte[0];
		}
	}
	
	/**
	 * Gets the initialization vector used for initializing the client cipher instance.
	 *
	 * @return The initialization vector used for initializing the client's cipher
	 */
	public byte[] getClientInitializationVector() {
		return this.clientInitializationVector;
	}
	
	
	private SecretKey generateSharedSecret( PrivateKey privateKey, PublicKey publicKey ) {
		KeyAgreement agreement;
		try {
			agreement = KeyAgreement.getInstance( "ECDH", "BC" );
		} catch ( NoSuchAlgorithmException | NoSuchProviderException e ) {
			System.err.println( "Could not find BouncyCastle Key Provider - please ensure that you have installed BouncyCastle properly" );
			System.exit( -1 );
			return null;
		}
		
		try {
			agreement.init( privateKey );
			agreement.doPhase( publicKey, true );
		} catch ( InvalidKeyException e ) {
			this.logger.error( "Failed to generate key agreement for server-side encryption", e );
			return null;
		}
		
		SecretKey secret;
		try {
			secret = agreement.generateSecret( "AES" );
		} catch ( NoSuchAlgorithmException | InvalidKeyException e ) {
			this.logger.error( "Failed to generate key agreement secret for AES cipher", e );
			return null;
		}
		
		try {
			MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
		} catch ( NoSuchAlgorithmException e ) {
			e.printStackTrace();
		}
		
		byte[] truncated = new byte[16];
		System.arraycopy( secret.getEncoded(), 0, truncated, 0, 16 );
		
		return new SecretKeySpec( truncated, "AES" );
	}
	
	/**
	 * Parses the specified JSON string and ensures it is a JSONObject.
	 *
	 * @param jwt The string to parse
	 *
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
	
}
