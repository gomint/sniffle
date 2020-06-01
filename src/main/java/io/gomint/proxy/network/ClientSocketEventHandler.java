/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.network;

import io.gomint.jraknet.Socket;
import io.gomint.jraknet.SocketEvent;
import io.gomint.jraknet.SocketEventHandler;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class ClientSocketEventHandler implements SocketEventHandler {

    private final ConnectionManager connectionManager;

    public ClientSocketEventHandler( ConnectionManager connectionManager ) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void onSocketEvent( Socket socket, SocketEvent event ) {
        switch ( event.getType() ) {
            case NEW_INCOMING_CONNECTION:
                this.connectionManager.prepareIncomingConnection( event.getConnection() );
                break;

            case CONNECTION_CLOSED:
            case CONNECTION_DISCONNECTED:
                this.connectionManager.notifyClientDisconnected( event.getConnection() );
                break;

            case UNCONNECTED_PING:
                this.handleUnconnectedPing( event );
                break;
        }
    }

    private void handleUnconnectedPing( SocketEvent event ) {
        // Fire ping event so plugins can modify the motd and player amounts
        event.getPingPongInfo().setMotd( "MCPE;MITM Proxy;313;1.8.0.14;0;1" );
    }

}
