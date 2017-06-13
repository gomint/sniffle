/*
 *  Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 *  This code is licensed under the BSD license found in the
 *  LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.jwt;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class MojangChainValidator {

    private static final String MOJANG_TRUSTED_PUBLIC_KEY_BASE64 = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V";
    private static final PublicKey MOJANG_TRUSTED_PUBLIC_KEY;
    private static final KeyFactory ECDH_KEY_FACTORY;

    static {
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance( "ECDH", "BC" );
        } catch ( NoSuchAlgorithmException e ) {
            e.printStackTrace();
            System.err.println( "Could not find ECDH Key Factory - please ensure that you have installed the latest version of BouncyCastle" );
            System.exit( -1 );
        } catch ( NoSuchProviderException e ) {
            e.printStackTrace();
            System.err.println( "Could not find BouncyCastle Key Provider - please ensure that you have installed BouncyCastle properly" );
            System.exit( -1 );
        }
        ECDH_KEY_FACTORY = keyFactory;

        MOJANG_TRUSTED_PUBLIC_KEY = createPublicKey( MOJANG_TRUSTED_PUBLIC_KEY_BASE64 );
        if ( MOJANG_TRUSTED_PUBLIC_KEY == null ) {
            System.err.println( "Could not generated public key for trusted Mojang key; please report this error in the GoMin.io forums for further assistance" );
            System.exit( -1 );
        }
    }

    private static PublicKey createPublicKey( String base64 ) {
        try {
            return ECDH_KEY_FACTORY.generatePublic( new X509EncodedKeySpec( Base64.getDecoder().decode( base64 ) ) );
        } catch ( InvalidKeySpecException e ) {
            e.printStackTrace();
            return null;
        }
    }

    private List<JwtToken> chain;

    private String username;
    private UUID uuid;
    private String xboxId;
    private ECPublicKey clientPublicKey;
    private Map<String, PublicKey> trustedKeys = new HashMap<>();

    public MojangChainValidator() {
        this.chain = new ArrayList<>();
    }

    public void addToken( JwtToken token ) {
        this.chain.add( token );
    }

    public String getUsername() {
        return this.username;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public String getXboxId() {
        return this.xboxId;
    }

    public ECPublicKey getClientPublicKey() {
        return this.clientPublicKey;
    }

    public boolean validate() {
        trustedKeys.put( MOJANG_TRUSTED_PUBLIC_KEY_BASE64, MOJANG_TRUSTED_PUBLIC_KEY );

        List<JwtToken> unverified = new ArrayList<>( this.chain );
        boolean hasExtraData = false;

        try {
            while ( !unverified.isEmpty() ) {
                // Take advantage of the 'x5u' header field Mojang sends us with their chains
                // This will tell us using which key a specific token has been signed and we can thus
                // easily find the token in the chain which comes circularly dependant on its own claim.
                // We hook into that dependency using what seems to be a constant non-changing public key
                // of Mojang:

                String x5u = null;
                JwtToken nextToken = null;

                for ( JwtToken token : unverified ) {
                    x5u = token.getHeader().getProperty( String.class, "x5u" );
                    if ( x5u == null ) {
                        // This token comes unexpectedly - might be a faker:
                        return false;
                    }

                    if ( trustedKeys.containsKey( x5u ) ) {
                        nextToken = token;
                        break;
                    }
                }

                if ( nextToken == null ) {
                    // No further tokens which could be verified -> yet there are still tokens in the unverified set:
                    return false;
                }

                try {
                    // We always use ES384 independently of what the client sent us in order to prevent algorithm exchange
                    // attacks as described here: https://auth0.com/blog/critical-vulnerabilities-in-json-web-token-libraries/
                    if ( !nextToken.validateSignature( JwtAlgorithm.ES384, trustedKeys.get( x5u ) ) ) {
                        // Seems to be a forged token:
                        return false;
                    }
                } catch ( JwtSignatureException e ) {
                    e.printStackTrace();
                    return false;
                }

                unverified.remove( nextToken );

                // This token is valid -> add its public key to the set of trusted keys if it specifies any and has its
                // certificateAuthority flag set:
                Boolean certificateAuthority = nextToken.getClaim( Boolean.class, "certificateAuthority" );
                String identityPublicKeyBase64 = nextToken.getClaim( String.class, "identityPublicKey" );

                if ( ( certificateAuthority != null && !certificateAuthority ) || identityPublicKeyBase64 == null ) {
                    // No public key to trust here:
                    continue;
                }

                // This certificate authority wants us to add its public key:
                PublicKey key = createPublicKey( identityPublicKeyBase64 );
                if ( key != null ) {
                    trustedKeys.put( identityPublicKeyBase64, key );
                }

                // Check, if this token provides us with client details:
                Map<String, Object> extraData = nextToken.getClaim( Map.class, "extraData" );
                if ( extraData != null && !hasExtraData ) {
                    hasExtraData = true;
                    this.clientPublicKey = (ECPublicKey) key;
                    this.loadClientInformation( extraData, false );
                }
            }

            return true;
        } finally {
            if ( !hasExtraData ) {
                this.detectClientInformationUnsafe();
            }
        }
    }

    private void detectClientInformationUnsafe() {
        for ( JwtToken token : this.chain ) {
            String identityPublicKeyBase64 = token.getClaim( String.class, "identityPublicKey" );
            if ( identityPublicKeyBase64 == null ) {
                continue;
            }

            Map<String, Object> extraData = token.getClaim( Map.class, "extraData" );
            if ( extraData != null ) {
                PublicKey key = createPublicKey( identityPublicKeyBase64 );
                if ( key == null ) {
                    continue;
                }

                this.clientPublicKey = (ECPublicKey) key;
                this.loadClientInformation( extraData, true );
                return;
            }
        }
    }

    private void loadClientInformation( Map<String, Object> extraData, boolean unsafe ) {
        Object usernameRaw = extraData.get( "displayName" );
        Object uuidRaw = extraData.get( "identity" );
        Object xuidRaw = extraData.get( "XUID" );

        if ( usernameRaw != null && usernameRaw instanceof String ) {
            this.username = (String) usernameRaw;
        }

        if ( uuidRaw != null && uuidRaw instanceof String ) {
            try {
                this.uuid = UUID.fromString( (String) uuidRaw );
            } catch ( IllegalArgumentException ignored ) {
                // ._.
            }
        }

        if ( !unsafe && xuidRaw != null && xuidRaw instanceof String ) {
            this.xboxId = (String) xuidRaw;
        }
    }

    public Map<String, PublicKey> getTrustedKeys() {
        return this.trustedKeys;
    }

}
