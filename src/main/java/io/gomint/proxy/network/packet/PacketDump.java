package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.Util;

/**
 * @author geNAZt
 * @version 1.0
 */
public class PacketDump extends Packet {

    private byte[] data;

    public PacketDump( byte id ) {
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

        System.out.println( Util.toHexString( this.data ) );
        System.out.println( new String( this.data ) );
    }

}
