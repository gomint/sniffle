package io.gomint.proxy;

import io.gomint.jraknet.ClientSocket;
import io.gomint.jraknet.Connection;
import io.gomint.jraknet.ServerSocket;
import io.gomint.jraknet.SocketEventHandler;

import java.net.SocketException;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class Proxy {

	private ServerSocket server;
	private SocketEventHandler socketEventHandler;
	private ClientSocket client;

	public Proxy() {
		this.socketEventHandler = new ProxySocketEventHandler( this );
	}

	// ======================================== GENERAL ======================================== //

	/**
	 * Attempts to bind the proxy to the given hostname and port.
	 *
	 * @param host The hostname to bind the proxy to
	 * @param port The port to bind the proxy to
	 * @throws SocketException Thrown if the underlying server socket could not be bound
	 */
	public void bind( String host, int port ) throws SocketException {
		this.server.setEventHandler( this.socketEventHandler );
		this.server.bind( host, port );
	}

	// ======================================== SOCKET EVENTS ======================================== //

	void notifyNewIncomingConnection( Connection connection ) {
		if ( this.client != null ) {
			connection.disconnect( "Player limit reached" );
			return;
		}

		this.
	}

	void notifyConnectionClosed( Connection connection ) {

	}

	void notifyConnectionAttemptFailed( String reason ) {

	}

	void notifyConnectionAttemptSucceeded( Connection connection ) {

	}

}
