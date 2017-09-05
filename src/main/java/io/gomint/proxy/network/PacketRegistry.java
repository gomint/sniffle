package io.gomint.proxy.network;

import io.gomint.proxy.network.packet.*;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class PacketRegistry {

    public static final byte PACKET_CLIENT_HANDSHAKE = 0x01;
    public static final byte PACKET_SERVER_HANDSHAKE = 0x03;
    public static final byte PACKET_ENCRYPTION_READY = 0x04;
    public static final byte PACKET_START_GAME = (byte) 0x0b;
    public static final byte PACKET_MOVE_ENTITY = 0x12;
    public static final byte PACKET_ENTITY_METADATA = 0x27;
    public static final byte PACKET_CHUNK_DATA = 0x3a;
    public static final byte PACKET_ADVENTURE_SETTINGS = 0x37;
    public static final byte PACKET_AVAILABLE_COMMANDS = 0x4e;
    public static final byte PACKET_COMMAND_STEP = 0x4f;
    public static final byte PACKET_BATCH = (byte) 0xfe;

    /**
     * Creates an instance of the Packet class corresponding to the given packet ID.
     *
     * @param packetID The ID of the packet
     * @return A newly created instance of the Packet subclass resembling packets with the specified ID or null if no
     * matching class was found
     */
    public static Packet createFromID( byte packetID ) {
        switch ( packetID ) {
            case PACKET_CLIENT_HANDSHAKE:
                return new PacketClientHandshake();
            case PACKET_SERVER_HANDSHAKE:
                return new PacketServerHandshake();
            case PACKET_ENCRYPTION_READY:
                return new PacketEncryptionReady();
            case PACKET_START_GAME:
                return new PacketStartGame();
            case PACKET_ENTITY_METADATA:
                return new PacketEntityMetadata();
            case PACKET_MOVE_ENTITY:
                return new PacketPassthrough( PACKET_MOVE_ENTITY );
            case PACKET_CHUNK_DATA:
                return new PacketPassthrough( PACKET_CHUNK_DATA );
            case PACKET_ADVENTURE_SETTINGS:
                return new PacketAdventureSettings();
            case PACKET_AVAILABLE_COMMANDS:
                return new PacketAvailableCommands();
            case PACKET_COMMAND_STEP:
                return new PacketCommandStep();
            default:
                return null;
        }
    }

}
