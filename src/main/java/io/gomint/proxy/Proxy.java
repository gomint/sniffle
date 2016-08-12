/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy;

import io.gomint.jraknet.ServerSocket;
import io.gomint.jraknet.SocketEventHandler;
import io.gomint.proxy.network.ClientSocketEventHandler;
import io.gomint.proxy.network.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class Proxy {
	
	private final Logger logger = LoggerFactory.getLogger( Proxy.class );
	
	private final InetSocketAddress fallbackServer;
	
	private final ConnectionManager connectionManager;
	
	private final ServerSocket       server;
	private final SocketEventHandler socketEventHandler;
	
	Proxy( String host, int port ) {
		this.fallbackServer = new InetSocketAddress( host, port );
		
		this.connectionManager = new ConnectionManager( this );
		
		this.socketEventHandler = new ClientSocketEventHandler( this.connectionManager );
		this.server = new ServerSocket( 1 );
	}
	
	// ======================================== GENERAL ======================================== //
	
	/**
	 * Attempts to bind the proxy to the given hostname and port.
	 *
	 * @param host The hostname to bind the proxy to
	 * @param port The port to bind the proxy to
	 *
	 * @throws SocketException Thrown if the underlying server socket could not be bound
	 */
	public void bind( String host, int port ) throws SocketException {
		this.server.setEventHandler( this.socketEventHandler );
		this.server.bind( host, port );
		this.logger.info( "Started server" );
	}
	
	/**
	 * Gets the address of the fallback server.
	 *
	 * @return The address of the fallback server
	 */
	public InetSocketAddress getFallbackServer() {
		return this.fallbackServer;
	}
	
}
