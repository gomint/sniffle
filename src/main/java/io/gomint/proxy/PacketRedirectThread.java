/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy;

import io.gomint.jraknet.Connection;
import io.gomint.jraknet.EncapsulatedPacket;
import io.gomint.jraknet.PacketBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

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

    private final byte[] batchIntermediate;
    private final Inflater batchDecompressor;

    public PacketRedirectThread( Connection inbound, Connection outbound, boolean fromServer ) {
        super( ( fromServer ? "Server" : "Client" ) + "PacketRedirectThread" );

        this.inbound = inbound;
        this.outbound = outbound;
        this.fromServer = fromServer;

        this.running = new AtomicBoolean( true );

        this.batchIntermediate = new byte[4];
        this.batchDecompressor = new Inflater();
    }

    /**
     * Adds a {@link ProxiedPacketHandler} to the end of the raw packet handler list, all raw handlers will be called on
     * receivement of an incoming packet with the raw packet data
     *
     * @param handler May modify the packet data, gets called after all handlers that are already added
     */
    public void addRawPacketHandler( ProxiedPacketHandler handler ) {
        rawPacketHandlers.add( handler );
    }

    /**
     * Adds a {@link ProxiedPacketHandler} to the end of the processed packet handler list, all processed handlers will
     * be called after the raw packet handlers and with processed packet data. So the packet id is always first byte
     * (no 0x8E) and batch packets have been merged.
     * <p>
     * Note: Modifications made to the packetData will be ignored
     *
     * @param handler May modify the packet data, gets called after all handlers that are already added
     */
    public void addProcessedPacketHandler( ProxiedPacketHandler handler ) {
        processedPacketHandlers.add( handler );
    }

    @Override
    public void run() {
        while ( running.get() ) {
            EncapsulatedPacket packetData = this.inbound.poll();
            if ( packetData == null ) {
                try {
                    Thread.sleep( 1L );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }

                continue;
            }

            byte[] packetDataRaw = packetData.getPacketData();
            for ( ProxiedPacketHandler packetHandler : rawPacketHandlers ) {
                packetDataRaw = packetHandler.handlePacket( packetDataRaw, this.fromServer, false );
                if ( packetDataRaw == null ) {
                    break; // Don't call any handlers with null packets
                }
            }

            if ( packetDataRaw != null ) {
                this.handlePacket( packetDataRaw, false );
                this.outbound.sendCopy( packetDataRaw );
            }
        }
    }

    private void handlePacket( byte[] packetData, boolean batch ) {
        PacketBuffer buffer = new PacketBuffer( packetData, 0 );
        byte packetId = buffer.readByte();
        if ( packetId == (byte) 0xFE ) {
            packetId = buffer.readByte();

            byte[] oldData = packetData;
            packetData = new byte[packetData.length - 1];
            System.arraycopy( oldData, 1, packetData, 0, packetData.length );
        }

        if ( packetId == (byte) 0x06 ) {
            handleBatchPacket( buffer, batch );
        } else {
            for ( ProxiedPacketHandler packetHandler : processedPacketHandlers ) {
                packetHandler.handlePacket( Arrays.copyOf( packetData, packetData.length ), this.fromServer, batch );
            }
        }
    }

    private void handleBatchPacket( PacketBuffer buffer, boolean batch ) {
        List<PacketBuffer> buffers = Util.handleBatchPacket( buffer, batch );
        for ( PacketBuffer packetBuffer : buffers ) {
            this.handlePacket( packetBuffer.getBuffer(), true );
        }
    }

    /**
     * Stop the loop redirecting the packets
     */
    public void shutdown() {
        this.running.set( false );
    }

}
