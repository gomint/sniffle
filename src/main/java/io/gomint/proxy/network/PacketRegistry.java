package io.gomint.proxy.network;

import io.gomint.proxy.network.packet.*;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class PacketRegistry {

    public static final int PACKET_CLIENT_HANDSHAKE = 0x01;
    public static final int PACKET_SERVER_HANDSHAKE = 0x03;
    public static final int PACKET_ENCRYPTION_READY = 0x04;
    public static final int PACKET_START_GAME = 0x0b;
    public static final int PACKET_SPAWN_ENTITY =  0x0d;
    public static final int PACKET_UPDATE_ATTRIBUTES = 0x1D;
    public static final int PACKET_WORLD_EVENT = 0x19;
    public static final int PACKET_ENTITY_METADATA = 0x27;
    public static final int PACKET_ADVENTURE_SETTINGS = 0x37;
    public static final int PACKET_TILE_ENTITY_DATA = 0x38;
    public static final int PACKET_AVAILABLE_COMMANDS = 0x4c;
    public static final int PACKET_WORLD_CHUNK = 0x3A;
    public static final byte PACKET_BATCH = (byte) 0xfe;
    public static final int PACKET_INVENTORY_CONTENT_PACKET = 0x31;
    public static final int PACKET_CRAFTING_RECIPES = 0x34;
    public static final int PACKET_BIOME_DEFINITION_LIST = 0x7a;
    public static final int PACKET_CREATIVE_CONTENT = 0x91;

    /**
     * Creates an instance of the Packet class corresponding to the given packet ID.
     *
     * @param packetID The ID of the packet
     * @return A newly created instance of the Packet subclass resembling packets with the specified ID or null if no
     * matching class was found
     */
    public static Packet createFromID( int packetID ) {
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
            case PACKET_CREATIVE_CONTENT:
                return new PacketCreativeContent();
            case PACKET_BIOME_DEFINITION_LIST:
                return new PacketBiomeDefinitionList();
            default:
                return null;
        }
    }

}
