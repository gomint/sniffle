package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.network.PacketRegistry;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author geNAZt
 * @version 1.0
 */
@Data
public class PacketAvailableCommands extends Packet {

    private static final Logger LOGGER = LoggerFactory.getLogger( PacketAvailableCommands.class );

    private List<String> enumValues;
    private List<String> postFixes;
    private Map<String, List<Integer>> enums;
    private List<CommandData> commandData;

    /**
     * Construct a new packet
     */
    public PacketAvailableCommands() {
        super( PacketRegistry.PACKET_AVAILABLE_COMMANDS );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        // First we need to write all enum values
        buffer.writeUnsignedVarInt( this.enumValues.size() );
        for ( String enumValue : this.enumValues ) {
            buffer.writeString( enumValue );
        }

        // After that we write all postfix data
        buffer.writeUnsignedVarInt( this.postFixes.size() );
        for ( String postFix : this.postFixes ) {
            buffer.writeString( postFix );
        }

        // Now we need to write enum index value data
        buffer.writeUnsignedVarInt( this.enums.size() );
        for ( Map.Entry<String, List<Integer>> entry : this.enums.entrySet() ) {
            buffer.writeString( entry.getKey() );
            buffer.writeUnsignedVarInt( entry.getValue().size() );
            for ( Integer enumValueIndex : entry.getValue() ) {
                writeEnumIndex( enumValueIndex, buffer );
            }
        }

        // Now write command data
        buffer.writeUnsignedVarInt( this.commandData.size() );
        for ( CommandData data : this.commandData ) {
            // Command meta
            buffer.writeString( data.getName() );
            buffer.writeString( data.getDescription() );

            // Flags?
            buffer.writeByte( data.getFlags() );
            buffer.writeByte( data.getPermission() );

            // Alias enum index
            buffer.writeLInt( -1 );     // TODO: Aliases are broken in 1.2, we fix this by taking each alias as seperate command

            // Write parameters and overload
            buffer.writeUnsignedVarInt( data.getParameters().size() );
            for ( List<CommandData.Parameter> parameters : data.getParameters() ) {
                buffer.writeUnsignedVarInt( parameters.size() );
                for ( CommandData.Parameter parameter : parameters ) {
                    buffer.writeString( parameter.getName() );
                    buffer.writeLInt( parameter.getType() );
                    buffer.writeBoolean( parameter.isOptional() );
                }
            }
        }
    }

    private void writeEnumIndex( int enumValueIndex, PacketBuffer buffer ) {
        if ( this.enumValues.size() < 256 ) {
            buffer.writeByte( (byte) enumValueIndex );
        } else if ( this.enumValues.size() < 65536 ) {
            buffer.writeLShort( (short) enumValueIndex );
        } else {
            buffer.writeLInt( enumValueIndex );
        }
    }

    private int readEnumIndex( PacketBuffer buffer ) {
        if ( this.enumValues.size() < 256 ) {
            return buffer.readByte() & 0xFF;
        } else if ( this.enumValues.size() < 65536 ) {
            return buffer.readLShort();
        } else {
            return buffer.readLInt();
        }
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.enumValues = new ArrayList<>();
        int values = buffer.readUnsignedVarInt();
        for ( int i = 0; i < values; i++ ) {
            this.enumValues.add( buffer.readString() );
        }

        this.postFixes = new ArrayList<>();
        values = buffer.readUnsignedVarInt();
        for ( int i = 0; i < values; i++ ) {
            this.postFixes.add( buffer.readString() );
        }

        this.enums = new HashMap<>();
        values = buffer.readUnsignedVarInt();
        for ( int i = 0; i < values; i++ ) {
            String enumName = buffer.readString();
            int innerValues = buffer.readUnsignedVarInt();
            List<Integer> enumIndexes = new ArrayList<>();
            for ( int i1 = 0; i1 < innerValues; i1++ ) {
                enumIndexes.add( readEnumIndex( buffer ) );
            }

            this.enums.put( enumName, enumIndexes );
        }

        this.commandData = new ArrayList<>();
        values = buffer.readUnsignedVarInt();
        for ( int i = 0; i < values; i++ ) {
            String name = buffer.readString();
            String descr = buffer.readString();

            CommandData commandData = new CommandData( name, descr );
            commandData.setParameters( new ArrayList<>() );

            byte flag = buffer.readByte();
            byte permission = buffer.readByte();

            int aliasEnumIndex = buffer.readLInt();

            int overloadValues = buffer.readUnsignedVarInt();
            for ( int i1 = 0; i1 < overloadValues; i1++ ) {
                List<CommandData.Parameter> overload = new ArrayList<>();

                int parameterValues = buffer.readUnsignedVarInt();
                for ( int i2 = 0; i2 < parameterValues; i2++ ) {
                    CommandData.Parameter parameter = new CommandData.Parameter( buffer.readString(), buffer.readLInt(), buffer.readBoolean() );
                    overload.add( parameter );
                }

                commandData.getParameters().add( overload );
            }

            LOGGER.info( "Found command: " + name + ": " + commandData );
            this.commandData.add( commandData );
        }
    }

}
