/*
 *  Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 *  This code is licensed under the BSD license found in the
 *  LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.jwt;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;

/**
 * Helper class to create a non-authenticated JWT chain.
 *
 * @author BlackyPaw
 * @version 1.0
 */
public class MojangLoginForger {
	
	private String    username;
	private UUID      uuid;
	private PublicKey publicKey;
	
	public void setUsername( String username ) {
		this.username = username;
	}
	
	public void setUUID( UUID uuid ) {
		this.uuid = uuid;
	}
	
	public void setPublicKey( PublicKey publicKey ) {
		this.publicKey = publicKey;
	}
	
	@SuppressWarnings( "unchecked" )
	public String forge( PrivateKey privateKey ) {
		final JwtAlgorithm algorithm = JwtAlgorithm.ES384;
		
		// Convert our public key to Base64:
		String publicKeyBase64 = Base64.getEncoder().encodeToString( this.publicKey.getEncoded() );
		
		// Construct JSON WebToken:
		JSONObject header = new JSONObject();
		header.put( "alg", algorithm.getJwtName() );
		header.put( "x5u", publicKeyBase64 );
		
		long timestamp = System.currentTimeMillis() / 1000L;
		
		JSONObject claims = new JSONObject();
		claims.put( "nbf", timestamp );
		claims.put( "exp", timestamp + 1L );
		
		JSONObject extraData = new JSONObject();
		extraData.put( "displayName", this.username );
		extraData.put( "identity", this.uuid.toString() );
		claims.put( "extraData", extraData );
		
		claims.put( "identityPublicKey", publicKeyBase64 );
		
		StringBuilder builder = new StringBuilder();
		builder.append( Base64.getUrlEncoder().encodeToString( header.toJSONString().getBytes( StandardCharsets.UTF_8 ) ) );
		builder.append( '.' );
		builder.append( Base64.getUrlEncoder().encodeToString( claims.toJSONString().getBytes( StandardCharsets.UTF_8 ) ) );
		
		// Sign the token:
		byte[] signatureBytes = builder.toString().getBytes( StandardCharsets.US_ASCII );
		byte[] signatureDigest;
		try {
			signatureDigest = algorithm.getSignature().sign( privateKey, signatureBytes );
		} catch ( JwtSignatureException e ) {
			e.printStackTrace();
			return null;
		}
		
		builder.append( '.' );
		builder.append( Base64.getUrlEncoder().encodeToString( signatureDigest ) );
		
		return builder.toString();
	}
	
}
