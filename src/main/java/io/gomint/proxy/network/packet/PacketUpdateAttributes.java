package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.entity.AttributeInstance;
import io.gomint.proxy.network.PacketRegistry;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author geNAZt
 * @version 1.0
 */
@Data
public class PacketUpdateAttributes extends Packet {

    private long entityId;
    private List<AttributeInstance> entries;

    public PacketUpdateAttributes() {
        super( PacketRegistry.PACKET_UPDATE_ATTRIBUTES );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeUnsignedVarLong( this.entityId );

        if ( this.entries == null ) {
            buffer.writeUnsignedVarInt( 0 );
        } else {
            buffer.writeUnsignedVarInt( this.entries.size() );
            for ( AttributeInstance entry : this.entries ) {
                buffer.writeLFloat( entry.getMinValue() );
                buffer.writeLFloat( entry.getMaxValue() );
                buffer.writeLFloat( entry.getValue() );
                buffer.writeLFloat( entry.getDefaultValue() );
                buffer.writeString( entry.getKey() );
            }
        }
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.entityId = buffer.readUnsignedVarLong();
        int amountOfAttributes = buffer.readUnsignedVarInt();
        for ( int i = 0; i < amountOfAttributes; i++ ) {
            float minValue = buffer.readLFloat();
            float maxValue = buffer.readLFloat();
            float value = buffer.readLFloat();
            float defaultValue = buffer.readLFloat();
            String key = buffer.readString();
            addAttributeInstance( new AttributeInstance( key, minValue, maxValue, value, defaultValue) );
        }

        System.out.println( this );
    }

    public void addAttributeInstance( AttributeInstance instance ) {
        if ( this.entries == null ) {
            this.entries = new ArrayList<>();
        }

        this.entries.add( instance );
    }

}
