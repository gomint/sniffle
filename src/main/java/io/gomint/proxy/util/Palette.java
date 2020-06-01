/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.util;

import io.gomint.jraknet.PacketBuffer;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * @author geNAZt
 * @version 1.0
 */
@Getter
public class Palette {

    @Getter
    private enum PaletteVersion {
        P1( 1, 32 ),
        P2( 2, 16 ),
        P3( 3, 10, 2 ),
        P4( 4, 8 ),
        P5( 5, 6, 2 ),
        P6( 6, 5, 2 ),
        P8( 8, 4 ),
        P16( 16, 2 );

        private final byte versionId;
        private final byte amountOfWords;
        private final byte amountOfPadding;

        PaletteVersion( int versionId, int amountOfWords ) {
            this( versionId, amountOfWords, 0 );
        }

        PaletteVersion( int versionId, int amountOfWords, int amountOfPadding ) {
            this.versionId = (byte) versionId;
            this.amountOfWords = (byte) amountOfWords;
            this.amountOfPadding = (byte) amountOfPadding;
        }
    }

    private ByteBuffer dataOutput;
    private PacketBuffer buffer;
    private PaletteVersion paletteVersion = null;

    // Output indexes
    private short[] output = null;

    // Input bitset
    private BitSet input = null;
    private int inputIndex = 0;
    private int wordsWritten = 0;

    /**
     * Construct a new reader for the given palette version
     *
     * @param in      of the data
     * @param version of the palette
     */
    public Palette( PacketBuffer in, byte version ) {
        this.buffer = in;

        for ( PaletteVersion paletteVersionCanidate : PaletteVersion.values() ) {
            if ( paletteVersionCanidate.getVersionId() == version ) {
                this.paletteVersion = paletteVersionCanidate;
                break;
            }
        }

        if ( this.paletteVersion == null ) {
            throw new IllegalArgumentException( "Palette version " + version + " is unknown" );
        }
    }

    public Palette( ByteBuffer data, int version ) {
        this.dataOutput = data;

        for ( PaletteVersion paletteVersionCanidate : PaletteVersion.values() ) {
            if ( paletteVersionCanidate.getVersionId() == version ) {
                this.paletteVersion = paletteVersionCanidate;
                break;
            }
        }

        if ( this.paletteVersion == null ) {
            throw new IllegalArgumentException( "Palette version " + version + " is unknown" );
        }
    }

    public void addIndex( Integer id ) {
        // Do we need new input?
        if ( this.input == null ) {
            this.input = new BitSet( 32 );
            this.inputIndex = 0;
        }

        // Check if old input is full and we need a new one
        if ( this.wordsWritten == this.paletteVersion.getAmountOfWords() ) {
            // Write to output
            this.dataOutput.putInt( this.convert( this.input ) );

            // New input
            this.input = new BitSet( 32 );
            this.inputIndex = 0;
            this.wordsWritten = 0;
        }

        // Write id
        while ( id != 0L ) {
            if ( id % 2L != 0 ) {
                this.input.set( this.inputIndex );
            }

            ++this.inputIndex;
            id = id >>> 1;
        }

        // Increment written words
        this.wordsWritten++;
    }

    public void finish() {
        this.dataOutput.putInt( this.convert( this.input ) );
        this.input = null;
    }

    public short[] getIndexes() {
        // Do we need to read first?
        if ( this.output == null ) {
            this.output = new short[4096];

            // We need the amount of iterations
            int iterations = (int) Math.floor( 4096 / (float) this.paletteVersion.getAmountOfWords() );
            for ( int i = 0; i < iterations; i++ ) {
                BitSet bitSet = convert( this.buffer.readLInt() );
                int index = 0;

                for ( byte b = 0; b < this.paletteVersion.getAmountOfWords(); b++ ) {
                    short val = 0;
                    int innerShiftIndex = 0;

                    for ( byte i1 = 0; i1 < this.paletteVersion.getVersionId(); i1++ ) {
                        if ( bitSet.get( index++ ) ) {
                            val ^= 1 << innerShiftIndex;
                        }

                        innerShiftIndex++;
                    }

                    int setIndex = ( i * this.paletteVersion.getAmountOfWords() ) + b;
                    if ( setIndex < 4096 ) {
                        this.output[setIndex] = val;
                    }
                }
            }
        }

        return this.output;
    }

    public boolean isPadded() {
        return this.paletteVersion.getAmountOfPadding() > 0;
    }

    private int convert( BitSet bits ) {
        int value = 0;

        for ( int i = 0; i < bits.length(); ++i ) {
            value += bits.get( i ) ? ( 1L << i ) : 0L;
        }

        return value;
    }

    private BitSet convert( int value ) {
        BitSet bits = new BitSet( 32 );
        int index = 0;
        while ( value != 0L ) {
            if ( value % 2L != 0 ) {
                bits.set( index );
            }

            ++index;
            value = value >>> 1;
        }

        return bits;
    }


}
