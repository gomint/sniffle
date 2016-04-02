package io.gomint.proxy;

import io.gomint.jraknet.ClientSocket;
import io.gomint.jraknet.Connection;
import io.gomint.jraknet.ServerSocket;
import io.gomint.jraknet.SocketEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class Proxy {

	private static final Logger LOGGER = LoggerFactory.getLogger( Proxy.class );

	private final String host;
	private final int port;

	private final ServerSocket server;
	private final SocketEventHandler socketEventHandler;
	private ClientSocket client;
	private Connection serverConnection;

	private PacketRedirectThread serverReader;
	private PacketRedirectThread clientReader;

	public Proxy(String host, int port) {
		this.host = host;
		this.port = port;

		this.socketEventHandler = new ProxySocketEventHandler( this );
		this.server = new ServerSocket( 1 );
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
		LOGGER.info( "Started server" );
	}

	// ======================================== SOCKET EVENTS ======================================== //

	void notifyNewIncomingConnection( Connection connection ) {
		if ( this.client != null ) {
			connection.disconnect( "Player limit reached" );
			return;
		}

		this.serverConnection = connection;

		this.client = new ClientSocket();
		try {
			this.client.initialize();
		} catch ( SocketException e ) {
			e.printStackTrace();
			this.client = null;
			this.serverConnection.disconnect("Error");
			this.serverConnection = null;
			return;
		}
		this.client.setEventHandler( this.socketEventHandler );
		this.client.connect( host, port );
	}

	void notifyConnectionClosed( Connection connection ) {
		if ( this.client != null && this.client.getConnection() == connection ) {
			// Client connection closed, close server connection too
			if ( this.serverConnection.isConnected() ) {
				this.serverConnection.disconnect( connection.getDisconnectMessage() );
			}

			this.client = null;

			LOGGER.info( "Closed client connection" );
		} else {
			if ( this.client != null ) {
				if ( this.client.getConnection().isConnected() ) {
					// Server connection closed, close client connection too
					this.client.getConnection().disconnect( connection.getDisconnectMessage() );
				}

				this.client = null;

				LOGGER.info( "Closed server connection" );
			}

			this.serverConnection = null;
		}

		if ( this.serverReader != null ) {
			this.serverReader.shutdown();
			this.serverReader = null;
		}

		if ( this.clientReader != null ) {
			this.clientReader.shutdown();
			this.clientReader = null;
		}
	}

	void notifyConnectionAttemptFailed( String reason ) {
		if ( this.serverConnection != null && this.serverConnection.isConnected() ) {
			this.serverConnection.disconnect( "Can't connect to server: " + reason );
		}

		this.serverConnection = null;
		this.client = null;

		LOGGER.info( "Failed to connect to server" );
	}

	void notifyConnectionAttemptSucceeded( Connection connection ) {
		this.serverReader = new PacketRedirectThread( this.serverConnection, connection, true );
		this.clientReader = new PacketRedirectThread( connection, this.serverConnection, false );

		// TODO add packet handlers

		this.serverReader.start();
		this.clientReader.start();

		LOGGER.info( "Redirecting packets between " + this.serverConnection.getAddress() + " and " + connection.getAddress() );
	}

}
