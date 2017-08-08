package io.gomint.proxy.network;

import io.gomint.jraknet.Connection;
import io.gomint.proxy.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class ConnectionManager {
	
	private final Logger logger = LoggerFactory.getLogger( ConnectionManager.class );
	private final Proxy  proxy;
	private final Thread updateThread;
	
	private boolean                            closed;
	private Map<Connection, ProxiedConnection> proxiesByClientConnection;
	
	public ConnectionManager( Proxy proxy ) {
		this.proxy = proxy;
		this.updateThread = new Thread( this::updateLoop );
		this.updateThread.setName( "NetworkWorker" );
		this.closed = false;
		this.proxiesByClientConnection = new ConcurrentHashMap<>();
		
		this.updateThread.start();
	}
	
	public Proxy getProxy() {
		return this.proxy;
	}
	
	public void prepareIncomingConnection( Connection clientConnection ) {
		ProxiedConnection proxiedConnection = new ProxiedConnection( this, clientConnection );
		this.proxiesByClientConnection.put( clientConnection, proxiedConnection );
	}
	
	public void notifyClientDisconnected( Connection clientConnection ) {
		this.logger.info( "Client disconnected " + clientConnection.getAddress() );
		ProxiedConnection connection = this.proxiesByClientConnection.remove( clientConnection );
		if ( connection != null ) {
			connection.performCleanup();
		}
	}
	
	public void close() {
		this.closed = true;
	}
	
	private void updateLoop() {
		while ( !this.closed ) {
			for ( ProxiedConnection connection : this.proxiesByClientConnection.values() ) {
				connection.update();
			}
			
			try {
				Thread.sleep( 10L );
			} catch ( InterruptedException ignored ) {
				// ._.
			}
		}
	}
	
}
