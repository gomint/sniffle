package io.gomint.proxy.jwt;

import org.json.simple.JSONObject;

import java.util.Map;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class JwtHeader {
	
	private JwtAlgorithm        algorithm;
	private Map<String, Object> properties;
	
	JwtHeader( Map<String, Object> properties ) {
		this.properties = properties;
		this.algorithm = JwtAlgorithm.getByName( this.getProperty( String.class, "alg" ) );
	}
	
	/**
	 * Returns the algorithm used secure the token this is the header of. Might be null in case no {@code alg} field
	 * was specified in the token's header or the specified algorithm is not supported.
	 *
	 * @return The algorithm used to secure the associated token or null if the algorithm was not specified or is not
	 * supported
	 */
	public JwtAlgorithm getAlgorithm() {
		return this.algorithm;
	}
	
	/**
	 * Gets the specified property if it was contained in the associated JWT token's header field or null if it was
	 * not specified.
	 *
	 * @param key The name of the property to get
	 *
	 * @return The property if found or null otherwise
	 */
	public Object getProperty( String key ) {
		return this.properties.get( key );
	}
	
	/**
	 * Gets the specified property if it was contained in the associated JWT token's header field and is of the
	 * specified type or null if it was not specified or is of a differnt type.
	 *
	 * @param clazz The class of the type to ensure
	 * @param key   The name of the property to get
	 * @param <T>   The generic type parameter matching {@code clazz}
	 *
	 * @return The property if found or null otherwise
	 */
	public <T> T getProperty( Class<T> clazz, String key ) {
		Object value = this.properties.get( key );
		if ( value == null || !clazz.isAssignableFrom( value.getClass() ) ) {
			return null;
		}
		return clazz.cast( value );
	}
	
	/**
	 * Serializes the JwtHeader to a JSONObject.
	 *
	 * @return The JwtHeader represented as a JSONObject
	 */
	JSONObject serializeToJson() {
		return new JSONObject( this.properties );
	}
	
}
