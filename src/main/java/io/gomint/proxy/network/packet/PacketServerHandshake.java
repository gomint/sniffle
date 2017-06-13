/*
 *  Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 *  This code is licensed under the BSD license found in the
 *  LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.network.PacketRegistry;
import lombok.Data;

/**
 * @author BlackyPaw
 * @version 1.0
 */
@Data
public class PacketServerHandshake extends Packet {
	
	private String publicKeyBase64;
	private byte[] sha256Salt;
	
	public PacketServerHandshake() {
		super( PacketRegistry.PACKET_SERVER_HANDSHAKE );
	}
	
	@Override
	public void serialize( PacketBuffer buffer ) {
		buffer.writeString( this.publicKeyBase64 );
		buffer.writeUnsignedVarInt( this.sha256Salt.length );
		buffer.writeBytes( this.sha256Salt );
	}
	
	@Override
	public void deserialize( PacketBuffer buffer ) {
		this.publicKeyBase64 = buffer.readString();
		this.sha256Salt = new byte[buffer.readUnsignedVarInt()];
		buffer.readBytes( this.sha256Salt );
	}
	
}
