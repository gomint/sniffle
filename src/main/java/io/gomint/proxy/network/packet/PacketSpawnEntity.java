/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;

import io.gomint.proxy.entity.AttributeInstance;
import io.gomint.proxy.entity.metadata.MetadataContainer;
import io.gomint.proxy.network.PacketRegistry;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author BlackyPaw
 * @version 1.0
 */
@Data
@EqualsAndHashCode( callSuper = false )
public class PacketSpawnEntity extends Packet {

    private long entityId;
    private int entityType;
    private float x;
    private float y;
    private float z;
    private float velocityX;
    private float velocityY;
    private float velocityZ;
    private float pitch;
    private float yaw;
    private List<AttributeInstance> attributes;
    private MetadataContainer metadata;

    public PacketSpawnEntity() {
        super( PacketRegistry.PACKET_SPAWN_ENTITY );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeSignedVarLong( this.entityId );
        buffer.writeUnsignedVarLong( this.entityId );
        buffer.writeUnsignedVarInt( this.entityType );
        buffer.writeLFloat( this.x );
        buffer.writeLFloat( this.y );
        buffer.writeLFloat( this.z );
        buffer.writeLFloat( this.velocityX );
        buffer.writeLFloat( this.velocityY );
        buffer.writeLFloat( this.velocityZ );
        buffer.writeLFloat( this.pitch );
        buffer.writeLFloat( this.yaw );

        if ( this.attributes == null ) {
            buffer.writeUnsignedVarInt( 0 );
        } else {
            buffer.writeUnsignedVarInt( this.attributes.size() );
            for ( AttributeInstance entry : this.attributes ) {
                buffer.writeString( entry.getKey() );
                buffer.writeLFloat( entry.getMinValue() );
                buffer.writeLFloat( entry.getValue() );
                buffer.writeLFloat( entry.getMaxValue() );
            }
        }

        this.metadata.serialize( buffer );
        buffer.writeUnsignedVarInt( 0 );             // Entity links; TODO: implement this
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.entityId = buffer.readSignedVarLong().longValue();
        buffer.readUnsignedVarLong(); // Runtime ID
        this.entityType = buffer.readUnsignedVarInt();
        this.x = buffer.readLFloat();
        this.y = buffer.readLFloat();
        this.z = buffer.readLFloat();
        this.velocityX = buffer.readLFloat();
        this.velocityY = buffer.readLFloat();
        this.velocityZ = buffer.readLFloat();
        this.pitch = buffer.readLFloat();
        this.yaw = buffer.readLFloat();

        this.attributes = new ArrayList<>();

        int amountOfAttributes = buffer.readUnsignedVarInt();
        for ( int i = 0; i < amountOfAttributes; i++ ) {
            String key = buffer.readString();
            float min = buffer.readLFloat();
            float val = buffer.readLFloat();
            float max = buffer.readLFloat();

            this.attributes.add( new AttributeInstance( key, min, max, val, val ) );
        }

        this.metadata = new MetadataContainer();
        this.metadata.deserialize( buffer );
    }

}