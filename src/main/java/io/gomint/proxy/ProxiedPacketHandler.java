/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy;

/**
 * @author Schuwi
 * @version 1.0
 */
public interface ProxiedPacketHandler {

    /**
     * Is called for every packet that is redirected between client and server. Any modifications to the packetData
     * will be applied to the redirected packet.
     *
     * @param packetData    Byte array containing the packet data, may be modified by previous {@link ProxiedPacketHandler}. Will never be null
     * @param fromServer    Is true if the packet was sent by the server to the client, false otherwise
     * @param batch         When set the packetData is just the decompressed data from the batch packet (always false if raw packet handler)
     * @return              The packetData after being processed, may be modified. Packet will not be redirected if returns null
     */
    byte[] handlePacket( byte[] packetData, boolean fromServer, boolean batch );

}
