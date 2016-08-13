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
public class PacketServerHandshake extends Packet {
	
	private String publicKeyBase64;
	private byte[] sha256Salt;
	
	public PacketServerHandshake() {
		super( PacketRegistry.PACKET_SERVER_HANDSHAKE );
	}
	
	public String getPublicKeyBase64() {
		return this.publicKeyBase64;
	}
	
	public void setPublicKeyBase64( String publicKeyBase64 ) {
		this.publicKeyBase64 = publicKeyBase64;
	}
	
	public byte[] getSha256Salt() {
		return this.sha256Salt;
	}
	
	public void setSha256Salt( byte[] sha256Salt ) {
		this.sha256Salt = sha256Salt;
	}
	
	@Override
	public void serialize( PacketBuffer buffer ) {
		buffer.writeString( this.publicKeyBase64 );
		buffer.writeUShort( this.sha256Salt.length );
		buffer.writeBytes( this.sha256Salt );
	}
	
	@Override
	public void deserialize( PacketBuffer buffer ) {
		this.publicKeyBase64 = buffer.readString();
		this.sha256Salt = new byte[buffer.readUShort()];
		buffer.readBytes( this.sha256Salt );
	}
	
}
