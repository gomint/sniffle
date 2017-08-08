package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;

/**
 * @author geNAZt
 * @version 1.0
 */
public class PacketPassthrough extends Packet {

    private byte[] data;

    public PacketPassthrough( byte id ) {
        super( id );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeBytes( this.data );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.data = new byte[buffer.getRemaining()];
        buffer.readBytes( this.data );
    }

}
