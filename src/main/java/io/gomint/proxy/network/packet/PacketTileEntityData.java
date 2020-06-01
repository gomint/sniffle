package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.math.BlockPosition;
import io.gomint.proxy.network.PacketRegistry;
import io.gomint.proxy.util.DumpUtil;
import io.gomint.taglib.AllocationLimitReachedException;
import io.gomint.taglib.NBTReader;
import io.gomint.taglib.NBTTagCompound;
import io.gomint.taglib.NBTWriter;
import lombok.Data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;

/**
 * @author geNAZt
 * @version 1.0
 */
@Data
public class PacketTileEntityData extends Packet {

    private BlockPosition position;
    private NBTTagCompound tileEntity;

    public PacketTileEntityData() {
        super( PacketRegistry.PACKET_TILE_ENTITY_DATA );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        // Block position
        writeBlockPosition( this.position, buffer );

        // NBT Tag
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NBTWriter nbtWriter = new NBTWriter( baos, ByteOrder.LITTLE_ENDIAN );
        nbtWriter.setUseVarint( true );
        try {
            nbtWriter.write( this.tileEntity );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        buffer.writeBytes( baos.toByteArray() );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.position = readBlockPosition( buffer );

        byte[] nbt = new byte[buffer.getRemaining()];
        buffer.readBytes( nbt );

        ByteArrayInputStream inputStream = new ByteArrayInputStream( nbt );
        NBTReader nbtReader = new NBTReader( inputStream, ByteOrder.LITTLE_ENDIAN );
        nbtReader.setUseVarint( true );

        try {
            this.tileEntity = nbtReader.parse();
        } catch ( IOException | AllocationLimitReachedException e ) {
            e.printStackTrace();
        }
    }

}
