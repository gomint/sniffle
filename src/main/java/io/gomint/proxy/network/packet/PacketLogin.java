package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.network.PacketRegistry;
import lombok.Data;

/**
 * @author BlackyPaw
 * @version 1.0
 */
@Data
public class PacketLogin extends Packet {

	private int protocol;
	private byte[] payload;
	
	public PacketLogin() {
		super( PacketRegistry.PACKET_CLIENT_HANDSHAKE );
	}
	
	@Override
	public void serialize( PacketBuffer buffer ) {
		buffer.writeInt( this.protocol );

		buffer.writeUnsignedVarInt( this.payload.length );
		buffer.writeBytes( this.payload );
	}
	
	@Override
	public void deserialize( PacketBuffer buffer ) {
		this.protocol = buffer.readInt();

		this.payload = new byte[buffer.readUnsignedVarInt()];
		buffer.readBytes( this.payload );
	}
	
}
