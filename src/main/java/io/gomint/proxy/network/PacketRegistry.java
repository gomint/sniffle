package io.gomint.proxy.network;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class PacketRegistry {
	
	public static final byte PACKET_CLIENT_HANDSHAKE = 0x01;
	public static final byte PACKET_SERVER_HANDSHAKE = 0x03;
	public static final byte PACKET_ENCRYPTION_READY = 0x04;
	public static final byte PACKET_BATCH            = 0x06;
	
	/**
	 * Creates an instance of the Packet class corresponding to the given packet ID.
	 *
	 * @param packetID The ID of the packet
	 *
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
			default:
				return null;
		}
	}
	
}
