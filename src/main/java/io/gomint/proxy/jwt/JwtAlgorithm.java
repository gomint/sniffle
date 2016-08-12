package io.gomint.proxy.jwt;

/**
 * Enumeration of supported encryption schemes.
 *
 * @author BlackyPaw
 * @version 1.0
 */
public enum JwtAlgorithm {
	
	ES384( new JwtSignatureES384() );
	
	/**
	 * Exception-silent variant of .valueOf( ... ).
	 *
	 * @param name The name of the JwtAlgorithm to get
	 *
	 * @return The JwtAlgorithm constant resembling the algorithm with the specified name or null if no one matched
	 */
	public static JwtAlgorithm getByName( String name ) {
		switch ( name ) {
			case "ES384":
				return ES384;
			default:
				return null;
		}
	}
	
	private final JwtSignature validator;
	
	JwtAlgorithm( JwtSignature validator ) {
		this.validator = validator;
	}
	
	/**
	 * Returns the name of this algorithm when specified inside the {@code alg} field of a JWT token's header.
	 *
	 * @return The name of this algorithm for specification inside the {@code alg} field
	 */
	public String getJwtName() {
		return this.name();
	}
	
	/**
	 * Gets the signature to be used for validating and signing signatures when using this algorithm.
	 *
	 * @return The signature for use with this algorithm
	 */
	public JwtSignature getSignature() {
		return this.validator;
	}
	
}
