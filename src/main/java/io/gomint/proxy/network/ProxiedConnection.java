package io.gomint.proxy.network;

import io.gomint.jraknet.ClientSocket;
import io.gomint.jraknet.Connection;
import io.gomint.jraknet.EncapsulatedPacket;
import io.gomint.jraknet.PacketBuffer;
import io.gomint.jraknet.PacketReliability;
import io.gomint.jraknet.Socket;
import io.gomint.jraknet.SocketEvent;
import io.gomint.jraknet.SocketEventHandler;
import io.gomint.proxy.jwt.MojangLoginForger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class ProxiedConnection {
	
	private final Logger logger = LoggerFactory.getLogger( ProxiedConnection.class );
	
	// Client:
	private final Connection clientConnection;
	private       int        clientProtocol;
	private       byte[]     clientSkin;
	
	// Server:
	private ClientSocket proxySocket;
	private Connection   proxiedConnection;
	
	// Miscellaneous:
	private final ConnectionManager connectionManager;
	private       Inflater          cachedInflater;
	private       Deflater          cachedDeflater;
	private       byte[]            intermediateBuffer;
	
	private EncryptionHandler encryptionHandler;
	
	/**
	 * Constructs a new ProxiedConnection wrapping the given client connection.
	 *
	 * @param connectionManager The connection manager that created this connection
	 * @param clientConnection  The client connection to wrap
	 */
	public ProxiedConnection( ConnectionManager connectionManager, Connection clientConnection ) {
		this.clientConnection = clientConnection;
		this.connectionManager = connectionManager;
		
		this.cachedInflater = new Inflater();
		this.cachedDeflater = new Deflater();
		this.intermediateBuffer = new byte[1024];
		
		this.encryptionHandler = new EncryptionHandler();
	}
	
	
	public void sendToClient( Packet packet ) {
		this.send( this.clientConnection, packet );
	}
	
	public void sendToServer( Packet packet ) {
		this.send( this.proxiedConnection, packet );
	}
	
	private void send( Connection connection, Packet packet ) {
		int estimateLength = packet.estimateLength();
		PacketBuffer buffer = new PacketBuffer( estimateLength == -1 ? 64 : estimateLength + 2 );
		buffer.writeByte( (byte) 0xFE );
		buffer.writeByte( packet.getId() );
		packet.serialize( buffer );
		
		byte[] raw;
		if ( buffer.getRemaining() != 0 ) {
			raw = new byte[ buffer.getPosition() ];
			System.arraycopy( buffer.getBuffer(), buffer.getBufferOffset(), raw, 0, buffer.getPosition() );
		} else {
			raw = buffer.getBuffer();
		}
		
		connection.send( PacketReliability.RELIABLE, raw );
	}
	
	/**
	 * Updates the proxied connection by polling all network packets which have been received since the last
	 * invocation of this method.
	 */
	public void update() {
		if ( !this.clientConnection.isConnected() ) {
			// Nothing to updateLoop - we are not yet or no longer ready:
			return;
		}
		
		// Poll network packets and handle them accordingly:
		EncapsulatedPacket packet;
		
		while ( ( packet = this.clientConnection.receive() ) != null ) {
			this.handleServerboundPacketRaw( packet );
		}
		
		if ( this.proxiedConnection != null && this.proxiedConnection.isConnected() ) {
			while ( ( packet = this.proxiedConnection.receive() ) != null ) {
				this.handleClientboundPacketRaw( packet );
			}
		}
	}
	
	/**
	 * Handles some raw network data received from the proxied connection, i.e. data sent from the
	 * proxied server to the client.
	 *
	 * @param raw The raw data received
	 */
	public void handleClientboundPacketRaw( EncapsulatedPacket raw ) {
		PacketBuffer buffer = new PacketBuffer( raw.getPacketData(), 0 );
		
		while ( buffer.getRemaining() > 0 ) {
			byte packetID = this.extractRealPacketID( buffer );
			if ( packetID == PacketRegistry.PACKET_BATCH ) {
				for ( Packet packet : this.extractBatchPacket( buffer ) ) {
					this.handleClientboundPacket( packet );
				}
			} else {
				System.out.println( "Received packet with ID " + packetID + " from server" );
				Packet packet = PacketRegistry.createFromID( packetID );
				if ( packet != null ) {
					packet.deserialize( buffer );
					this.handleClientboundPacket( packet );
				} else {
					// Unknown packet -> direct passthrough:
					
					// TODO: Implement direct passthrough here
					buffer.skip( buffer.getRemaining() );
				}
			}
		}
	}
	
	/**
	 * Handles some raw network data received from the client connection, i.e. data sent from the
	 * client to the proxied server.
	 *
	 * @param raw The raw data received
	 */
	public void handleServerboundPacketRaw( EncapsulatedPacket raw ) {
		byte[] data = raw.getPacketData();
		if ( data.length <= 0 ) {
			return;
		}
		
		int offset = 0;
		if ( data[0] == (byte) 0xFE ) {
			offset++;
		}
		
		byte[] decrypted = this.encryptionHandler.decryptClientside( data, offset, data.length - offset );
		PacketBuffer buffer = new PacketBuffer( decrypted, 0 );
		
		while ( buffer.getRemaining() > 0 ) {
			byte packetID = this.extractRealPacketID( buffer );
			if ( packetID == PacketRegistry.PACKET_BATCH ) {
				for ( Packet packet : this.extractBatchPacket( buffer ) ) {
					this.handleServerboundPacket( packet );
				}
			} else {
				System.out.println( "Received packet with ID " + packetID + " from client" );
				Packet packet = PacketRegistry.createFromID( packetID );
				if ( packet != null ) {
					packet.deserialize( buffer );
					this.handleServerboundPacket( packet );
				} else {
					// Unknown packet -> direct passthrough:
					
					// TODO: Implement direct passthrough here
					buffer.skip( buffer.getRemaining() );
				}
			}
		}
	}
	
	/**
	 * Attempts to connect to the specified backend server, disconnecting the client if the connection attempt fails.
	 *
	 * @param address The address of the backend server to connect to
	 */
	public void connectToBackendServer( InetSocketAddress address ) {
		if ( this.proxySocket != null ) {
			this.proxySocket.close();
			this.proxiedConnection = null;
		}
		
		this.proxySocket = new ClientSocket();
		try {
			this.proxySocket.initialize();
		} catch ( SocketException e ) {
			this.logger.error( "Failed to establish connection to backend server: " + address.toString(), e );
			
			// Disconnect client -> will cause connection manager to remove this proxied connection from its maps:
			this.clientConnection.disconnect( "Failed to connect to backend server" );
			return;
		}
		
		this.proxySocket.setEventHandler( new ProxySocketEventHandler() );
		this.proxySocket.connect( address );
	}
	
	/**
	 * Returns the connection's current state.
	 *
	 * @return The connection's current state
	 */
	public ProxiedConnectionState getState() {
		return ( this.clientConnection.isConnected() ? ( this.proxiedConnection != null && this.proxiedConnection.isConnected() ? ProxiedConnectionState.ESTABLISHED : ProxiedConnectionState.LINGERING ) : ProxiedConnectionState.CLOSED );
	}
	
	/**
	 * Disconnects the client associated with this ProxiedConnection and closes the connection to the proxied
	 * server if an such has been opened.
	 *
	 * @param reason The disconnect reason to be sent to the client (not the proxied server)
	 */
	public void disconnect( String reason ) {
		if ( this.clientConnection.isConnected() ) {
			this.clientConnection.disconnect( reason );
		}
		
		if ( this.proxiedConnection != null && this.proxiedConnection.isConnected() ) {
			this.proxiedConnection.disconnect( "" );
			this.proxiedConnection = null;
		}
		
		if ( this.proxySocket != null ) {
			this.proxySocket.close();
			this.proxySocket = null;
		}
	}
	
	/**
	 * Cleans up any internally allocated resources.
	 */
	protected void performCleanup() {
		this.cachedInflater.end();
		this.cachedDeflater.end();
	}
	
	/**
	 * Handles a decoded packet that was sent from the proxied server to the client.
	 *
	 * @param packet The decoded packet
	 */
	private void handleClientboundPacket( Packet packet ) {
		switch ( packet.getId() ) {
			case PacketRegistry.PACKET_SERVER_HANDSHAKE:
				this.handleServerHandshake( (PacketServerHandshake) packet );
				break;
		}
		System.out.println( "Received packet with ID " + packet.getId() + " from server" );
	}
	
	/**
	 * Handles a decoded packet that was sent from the client to the proxied server.
	 *
	 * @param packet The decoded packet
	 */
	private void handleServerboundPacket( Packet packet ) {
		switch ( packet.getId() ) {
			case PacketRegistry.PACKET_CLIENT_HANDSHAKE:
				this.handleClientHandshake( (PacketClientHandshake) packet );
				break;
		}
		System.out.println( "Received packet with ID " + packet.getId() + " from client" );
	}
	
	/**
	 * Handles a client's handshake packet by setting up the client's side of encryption, if necessary.
	 *
	 * @param packet The handshake packet that was received
	 */
	private void handleClientHandshake( PacketClientHandshake packet ) {
		this.clientProtocol = packet.getProtocolVersion();
		this.clientSkin = packet.getSkin();
		this.encryptionHandler.supplyClientJWTChain( packet.getJwt() );
		if ( this.encryptionHandler.hasObligatoryInformation() ) {
			this.connectToBackendServer( this.connectionManager.getProxy().getFallbackServer() );
		} else {
			this.clientConnection.disconnect( "Invalid Handshake" );
		}
	}
	
	/**
	 * Handles the handshake of the proxied server by setting up the server's side of encryption.
	 *
	 * @param packet The handshake we received from the proxied server
	 */
	private void handleServerHandshake( PacketServerHandshake packet ) {
		this.encryptionHandler.setServerPublicKey( packet.getPublicKeyBase64() );
		this.encryptionHandler.beginServersideEncryption( packet.getInitializationVector() );
		
		this.encryptionHandler.beginClientsideEncryption();
		
		PacketServerHandshake handshake = new PacketServerHandshake();
		handshake.setPublicKeyBase64( Base64.getEncoder().encodeToString( EncryptionHandler.PROXY_KEY_PAIR.getPublic().getEncoded() ) );
		handshake.setInitializationVector( this.encryptionHandler.getClientInitializationVector() );
		this.sendToClient( handshake );
		
		// Tell the server that we are ready to receive encrypted packets from now on:
		PacketEncryptionReady response = new PacketEncryptionReady();
		// this.sendToServer( response );
	}
	
	/**
	 * Invoked whenever our proxied server connection got ready and we should now send a handshake and such.
	 */
	@SuppressWarnings( "unchecked" )
	private void notifyProxiedConnectionAvailable() {
		// Successfully connected to proxied server -> send handshake:
		PacketClientHandshake handshake = new PacketClientHandshake();
		handshake.setProtocolVersion( this.clientProtocol );
		
		// Generate our own JWT chain:
		MojangLoginForger forger = new MojangLoginForger();
		forger.setUsername( this.encryptionHandler.getClientUsername() );
		forger.setUUID( this.encryptionHandler.getClientUUID() );
		forger.setPublicKey( EncryptionHandler.PROXY_KEY_PAIR.getPublic() );
		String jwtToken = forger.forge( EncryptionHandler.PROXY_KEY_PAIR.getPrivate() );
		
		JSONObject root  = new JSONObject();
		JSONArray  chain = new JSONArray();
		chain.add( jwtToken );
		root.put( "chain", chain );
		
		String jwtChain = root.toJSONString();
		
		handshake.setJwt( jwtChain );
		handshake.setSkin( this.clientSkin );
		
		// Send our handshake to the server -> this will trigger it to respond with a 0x03 ServerHandshake packet:
		this.sendToServer( handshake );
	}
	
	/**
	 * Interprets the next one to two bytes as a packet ID and returns the actual packet ID without the
	 * optional 0xFE prefix.
	 *
	 * @param buffer The packet buffer to read data from
	 *
	 * @return The actual packet ID extracted from the given buffer
	 */
	private byte extractRealPacketID( PacketBuffer buffer ) {
		byte packetId = buffer.readByte();
		if ( packetId == (byte) 0xFE ) {
			return buffer.readByte();
		}
		return packetId;
	}
	
	/**
	 * Extracts all packets contained in the batch packet encoded in the specified bufer.
	 *
	 * @param buffer The buffer to read the batch packet from
	 *
	 * @return All packets found inside the given batch packet
	 */
	private Collection<Packet> extractBatchPacket( PacketBuffer buffer ) {
		int payloadLength = buffer.readInt();
		if ( buffer.getRemaining() < payloadLength ) {
			this.logger.warn( "Received invalid batch packet - missing some payload" );
			return new ArrayList<>( 0 );
		}
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		try {
			this.cachedInflater.reset();
			this.cachedInflater.setInput( buffer.getBuffer(), buffer.getBufferOffset() + buffer.getPosition(), payloadLength );
			while ( !this.cachedInflater.finished() ) {
				int decompressed = this.cachedInflater.inflate( this.intermediateBuffer );
				bout.write( this.intermediateBuffer, 0, decompressed );
			}
		} catch ( DataFormatException e ) {
			this.logger.warn( "Received invalid batch packet - could not decompress", e );
			return new ArrayList<>( 0 );
		}
		
		List<Packet> packets = new ArrayList<>();
		PacketBuffer payload = new PacketBuffer( bout.toByteArray(), 0 );
		while ( payload.getRemaining() > 0 ) {
			int packetLength     = payload.readInt();
			int previousPosition = payload.getPosition();
			
			byte   packetID = this.extractRealPacketID( payload );
			Packet packet   = PacketRegistry.createFromID( packetID );
			if ( packet == null ) {
				this.logger.warn( "Received unknown packet batched into packet batch (ID: " + packetID + ")" );
				payload.skip( packetLength - ( payload.getPosition() - previousPosition ) );
				continue;
			}
			packet.deserialize( payload );
			packets.add( packet );
		}
		
		buffer.skip( payloadLength );
		
		return packets;
	}
	
	private class ProxySocketEventHandler implements SocketEventHandler {
		
		@Override
		public void onSocketEvent( Socket socket, SocketEvent socketEvent ) {
			switch ( socketEvent.getType() ) {
				case CONNECTION_ATTEMPT_FAILED:
					ProxiedConnection.this.proxiedConnection = null;
					ProxiedConnection.this.disconnect( "Failed to connect to backend server" );
					break;
				case CONNECTION_ATTEMPT_SUCCEEDED:
					ProxiedConnection.this.proxiedConnection = ProxiedConnection.this.proxySocket.getConnection();
					ProxiedConnection.this.notifyProxiedConnectionAvailable();
					break;
				
				case CONNECTION_CLOSED:
				case CONNECTION_DISCONNECTED:
					ProxiedConnection.this.disconnect( "Lost connection to backend server" );
					break;
			}
		}
		
	}
	
}