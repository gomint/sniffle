package io.gomint.proxy;

import io.gomint.jraknet.Connection;
import io.gomint.jraknet.PacketBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * @author Schuwi
 * @version 1.0
 */
public class PacketRedirectThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger( PacketRedirectThread.class );

    private final Connection inbound;
    private final Connection outbound;
    private final boolean fromServer; // Whether this thread handles packets coming from the server

    private final List<ProxiedPacketHandler> rawPacketHandlers = new ArrayList<>();
    private final List<ProxiedPacketHandler> processedPacketHandlers = new ArrayList<>();
    private final AtomicBoolean running;

    private final byte[]   batchIntermediate;
    private final Inflater batchDecompressor;

    public PacketRedirectThread( Connection inbound, Connection outbound, boolean fromServer ) {
        super( ( fromServer ? "Server" : "Client" ) + "PacketRedirectThread" );

        this.inbound = inbound;
        this.outbound = outbound;
        this.fromServer = fromServer;

        this.running = new AtomicBoolean( true );

        this.batchIntermediate = new byte[1024];
        this.batchDecompressor = new Inflater();
    }

    /**
     * Adds a {@link ProxiedPacketHandler} to the end of the raw packet handler list, all raw handlers will be called on
     * receivement of an incoming packet with the raw packet data
     *
     * @param handler    May modify the packet data, gets called after all handlers that are already added
     */
    public void addRawPacketHandler( ProxiedPacketHandler handler ) {
        rawPacketHandlers.add( handler );
    }

    /**
     * Adds a {@link ProxiedPacketHandler} to the end of the processed packet handler list, all processed handlers will
     * be called after the raw packet handlers and with processed packet data. So the packet id is always first byte
     * (no 0x8E) and batch packets have been merged.
     *
     * Note: Modifications made to the packetData will be ignored
     *
     * @param handler    May modify the packet data, gets called after all handlers that are already added
     */
    public void addProcessedPacketHandler( ProxiedPacketHandler handler ) {
        processedPacketHandlers.add( handler );
    }

    @Override
    public void run() {
        while ( running.get() ) {
            byte[] packetData = this.inbound.receive();
            if ( packetData == null ) {
                try {
                    Thread.sleep( 1L );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }
                continue;
            }

            for ( ProxiedPacketHandler packetHandler : rawPacketHandlers ) {
                packetData = packetHandler.handlePacket( packetData, this.fromServer, false );
                if ( packetData == null )
                    break; // Don't call any handlers with null packets
            }

            if ( packetData != null ) {
                this.handlePacket( packetData, false );
                this.outbound.sendCopy( packetData );
            }
        }
    }

    private void handlePacket( byte[] packetData, boolean batch ) {
        PacketBuffer buffer = new PacketBuffer( packetData, 0 );
        byte packetId = buffer.readByte();
        if ( packetId == (byte) 0x8E ) {
            packetId = buffer.readByte();
        }

        if ( packetId == (byte) 0x92 ) {
            handleBatchPacket( buffer, batch );
        } else {
            for ( ProxiedPacketHandler packetHandler : processedPacketHandlers ) {
                packetHandler.handlePacket( Arrays.copyOf( packetData, packetData.length ), this.fromServer, batch );
            }
        }
    }

    private void handleBatchPacket( PacketBuffer buffer, boolean batch ) {
        if ( batch ) {
            LOGGER.error( "Invalid nested batch packets" );
            return;
        }

        buffer.skip( 4 );               // Compressed payload length (not of interest; only uncompressed size matteres)

        this.batchDecompressor.reset();
        this.batchDecompressor.setInput( buffer.getBuffer(), buffer.getPosition(), buffer.getRemaining() );

        byte[] payload;
        try {
            // Only inflate decompressed payload size before allocating the actual payload array:
            this.batchDecompressor.inflate( this.batchIntermediate, 0, 4 );
            int decompressedSize = ( ( this.batchIntermediate[0] & 255 ) << 24 |
                    ( this.batchIntermediate[1] & 255 ) << 16 |
                    ( this.batchIntermediate[2] & 255 ) << 8 |
                    ( this.batchIntermediate[3] & 255 ) );

            if ( decompressedSize < 0 ) {
                LOGGER.warn( "Received malformed batch packet; declared negative payload size (" + decompressedSize + ")" );
                return;
            }

            payload = new byte[decompressedSize];
            this.batchDecompressor.inflate( payload );
        } catch ( DataFormatException e ) {
            LOGGER.warn( "Received malformed batch packet", e );
            return;
        }

        this.handlePacket( payload, true );
    }

    /**
     * Stop the loop redirecting the packets
     */
    public void shutdown() {
        this.running.set( false );
    }

}
