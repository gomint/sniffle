/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.Gamerule;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author geNAZt
 * @version 1.0
 */
public abstract class Packet {

    /**
     * Internal MC:PE id of this packet
     */
	protected final byte id;

	/**
	 * Constructor for implemented Packets
	 *
	 * @param id The id which the Packet should use
     */
	protected Packet( byte id ) {
		this.id = id;
	}

	/**
	 * Gets the packet's ID.
	 *
	 * @return The packet's ID
	 */
	public byte getId() {
		return this.id;
	}

	/**
	 * Serializes this packet into the given buffer.
	 *
	 * @param buffer The buffer to serialize this packet into
	 */
	public abstract void serialize( PacketBuffer buffer );

	/**
	 * Deserializes this packet from the given buffer.
	 *
	 * @param buffer The buffer to deserialize this packet from
	 */
	public abstract void deserialize( PacketBuffer buffer );

	/**
	 * Returns an estimate length of the packet (used for pre-allocation).
	 *
	 * @return The estimate length of the packet or -1 if unknown
	 */
	public int estimateLength() {
		return -1;
	}

	/**
	 * Returns the ordering channel to send the packet on.
	 *
	 * @return The ordering channel of the packet
	 */
	public int orderingChannel() {
		return 0;
	}

	public void writeGamerules( Map<Gamerule, Object> gamerules, PacketBuffer buffer ) {
		if ( gamerules == null ) {
			buffer.writeUnsignedVarInt( 0 );
			return;
		}

		buffer.writeUnsignedVarInt( gamerules.size() );
		gamerules.forEach( new BiConsumer<Gamerule, Object>() {
			@Override
			public void accept( Gamerule gamerule, Object value ) {
				buffer.writeString( gamerule.getNbtName().toLowerCase() );

				if ( gamerule.getValueType() == Boolean.class ) {
					buffer.writeByte( (byte) 1 );
					buffer.writeBoolean( (Boolean) value );
				} else if ( gamerule.getValueType() == Integer.class ) {
					buffer.writeByte( (byte) 2 );
					buffer.writeUnsignedVarInt( (Integer) value );
				} else if ( gamerule.getValueType() == Float.class ) {
					buffer.writeByte( (byte) 3 );
					buffer.writeLFloat( (Float) value );
				}
			}
		} );
	}

	public Map<Gamerule, Object> readGamerules( PacketBuffer buffer ) {
		int amount = buffer.readUnsignedVarInt();
		if ( amount == 0 ) {
			return null;
		}

		Map<Gamerule, Object> gamerules = new HashMap<>();
		for ( int i = 0; i < amount; i++ ) {
			String name = buffer.readString();
			byte type = buffer.readByte();

			Object val = null;
			switch ( type ) {
				case 1:
					val = buffer.readBoolean();
					break;
				case 2:
					val = buffer.readUnsignedVarInt();
					break;
				case 3:
					val = buffer.readLFloat();
					break;
			}

			System.out.println( name + " -> " + String.valueOf( val ) );
		}

		return gamerules;
	}

}