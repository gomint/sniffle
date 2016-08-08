/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.packet;

/**
 * @author geNAZt
 * @version 1.0
 */
public class Protocol {

    /**
     * Packet ID of the login packet
     */
    public static final byte LOGIN_PACKET = (byte) 0x01;

    /**
     * Packet ID of the status packet
     */
    public static final byte PLAY_STATUS_PACKET = (byte) 0x02;

    /**
     * Packet ID of the disconnect packet
     */
    public static final byte DISONNECT_PACKET = (byte) 0x05;

    /**
     * Packet ID of the batch packet
     */
    public static final byte BATCH_PACKET = (byte) 0x06;

    /**
     * Packet ID for Text messages
     */
    public static final byte TEXT_PACKET = (byte) 0x07;

    /**
     * Packet ID for Player movement
     */
    public static final byte MOVE_PLAYER_PACKET = (byte) 0x10;

    /**
     * Packet ID for Player Add
     */
    public static final byte ADD_PLAYER_PACKET = (byte) 0x0a;

    /**
     * Packet ID for Entity Add
     */
    public static final byte ADD_ENTITY_PACKET = (byte) 0x0b;

    /**
     * Packet ID for Entity Remove
     */
    public static final byte REMOVE_ENTITY_PACKET = (byte) 0x0c;

    /**
     * Packet ID for Chunk data
     */
    public static final byte CHUNK_DATA_PACKET = (byte) 0x34;

    /**
     * Packet ID for Dimension change
     */
    public static final byte CHANGE_DIMENSION_PACKET = (byte) 0x36;

}
