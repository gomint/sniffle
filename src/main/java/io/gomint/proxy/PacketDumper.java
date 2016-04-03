/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy;

import io.gomint.jraknet.PacketBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Schuwi
 * @version 1.0
 */
public class PacketDumper implements ProxiedPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger( PacketDumper.class );

    private final File dumpFolder;
    private final AtomicInteger currentPacket;

    public PacketDumper( File dumpFolder ) {
        this.dumpFolder = dumpFolder;
        this.currentPacket = new AtomicInteger();
    }

    @Override
    public byte[] handlePacket(byte[] packetData, boolean fromServer, boolean batch) {
        PacketBuffer buffer = new PacketBuffer( packetData, 0 );
        byte packetId = buffer.readByte();
        int count = currentPacket.getAndIncrement();

        String fileName = String.format( "%04d_" + ( fromServer ? "S" : "C" ) + "_%02X.dump", count, packetId );
        File dump = new File( dumpFolder, fileName );

        // Dump buffer contents
        try ( OutputStream out = new FileOutputStream( dump, true ) ) {
            try ( BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( out ) ) ) {
                writer.write( String.format("# Packet dump of 0x%02x%s\n", packetId, batch ? " in batch" : "" ) );
                writer.write( "-------------------------------------\n" );
                writer.write( "# Textual payload\n" );
                StringBuilder lineBuilder = new StringBuilder();
                while ( buffer.getRemaining() > 0 ) {
                    for ( int i = 0; i < 16 && buffer.getRemaining() > 0; ++i ) {
                        lineBuilder.append( String.format("%02x", buffer.readByte()) );
                        if ( i + 1 < 16 && buffer.getRemaining() > 0 ) {
                            lineBuilder.append( " " );
                        }
                    }
                    lineBuilder.append( "\n" );

                    writer.write( lineBuilder.toString() );
                    lineBuilder.setLength(0);
                }
                writer.write( "-------------------------------------\n" );
                writer.write( "# Binary payload\n" );
                writer.flush();

                buffer.resetPosition();
                buffer.skip( 1 ); // Packet ID
                out.write( buffer.getBuffer(), buffer.getPosition(), buffer.getRemaining() );
            }
        } catch ( IOException e ) {
            LOGGER.error( "Failed to dump packet " + fileName, e );
        }

        return packetData;
    }

}
