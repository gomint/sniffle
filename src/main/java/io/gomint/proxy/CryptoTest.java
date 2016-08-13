/*
 *  Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 *  This code is licensed under the BSD license found in the
 *  LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

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
import java.nio.charset.StandardCharsets;
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
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class CryptoTest {
	
	private static final String CLIENT_PUBLIC_BASE64 = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEhPXrlN6VCUXANXCvIx4Eq9YW2vdoiNxw6g5yi42PFvvCNmvcPAy4et5tJOGiEYImmGiKda7rwNEkGgbXQtyxX7MgQfbNyyUNbYh6aY2JCBIX3v0r7Px7pkOPyjYSY3ng";
	
	private static final String SERVER_PRIVATE_BASE64 = "MIG/AgEAMBAGByqGSM49AgEGBSuBBAAiBIGnMIGkAgEBBDATbYTeaQufv1/dAV0qAX+jRMmPTuU1kHXMlzcd0lLVOBTJHspe3UgbWxQa9tLfAkOgBwYFK4EEACKhZANiAAQACs1vi2zRWY3t+bTlQF4vfNTLvpAxiju6eePQ5IEbgVsWQQzO/LHpxH03RghWyaRrjZ3aDetSwW606lDtOCYpWgfjN8vC4ciQQq3XWFG/AEdSPYfWOrSTOUvVepy4+u0=";
	private static final String SERVER_PUBLIC_BASE64 = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAEAArNb4ts0VmN7fm05UBeL3zUy76QMYo7unnj0OSBG4FbFkEMzvyx6cR9N0YIVsmka42d2g3rUsFutOpQ7TgmKVoH4zfLwuHIkEKt11hRvwBHUj2H1jq0kzlL1XqcuPrt";
	
	private static final String ENCRYPTED_DATA_BASE64 = "OMDEF5gVD5ewM+J5TmGMG8EPRbKiz71SDA==";
	private static final String ENCRYPTED_IV_BASE64 = "3VRaLBAjz4QrlUgQxt1srA==";
	
	
	private static KeyFactory ECDH_KEY_FACTORY;
	
	static {
		Security.addProvider( new org.bouncycastle.jce.provider.BouncyCastleProvider() );
		
		try {
			ECDH_KEY_FACTORY = KeyFactory.getInstance( "ECDH", "BC" );
		} catch ( NoSuchAlgorithmException | NoSuchProviderException e ) {
			System.exit( -1 );
		}
	}
	
	private static PublicKey createPublicKey( String base64 ) {
		try {
			return ECDH_KEY_FACTORY.generatePublic( new X509EncodedKeySpec( Base64.getDecoder().decode( base64 ) ) );
		} catch ( InvalidKeySpecException e ) {
			throw new AssertionError( "Could not create public key", e );
		}
	}
	
	private static PrivateKey createPrivateKey( String base64 ) {
		try {
			return ECDH_KEY_FACTORY.generatePrivate( new PKCS8EncodedKeySpec( Base64.getDecoder().decode( base64 ) ) );
		} catch ( InvalidKeySpecException e ) {
			throw new AssertionError( "Could not create private key", e );
		}
	}
	
	private static byte[] generateSharedSecret( PrivateKey selfPrivate, PublicKey remotePublic ) {
		KeyAgreement agreement;
		try {
			agreement = KeyAgreement.getInstance( "ECDH", "BC" );
		} catch ( NoSuchAlgorithmException | NoSuchProviderException e ) {
			throw new AssertionError( "Could not get KeyAgreement instance", e );
		}
		
		try {
			agreement.init( selfPrivate );
			agreement.doPhase( remotePublic, true );
		} catch ( InvalidKeyException e ) {
			throw new AssertionError( "Could not generate shared secret", e );
		}
		
		/*
		try {
			SecretKey key = agreement.generateSecret( "AES" );
			return key.getEncoded();
		} catch ( NoSuchAlgorithmException | InvalidKeyException e ) {
			throw new AssertionError( "Could not generate shared secret", e );
		}
		*/
		
		return agreement.generateSecret();
	}
	
	private static byte[] hashSha1( byte[] input ) {
		try {
			MessageDigest digest = MessageDigest.getInstance( "SHA-1" );
			digest.update( input );
			return digest.digest();
		} catch ( NoSuchAlgorithmException e ) {
			throw new AssertionError( "SHA-1 not supported", e );
		}
	}
	
	private static byte[] hashSha256( byte[] input ) {
		try {
			MessageDigest digest = MessageDigest.getInstance( "SHA-256" );
			digest.update( input );
			return digest.digest();
		} catch ( NoSuchAlgorithmException e ) {
			throw new AssertionError( "SHA-256 not supported", e );
		}
	}
	
	private static Cipher createDecryptor( byte[] key, byte[] iv ) {
		SecretKeySpec keySpec = new SecretKeySpec( key, "AES" );
		IvParameterSpec ivSpec = new IvParameterSpec( iv );
		
		Cipher cipher;
		try {
			//cipher = Cipher.getInstance( "AES/CFB/NoPadding" );
			//cipher = Cipher.getInstance( "AES/ECB/NoPadding" );
			//cipher = Cipher.getInstance( "AES/OFB/NoPadding" );
			
			// Why do none of them work D: ?!
			
			cipher = Cipher.getInstance( "AES" );
		} catch ( NoSuchAlgorithmException | NoSuchPaddingException e ) {
			throw new AssertionError( "AES with CFB mode and NoPadding not supported", e );
		}
		
		try {
			cipher.init( Cipher.DECRYPT_MODE, keySpec, ivSpec );
		} catch ( InvalidKeyException | InvalidAlgorithmParameterException e ) {
			throw new AssertionError( "Could not initialize AES cipher", e );
		}
		
		return cipher;
	}
	
	
	public static void main( String[] args ) {
		
		// Test for .NET export:
		try {
			KeyPairGenerator rsaGenerator = KeyPairGenerator.getInstance( "RSA" );
			rsaGenerator.initialize( 4096 );
			KeyPair pair = rsaGenerator.generateKeyPair();
			
			Signature signature = Signature.getInstance("SHA1withRSA", "BC");
			signature.initSign( pair.getPrivate() );
			
			byte[] message = "Hallo, .NET-Welt!".getBytes( StandardCharsets.UTF_8 );
			signature.update( message );
			
			byte[] digest = signature.sign();
			
			try ( BufferedWriter writer = new BufferedWriter( new FileWriter( "rsatest.data" ) ) ) {
				writer.write( Base64.getEncoder().encodeToString( message ) );
				writer.write( '\n' );
				writer.write( Base64.getEncoder().encodeToString( digest ) );
				writer.write( '\n' );
				writer.write( Base64.getEncoder().encodeToString( pair.getPrivate().getEncoded() ) );
				writer.write( '\n' );
				writer.write( Base64.getEncoder().encodeToString( pair.getPublic().getEncoded() ) );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		} catch ( NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | SignatureException e ) {
			e.printStackTrace();
		}
		
		
		
		
		
		
		
		
		PublicKey clientPublic = createPublicKey( CLIENT_PUBLIC_BASE64 );
		// PublicKey serverPublic = createPublicKey( SERVER_PUBLIC_BASE64 );
		PrivateKey serverPrivate = createPrivateKey( SERVER_PRIVATE_BASE64 );
		byte[] iv = Base64.getDecoder().decode( ENCRYPTED_IV_BASE64 );
		
		byte[] secret = generateSharedSecret( serverPrivate, clientPublic );
		
		// The initialization vector being 16 bytes long hints at the cipher to have used
		// 128 bit block sizes. This requires our key to be 128-bit long, too, which is why
		// I thought they might have shortened what they got from ECDH by simply truncating
		// it which was unfortunately not the case. This can only mean that they are hashing
		// the secret in a way to retrieve a digest of the specific length which is why I
		// went on to trying SHA-1 and cutting of the last 4 bytes - didn't work either.
		//
		// So what's left? Trying, trying, trying and maybe we'll know one day what they
		// do to produce their shared secret:
		
		
		// Some time later...
		//    ...
		//    ...
		//    ...
		//    ...
		//    ...
		// (all of the above ...s indicate me failing for 4 - 6 hours straight - just like a prisoner keeps track
		// of his days inside his cell by scribbling into his cell's walls...)
		
		// Might the folks at Mojang have read this specific question on StackOverflow? Let's find out!
		// http://security.stackexchange.com/questions/52376/how-to-treat-hash-ecdh-shared-secret-to-use-as-keys-for-aes
		byte[] secret2 = new byte[iv.length + secret.length];
		System.arraycopy( iv, 0, secret2, 0, iv.length );
		System.arraycopy( secret, 0, secret2, iv.length, secret.length );
		
		byte[] digest = hashSha256( secret2 );
		byte[] iv2 = Arrays.copyOfRange( digest, 0, 16 );
		
		byte[] encrypted = Base64.getDecoder().decode( ENCRYPTED_DATA_BASE64 );
		
		
		
		// God Damnit! Even after trying all different block modes AES just won't work!
		
		// ...
		
		// Aha! Found out, that one might need to use some unrestricted policy files which allow AES to use
		// keys longer than 16 bytes - just as what we would get from SHA-256 if Mojang really read that
		// StackOverflow post
		
		
		// ...
		
		
		// One further attempt: use BouncyCastle directly to try out some parameters regarding AES (setting block size,
		// feedback size and such directly...)
		// Use BouncyCastle directly:
		BufferedBlockCipher cipher = new BufferedBlockCipher( new CFBBlockCipher( new RijndaelEngine( 128 ), 8 ) );
		cipher.init( false, new ParametersWithIV( new KeyParameter( digest ), iv2 ) );
		
		byte[] tmp = new byte[256];
		int offset = cipher.processBytes( encrypted, 0, encrypted.length, tmp, 0  );
		try {
			cipher.doFinal( tmp, offset );
		} catch ( InvalidCipherTextException e ) {
			e.printStackTrace();
		}
		
	}
	
}
