/*
 *  Copyright (c) 2015, GoMint, BlackyPaw and geNAZt
 *
 *  This code is licensed under the BSD license found in the
 *  LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.network;

import io.gomint.jraknet.PacketBuffer;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class PacketEncryptionReady extends Packet {
	
	public PacketEncryptionReady() {
		super( PacketRegistry.PACKET_ENCRYPTION_READY );
	}
	
	@Override
	public void serialize( PacketBuffer buffer ) {
		
	}
	
	@Override
	public void deserialize( PacketBuffer buffer ) {
		
	}
	
}
