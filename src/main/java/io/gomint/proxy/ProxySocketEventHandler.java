/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy;

import io.gomint.jraknet.ClientSocket;
import io.gomint.jraknet.Socket;
import io.gomint.jraknet.SocketEvent;
import io.gomint.jraknet.SocketEventHandler;

/**
 * @author BlackyPaw
 * @version 1.0
 */
class ProxySocketEventHandler implements SocketEventHandler {

	private final Proxy proxy;

	public ProxySocketEventHandler( Proxy proxy ) {
		this.proxy = proxy;
	}

	@Override
	public void onSocketEvent( Socket socket, SocketEvent event ) {
		switch ( event.getType() ) {
			case NEW_INCOMING_CONNECTION:
				this.proxy.notifyNewIncomingConnection( event.getConnection() );
				break;

			case CONNECTION_CLOSED:
			case CONNECTION_DISCONNECTED:
				this.proxy.notifyConnectionClosed( event.getConnection() );
				break;

			case CONNECTION_ATTEMPT_FAILED:
				this.proxy.notifyConnectionAttemptFailed( event.getReason() );
				break;

			case CONNECTION_ATTEMPT_SUCCEEDED:
				this.proxy.notifyConnectionAttemptSucceeded( ( (ClientSocket) socket ).getConnection() );
				break;
		}
	}

}
