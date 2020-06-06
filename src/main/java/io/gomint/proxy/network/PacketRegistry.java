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
    public static final byte PACKET_SPAWN_ENTITY = (byte) 0x0d;
    public static final byte PACKET_UPDATE_ATTRIBUTES = 0x1D;
    public static final byte PACKET_WORLD_EVENT = 0x19;
    public static final byte PACKET_ENTITY_METADATA = 0x27;
    public static final byte PACKET_ADVENTURE_SETTINGS = 0x37;
    public static final byte PACKET_TILE_ENTITY_DATA = (byte) 0x38;
    public static final byte PACKET_AVAILABLE_COMMANDS = (byte) 0x4c;
    public static final byte PACKET_WORLD_CHUNK = (byte) 0x3A;
    public static final byte PACKET_BATCH = (byte) 0xfe;
    public static final byte PACKET_INVENTORY_CONTENT_PACKET = (byte) 0x31;
    public static final byte PACKET_CRAFTING_RECIPES = (byte) 0x34;

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
                return new PacketLogin();
            case PACKET_START_GAME:
                return new PacketStartGame();
            case PACKET_ENCRYPTION_READY:
                return new PacketEncryptionReady();
            case PACKET_SERVER_HANDSHAKE:
                return new PacketServerHandshake();
            case PACKET_CRAFTING_RECIPES:
                return new PacketCraftingRecipes();
            case PACKET_INVENTORY_CONTENT_PACKET:
                return new PacketInventoryContent();
            /*case PACKET_ENTITY_METADATA:
                return new PacketEntityMetadata();*/
            default:
                return null;
        }
    }

}
