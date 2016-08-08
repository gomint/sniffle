/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.packet;

import io.gomint.jraknet.PacketBuffer;

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

}