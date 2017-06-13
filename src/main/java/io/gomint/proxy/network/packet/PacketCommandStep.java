package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.Util;
import io.gomint.proxy.network.PacketRegistry;

/**
 * @author geNAZt
 * @version 1.0
 */
public class PacketCommandStep extends Packet {

    private String commandName;
    private String overloadName;

    private int unknown1;
    private int unknown2;
    private boolean hasOutput;

    private long clientGuid;

    private String input;
    private String output;

    private byte[] data;

    public PacketCommandStep() {
        super( PacketRegistry.PACKET_COMMAND_STEP );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeString( this.commandName );
        buffer.writeString( this.overloadName );

        buffer.writeUnsignedVarInt( this.unknown1 );
        buffer.writeUnsignedVarInt( this.unknown2 );

        buffer.writeBoolean( this.hasOutput );
        buffer.writeUnsignedVarLong( this.clientGuid );

        buffer.writeString( this.input );
        buffer.writeString( this.output );

        buffer.writeBytes( this.data );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.commandName = buffer.readString();
        this.overloadName = buffer.readString();

        this.unknown1 = buffer.readUnsignedVarInt();
        this.unknown2 = buffer.readUnsignedVarInt();

        this.hasOutput = buffer.readBoolean();

        System.out.println( this.unknown1 );
        System.out.println( this.unknown2 );
        System.out.println( this.hasOutput );

        this.clientGuid = buffer.readUnsignedVarLong();

        this.input = buffer.readString();
        this.output = buffer.readString();

        System.out.println( "GUID: " + this.clientGuid );
        System.out.println( "Commandname: " + this.commandName );
        System.out.println( "Overloadname: " + this.overloadName );
        System.out.println( "Input: " + this.input );
        System.out.println( "Output: " + this.output );

        // String next?

        this.data = new byte[buffer.getRemaining()];
        buffer.readBytes( this.data );

        System.out.println( Util.toHexString( this.data ) );
        System.out.println( new String( this.data ) );
    }

}
