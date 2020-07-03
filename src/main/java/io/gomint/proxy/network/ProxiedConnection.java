package io.gomint.proxy.network;

import io.gomint.crypto.Processor;
import io.gomint.jraknet.*;
import io.gomint.proxy.Util;
import io.gomint.proxy.jwt.*;
import io.gomint.proxy.network.packet.Packet;
import io.gomint.proxy.network.packet.PacketEncryptionReady;
import io.gomint.proxy.network.packet.PacketLogin;
import io.gomint.proxy.network.packet.PacketServerHandshake;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Key;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class ProxiedConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger( ProxiedConnection.class );

    // Client:
    private final Connection clientConnection;
    private Queue<Packet> clientPacketQueue;

    // Server:
    private ClientSocket proxySocket;
    private Connection proxiedConnection;
    private Queue<Packet> serverPacketQueue;

    // Miscellaneous:
    private final ConnectionManager connectionManager;
    private JSONObject skinData;
    private EncryptionHandler encryptionHandler;

    // Native stuff
    private Processor inClient = new Processor(false);
    private Processor outClient = new Processor(true);
    private Processor inServer = new Processor(false);
    private Processor outServer = new Processor(true);

    //
    private boolean enableEncryption;

    /**
     * Constructs a new ProxiedConnection wrapping the given client connection.
     *
     * @param connectionManager The connection manager that created this connection
     * @param clientConnection  The client connection to wrap
     */
    public ProxiedConnection( ConnectionManager connectionManager, Connection clientConnection ) {
        this.clientConnection = clientConnection;
        this.connectionManager = connectionManager;

        this.encryptionHandler = new EncryptionHandler();

        this.clientPacketQueue = new LinkedList<>();
        this.serverPacketQueue = new LinkedList<>();
    }

    public void sendToClient( Packet packet ) {
        synchronized ( this.clientPacketQueue ) {
            this.clientPacketQueue.add( packet );
        }
    }

    public void sendToServer( Packet packet ) {
        synchronized ( this.serverPacketQueue ) {
            this.serverPacketQueue.add( packet );
        }
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
            if (this.enableEncryption) {
                this.inClient.enableCrypto(this.encryptionHandler.getClientKey(), this.encryptionHandler.getClientIV());
                this.enableEncryption = false;
            }

            this.handleServerboundPacketRaw( new PacketBuffer( packet.getPacketData() ) );
        }

        if ( this.proxiedConnection != null && this.proxiedConnection.isConnected() ) {
            while ( ( packet = this.proxiedConnection.receive() ) != null ) {
                this.handleClientboundPacketRaw( new PacketBuffer( packet.getPacketData() ) );
            }
        }

        // Send out all enqueued packets:
        synchronized ( this.clientPacketQueue ) {
            if ( this.clientPacketQueue.size() > 0 ) {
                this.clientConnection.send( PacketReliability.RELIABLE_ORDERED, 0,
                    this.batchPackets( this.clientPacketQueue, "Client" ) );
            }
        }

        if ( this.proxiedConnection != null && this.proxiedConnection.isConnected() ) {
            synchronized ( this.serverPacketQueue ) {
                if ( this.serverPacketQueue.size() > 0 ) {
                    this.proxiedConnection.send( PacketReliability.RELIABLE_ORDERED, 0,
                        this.batchPackets( this.serverPacketQueue, "Server" ) );
                }
            }
        } else {
            synchronized ( this.serverPacketQueue ) {
                // Prevent unnecessary memory consumption:
                this.serverPacketQueue.clear();
            }
        }
    }

    /**
     * Handles some raw network data received from the proxied connection, i.e. data sent from the
     * proxied server to the client.
     *
     * @param raw The raw data received
     */
    public void handleClientboundPacketRaw( PacketBuffer raw ) {
        // Skip 0xFE datagram header:
        if ( raw.getRemaining() <= 1 ) {
            // We need at least one byte of packet data:
            return;
        }

        while ( raw.getRemaining() > 0 ) {
            byte packetID = raw.readByte();
            if ( packetID == PacketRegistry.PACKET_BATCH ) {
                for ( Packet packet : this.extractBatchPacket( raw, false ) ) {
                    this.handleClientboundPacket( packet );
                }
            } else {
                Packet packet = PacketRegistry.createFromID( packetID );
                if ( packet != null ) {
                    packet.deserialize( raw );
                    this.handleClientboundPacket( packet );
                } else {
                    // Unknown packet -> direct passthrough:
                    System.out.println( "Unknown packet with ID " + packetID );

                    byte[] data = new byte[raw.getRemaining()];
                    raw.readBytes( data );

                    Packet packet1 = new Packet( packetID ) {
                        @Override
                        public void serialize( PacketBuffer buffer ) {
                            buffer.writeBytes( data );
                        }

                        @Override
                        public void deserialize( PacketBuffer buffer ) {

                        }
                    };

                    this.clientPacketQueue.add( packet1 );
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
    public void handleServerboundPacketRaw( PacketBuffer raw ) {
        // Skip 0xFE datagram header:
        if ( raw.getRemaining() <= 1 ) {
            // We need at least one byte of packet data:
            return;
        }

        while ( raw.getRemaining() > 0 ) {
            byte packetID = raw.readByte();
            if ( packetID == PacketRegistry.PACKET_BATCH ) {
                for ( Packet packet : this.extractBatchPacket( raw, true ) ) {
                    this.handleServerboundPacket( packet );
                }
            } else {
                Packet packet = PacketRegistry.createFromID( packetID );
                if ( packet != null ) {
                    packet.deserialize( raw );
                    this.handleServerboundPacket( packet );
                } else {
                    // Unknown packet -> direct passthrough:
                    LOGGER.info( "Unknown packet with ID " + packetID );

                    byte[] data = new byte[raw.getRemaining()];
                    raw.readBytes( data );

                    System.out.println( Util.toHexString( data ) );

                    Packet packet1 = new Packet( packetID ) {
                        @Override
                        public void serialize( PacketBuffer buffer ) {
                            buffer.writeBytes( data );
                        }

                        @Override
                        public void deserialize( PacketBuffer buffer ) {

                        }
                    };

                    this.serverPacketQueue.add( packet1 );
                    // TODO: Implement direct passthrough here
                    // buffer.skip( buffer.getRemaining() );
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
        this.proxySocket.setMojangModificationEnabled( true );

        try {
            this.proxySocket.initialize();
        } catch ( SocketException e ) {
            LOGGER.error( "Failed to establish connection to backend server: " + address.toString(), e );

            // Disconnect client -> will cause connection manager to remove this proxied connection from its maps:
            this.clientConnection.disconnect( "Failed to connect to backend server" );
            return;
        }

        this.proxySocket.setEventHandler( new ProxySocketEventHandler() );
        this.proxySocket.ping( address );
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
        LOGGER.warn( "Disconnecting client: " + reason );

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
        //this.cachedInflater.end();
        //this.cachedDeflater.end();
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
            default:
                // Pass packets to client
                this.clientPacketQueue.add( packet );
                break;
        }
    }

    /**
     * Handles a decoded packet that was sent from the client to the proxied server.
     *
     * @param packet The decoded packet
     */
    private void handleServerboundPacket( Packet packet ) {
        switch ( packet.getId() ) {
            case PacketRegistry.PACKET_CLIENT_HANDSHAKE:
                this.handleClientHandshake( (PacketLogin) packet );
                break;
            case PacketRegistry.PACKET_ENCRYPTION_READY:
                this.outClient.enableCrypto(this.encryptionHandler.getClientKey(), this.encryptionHandler.getClientIV());
                this.encryptionHandler.setEncryptionToClientEnabled( true );
                break;
            default:
                this.serverPacketQueue.add( packet );
                break;
        }
    }

    /**
     * Handles a client's handshake packet by setting up the client's side of encryption, if necessary.
     *
     * @param packet The handshake packet that was received
     */
    private void handleClientHandshake( PacketLogin packet ) {
        LOGGER.info( "Client protocol version: " + packet.getProtocol() );

        // More data please
        ByteBuffer byteBuffer = ByteBuffer.wrap( packet.getPayload() );
        byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
        byte[] stringBuffer = new byte[byteBuffer.getInt()];
        byteBuffer.get( stringBuffer );

        this.encryptionHandler.supplyClientJWTChain( new String( stringBuffer ) );
        if ( this.encryptionHandler.hasObligatoryInformation() ) {
            this.encryptionHandler.beginClientsideEncryption();

            // Parse skin
            byte[] skin = new byte[byteBuffer.getInt()];
            byteBuffer.get( skin );

            JwtToken skinToken = JwtToken.parse( new String( skin ) );

            try {
                skinToken.validateSignature( JwtAlgorithm.ES384, this.encryptionHandler.getTrustedKeys().get( skinToken.getHeader().getProperty( "x5u" ) ) );
                this.skinData = skinToken.getClaims();
            } catch ( JwtSignatureException e ) {
                e.printStackTrace();
            }

            EncryptionRequestForger forger = new EncryptionRequestForger();
            String jwt = forger.forge( this.encryptionHandler.getProxyPublic(), this.encryptionHandler.getProxyPrivate(), this.encryptionHandler.getClientSalt() );

            JwtToken token = JwtToken.parse( jwt );
            String keyDataBase64 = (String) token.getHeader().getProperty( "x5u" );
            Key key = MojangChainValidator.createPublicKey( keyDataBase64 );

            try {
                if ( token.validateSignature( key ) ) {
                    LOGGER.debug( "For client: Valid encryption start JWT" );
                }
            } catch ( JwtSignatureException e ) {
                e.printStackTrace();
            }

            PacketServerHandshake handshake = new PacketServerHandshake();
            handshake.setJwtData( jwt );

            LOGGER.debug( "Sending proxy encryption handshake JWT: {}", jwt );
            this.sendToClient( handshake );
            this.enableEncryption = true;
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
        // We need to verify the JWT request
        JwtToken token = JwtToken.parse( packet.getJwtData() );
        String keyDataBase64 = (String) token.getHeader().getProperty( "x5u" );
        Key key = MojangChainValidator.createPublicKey( keyDataBase64 );

        try {
            if ( token.validateSignature( key ) ) {
                LOGGER.debug( "For server: Valid encryption start JWT" );
            }
        } catch ( JwtSignatureException e ) {
            e.printStackTrace();
        }

        LOGGER.debug( "Encryption JWT public: " + keyDataBase64 );
        this.encryptionHandler.setServerPublicKey( keyDataBase64 );
        this.encryptionHandler.beginServersideEncryption( Base64.getDecoder().decode( (String) token.getClaim( "salt" ) ) );

        this.outServer.enableCrypto(this.encryptionHandler.getServerKey(), this.encryptionHandler.getServerIV());
        this.inServer.enableCrypto(this.encryptionHandler.getServerKey(), this.encryptionHandler.getServerIV());

        // Tell the server that we are ready to receive encrypted packets from now on:
        PacketEncryptionReady response = new PacketEncryptionReady();
        this.sendToServer( response );
    }

    /**
     * Invoked whenever our proxied server connection got ready and we should now send a handshake and such.
     */
    @SuppressWarnings( "unchecked" )
    private void notifyProxiedConnectionAvailable() {
        // Send our handshake to the server -> this will trigger it to respond with a 0x03 ServerHandshake packet:
        MojangLoginForger mojangLoginForger = new MojangLoginForger();
        mojangLoginForger.setPublicKey( EncryptionHandler.PROXY_KEY_PAIR.getPublic() );
        mojangLoginForger.setUsername( this.encryptionHandler.getClientUsername() );
        mojangLoginForger.setUUID( this.encryptionHandler.getClientUUID() );
        mojangLoginForger.setSkinData( this.skinData );

        String jwt = "{\"chain\":[\"" + mojangLoginForger.forge( EncryptionHandler.PROXY_KEY_PAIR.getPrivate() ) + "\"]}";
        String skin = mojangLoginForger.forgeSkin( EncryptionHandler.PROXY_KEY_PAIR.getPrivate() );

        // More data please
        ByteBuffer byteBuffer = ByteBuffer.allocate( jwt.length() + skin.length() + 8 );
        byteBuffer.order( ByteOrder.LITTLE_ENDIAN );
        byteBuffer.putInt( jwt.length() );
        byteBuffer.put( jwt.getBytes() );

        // We need the skin
        byteBuffer.putInt( skin.length() );
        byteBuffer.put( skin.getBytes() );

        PacketLogin packetLogin = new PacketLogin();
        packetLogin.setProtocol( 407 );
        packetLogin.setPayload( byteBuffer.array() );
        this.sendToServer(packetLogin);
    }

    /**
     * Interprets the next one to two bytes as a packet ID and returns the actual packet ID without the
     * optional 0xFE prefix.
     *
     * @param buffer The packet buffer to read data from
     * @return The actual packet ID extracted from the given buffer
     */
    private int extractRealPacketID( PacketBuffer buffer ) {
        int id = buffer.readUnsignedVarInt();
        return id & 0x3FF;
    }

    private ByteBuf writePackets(Queue<Packet> packets) {
        // Write all packets into the inBuf for compression
        PacketBuffer buffer = new PacketBuffer(16);
        ByteBuf inBuf = newNettyBuffer();

        while ( !packets.isEmpty() ) {
            Packet packet = packets.poll();

            buffer.setReadPosition(0);
            buffer.setWritePosition(0);

            // CHECKSTYLE:OFF
            try {
                LOGGER.debug("Writing (batch) packet: {}", packet.getClass().getName());

                packet.serializeHeader(buffer);
                packet.serialize(buffer);

                writeVarInt(buffer.getWritePosition(), inBuf);
                inBuf.writeBytes(buffer.getBuffer());
            } catch (Exception e) {
                LOGGER.error("Could not serialize packet", e);
            }
            // CHECKSTYLE:ON
        }

        return inBuf;
    }

    private ByteBuf newNettyBuffer() {
        return PooledByteBufAllocator.DEFAULT.directBuffer();
    }

    private void writeVarInt(int value, ByteBuf stream) {
        while ((value & -128) != 0) {
            stream.writeByte(value & 127 | 128);
            value >>>= 7;
        }

        stream.writeByte(value);
    }

    private PacketBuffer batchPackets( Queue<Packet> queue, String direction ) {
        // Assemble uncompressed contents:
        ByteBuf raw = writePackets(queue);

        ByteBuf out;
        if (direction.equals("Server")) {
            out = this.outServer.process(raw);
        } else {
            out = this.outClient.process(raw);
        }

        // Now serialize the batch packet:
        PacketBuffer buffer = new PacketBuffer( 1 + out.readableBytes() );
        buffer.writeByte( (byte) 0xFE );
        buffer.writeBytes( out );
        return buffer;
    }

    /**
     * Extracts all packets contained in the batch packet encoded in the specified bufer.
     *
     * @param buffer The buffer to read the batch packet from
     * @return All packets found inside the given batch packet
     */
    private Collection<Packet> extractBatchPacket( PacketBuffer buffer, boolean fromClient ) {
        ByteBuf in;
        if (fromClient) {
            in = this.inClient.process(buffer.getBuffer());
        } else {
            in = this.inServer.process(buffer.getBuffer());
        }

        List<Packet> packets = new ArrayList<>();
        PacketBuffer payload = new PacketBuffer( in );

        while ( payload.getRemaining() > 0 ) {
            int packetLength = payload.readUnsignedVarInt();

            byte[] pyLoad = new byte[packetLength];
            payload.readBytes( pyLoad );

            PacketBuffer buffer1 = new PacketBuffer( packetLength );
            buffer1.writeBytes(pyLoad);

            int packetID = this.extractRealPacketID( buffer1 );
            Packet packet = PacketRegistry.createFromID( packetID );
            if ( packet == null ) {
                byte[] packetPayload = new byte[buffer1.getRemaining()];
                buffer1.readBytes( packetPayload );

                // Add dummy packet
                packets.add( new Packet( packetID ) {
                    @Override
                    public void serialize( PacketBuffer buffer ) {
                        buffer.writeBytes( packetPayload );
                    }

                    @Override
                    public void deserialize( PacketBuffer buffer ) {

                    }
                } );

                continue;
            }

            LOGGER.debug("Unpacked packet: " + packet.getClass().getName());
            packet.deserialize( buffer1 );
            packets.add( packet );
        }

        buffer.skip( buffer.getRemaining() );

        return packets;
    }

    private class ProxySocketEventHandler implements SocketEventHandler {

        @Override
        public void onSocketEvent( Socket socket, SocketEvent socketEvent ) {
            switch ( socketEvent.getType() ) {
                case UNCONNECTED_PONG:
                    String[] split = socketEvent.getPingPongInfo().getMotd().split(";");

                    int port = Integer.parseInt(split[split.length - 2]);

                    ProxiedConnection.this.proxySocket.connect(new InetSocketAddress( ((InetSocketAddress) socketEvent.getPingPongInfo().getAddress()).getHostName(), port));
                    break;
                case CONNECTION_ATTEMPT_FAILED:
                    ProxiedConnection.this.proxiedConnection = null;
                    ProxiedConnection.this.disconnect( "Failed to connect to backend server" );
                    break;
                case CONNECTION_ATTEMPT_SUCCEEDED:
                    ProxiedConnection.this.proxiedConnection = ProxiedConnection.this.proxySocket.getConnection();
                    ProxiedConnection.this.notifyProxiedConnectionAvailable();
                    LOGGER.info( "Connected to backend server" );
                    break;

                case CONNECTION_CLOSED:
                case CONNECTION_DISCONNECTED:
                    ProxiedConnection.this.disconnect( "Lost connection to backend server" );
                    break;
            }
        }

    }

}