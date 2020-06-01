package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.Util;

/**
 * @author geNAZt
 * @version 1.0
 */
public class PacketManualWTF extends Packet {

    /**
     * Constructor for implemented Packets
     *
     * @param id The id which the Packet should use
     */
    public PacketManualWTF( byte id ) {
        super( id );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeBytes( new byte[]{ 0x0, 0x0, 0x3 } );   // Server type, Player is 0

        // This should be success flag
        buffer.writeBoolean( true );

        // This should be amount of output message formats
        buffer.writeUnsignedVarInt( 2 );

        // Now we write success false type
        buffer.writeBoolean( false );

        // Write output format
        buffer.writeString( "commands.generic.noTargetMatch" );

        // No parameters
        buffer.writeUnsignedVarInt( 0 );

        // Second output format
        buffer.writeBoolean( false );

        // Success output format
        buffer.writeString( "Test output: %%1$s -> %%2$d" );

        // We have two parameters
        buffer.writeUnsignedVarInt( 2 );

        // First is the name
        buffer.writeString( "geNAZt" );

        // Second is the message
        buffer.writeString( "Test" );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        // I don't care what the server tells me here
        byte[] data = new byte[buffer.getRemaining()];
        buffer.readBytes( data );

        System.out.println( Util.toHexString( data ) );
        System.out.println( new String( data ) );
    }

}
