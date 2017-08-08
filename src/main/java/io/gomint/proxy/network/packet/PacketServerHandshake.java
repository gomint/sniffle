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
	
	private String jwtData;
	
	public PacketServerHandshake() {
		super( PacketRegistry.PACKET_SERVER_HANDSHAKE );
	}
	
	@Override
	public void serialize( PacketBuffer buffer ) {
		buffer.writeString( this.jwtData );
	}
	
	@Override
	public void deserialize( PacketBuffer buffer ) {
		this.jwtData = buffer.readString();
	}
	
}
