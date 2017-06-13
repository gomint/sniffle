package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.network.PacketRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author geNAZt
 * @version 1.0
 */
public class PacketAvailableCommands extends Packet {

    private static final Logger LOGGER = LoggerFactory.getLogger( PacketAvailableCommands.class );

    private String commands;
    private String unknown;

    public PacketAvailableCommands() {
        super( PacketRegistry.PACKET_AVAILABLE_COMMANDS );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeString( this.commands );
        buffer.writeString( this.unknown );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.commands = buffer.readString();
        this.unknown = buffer.readString();

        /*LOGGER.debug( "Commands: " );
        LOGGER.debug( this.commands );
        LOGGER.debug( "Unknown: " );
        LOGGER.debug( this.unknown );*/
    }

}
