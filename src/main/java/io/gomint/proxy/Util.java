package io.gomint.proxy;

import io.gomint.jraknet.PacketBuffer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

/**
 * @author geNAZt
 * @version 1.0
 */
public class Util {

    public static String toHexString( byte[] data ) {
        StringBuilder stringBuilder = new StringBuilder();

        for ( byte b : data ) {
            stringBuilder.append( "0x" ).append( Integer.toHexString( b & 0xFF ) ).append( " " );
        }

        return stringBuilder.toString();
    }

    public static List<PacketBuffer> handleBatchPacket( PacketBuffer buffer, boolean batch ) {
        if ( batch ) {
            return new ArrayList<>();
        }

        int compressedSize = buffer.readInt();              // Compressed payload length (not of interest; only uncompressed size matters)

        InflaterInputStream inflaterInputStream = new InflaterInputStream( new ByteArrayInputStream( buffer.getBuffer(), buffer.getPosition(), compressedSize ) );

        ByteArrayOutputStream bout = new ByteArrayOutputStream( compressedSize );
        byte[] batchIntermediate = new byte[256];

        try {
            int read;
            while ( ( read = inflaterInputStream.read( batchIntermediate ) ) > -1 ) {
                bout.write( batchIntermediate, 0, read );
            }
        } catch ( IOException e ) {
            // Check if we have a debugger attached
            e.printStackTrace();
        }

        List<PacketBuffer> buffers = new ArrayList<>();
        PacketBuffer payloadBuffer = new PacketBuffer( bout.toByteArray(), 0 );
        while ( payloadBuffer.getRemaining() > 0 ) {
            int packetLength = payloadBuffer.readInt();

            byte[] pktData = new byte[packetLength];
            payloadBuffer.readBytes( pktData );

            buffers.add( new PacketBuffer( pktData, 0 ) );
        }

        return buffers;
    }

}
