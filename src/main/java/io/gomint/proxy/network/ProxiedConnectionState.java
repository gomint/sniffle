package io.gomint.proxy.network;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public enum ProxiedConnectionState {
	
	/**
	 * The connection is lingering, i.e. a client connection is associated with it but
	 * no connection to a backend server has yet been established.
	 * <p>
	 * The proxy will try to re-connect the client to a backend server if the connection is
	 * in this state and will close it if all such attempts fail.
	 */
	LINGERING,
	
	/**
	 * The connection is fully established meaning there is a backlink to a backend server as
	 * well as an open client connection.
	 */
	ESTABLISHED,
	
	/**
	 * The connection has been closed entirely.
	 */
	CLOSED;
	
}
