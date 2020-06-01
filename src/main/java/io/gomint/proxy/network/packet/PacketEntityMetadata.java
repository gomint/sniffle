/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.entity.metadata.MetadataContainer;
import io.gomint.proxy.network.PacketRegistry;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author BlackyPaw
 * @version 1.0
 */
@Data
@EqualsAndHashCode( callSuper = false )
public class PacketEntityMetadata extends Packet {

    private long entityId;
    private MetadataContainer metadata;

    public PacketEntityMetadata() {
        super( PacketRegistry.PACKET_ENTITY_METADATA );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeUnsignedVarLong( this.entityId );
        this.metadata.serialize( buffer );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.entityId = buffer.readUnsignedVarLong();
        this.metadata = new MetadataContainer();
        this.metadata.deserialize( buffer );
    }

}