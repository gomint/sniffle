/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.entity.metadata;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.inventory.ItemStack;
import io.gomint.proxy.math.Vector;
import lombok.ToString;
import net.openhft.koloboke.collect.map.ByteObjMap;
import net.openhft.koloboke.collect.map.hash.HashByteObjMaps;
import net.openhft.koloboke.function.ByteObjConsumer;

/**
 * @author BlackyPaw
 * @author geNAZt
 * @version 2.0
 */
@ToString
public class MetadataContainer {

    /**
     * Internal byte representation for a byte meta
     */
    static final byte METADATA_BYTE = 0;

    /**
     * Internal byte representation for a short meta
     */
    static final byte METADATA_SHORT = 1;

    /**
     * Internal byte representation for a int meta
     */
    static final byte METADATA_INT = 2;

    /**
     * Internal byte representation for a float meta
     */
    static final byte METADATA_FLOAT = 3;

    /**
     * Internal byte representation for a string meta
     */
    static final byte METADATA_STRING = 4;

    /**
     * Internal byte representation for a item meta
     */
    static final byte METADATA_ITEM = 5;

    /**
     * Internal byte representation for a position meta
     */
    static final byte METADATA_POSITION = 6;

    /**
     * Internal byte representation for a long meta
     */
    static final byte METADATA_LONG = 7;

    /**
     * Internal byte representation for a vector meta
     */
    static final byte METADATA_VECTOR = 8;

    public static final byte DATA_INDEX = 0;
    public static final byte DATA_PLAYER_INDEX = 27;

    private ByteObjMap<MetadataValue> entries;

    /**
     * Constructs a new, empty metadata container.
     */
    public MetadataContainer() {
        this( 8 );
    }

    /**
     * Constructs a new, empty metadata container which may pre-allocate
     * enough internal capacities to hold at least capacity entries.
     *
     * @param capacity The capacity to pre-allocate
     */
    public MetadataContainer( int capacity ) {
        this.entries = HashByteObjMaps.newMutableMap( ( capacity > 32 ? 32 : capacity ) );
    }

    /**
     * Get the flag stored under the given index and flag id
     *
     * @param indexId The index of the value
     * @param flagId  The flag id used to encrypt the boolean
     * @return true when the flag has been set, false when not
     */
    public boolean getDataFlag( int indexId, int flagId ) {
        return ( ( ( indexId == DATA_PLAYER_INDEX ? this.getByte( indexId ) & 0xff : this.getLong( indexId ) ) ) & ( 1 << flagId ) ) > 0;
    }

    /**
     * Set the flag to the index given
     *
     * @param indexId The index of the value
     * @param flagId  The flag id used to encrypt the boolean
     * @param value   The boolean to encrypt
     */
    public void setDataFlag( int indexId, int flagId, boolean value ) {
        if ( this.getDataFlag( indexId, flagId ) != value ) {
            if ( indexId == DATA_PLAYER_INDEX ) {
                byte flags = this.getByte( indexId );
                flags ^= 1 << flagId;
                this.putByte( indexId, flags );
            } else {
                long flags = this.getLong( indexId );
                flags ^= 1 << flagId;
                this.putLong( indexId, flags );
            }
        }
    }

    /**
     * Puts the specified metadata value into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void put( int index, MetadataValue value ) {
        this.entries.put( (byte) index, value );
    }

    /**
     * Checks whether or not the container holds a value at the specified index.
     *
     * @param index The index of the value
     * @return Whether or not the container holds a value at the specified index
     */
    public boolean has( int index ) {
        return this.entries.containsKey( (byte) index );
    }

    /**
     * Gets the metadata value stored at the specified index.
     *
     * @param index The index where the value is stored (must be in the range of 0-31)
     * @return The value if found or null otherwise
     */
    public MetadataValue get( int index ) {
        return this.entries.get( (byte) index );
    }

    /**
     * Puts a boolean value which will be converted into a byte value internally into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void putBoolean( int index, boolean value ) {
        this.putByte( index, (byte) ( value ? 1 : 0 ) );
    }

    /**
     * Gets a boolean stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not a boolean
     */
    public boolean getBoolean( int index ) {
        return ( this.getByte( index ) != 0 );
    }

    /**
     * Puts a byte value into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void putByte( int index, byte value ) {
        this.entries.put( (byte) index, new MetadataByte( value ) );
    }

    /**
     * Gets a byte stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not a byte
     */
    public byte getByte( int index ) {
        MetadataValue value = this.get( index );
        if ( value == null ) {
            throw new IllegalArgumentException( "No value stored at index " + index );
        }

        if ( value.getTypeId() != METADATA_BYTE ) {
            throw new IllegalArgumentException( "Value of different type stored at index " + index );
        }

        return ( (MetadataByte) value ).getValue();
    }

    /**
     * Puts a short value into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void putShort( int index, short value ) {
        this.entries.put( (byte) index, new MetadataShort( value ) );
    }

    /**
     * Gets a short stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not a short
     */
    public short getShort( int index ) {
        MetadataValue value = this.get( index );
        if ( value == null ) {
            throw new IllegalArgumentException( "No value stored at index " + index );
        }

        if ( value.getTypeId() != METADATA_SHORT ) {
            throw new IllegalArgumentException( "Value of different type stored at index " + index );
        }

        return ( (MetadataShort) value ).getValue();
    }

    /**
     * Puts an int value into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void putInt( int index, int value ) {
        this.entries.put( (byte) index, new MetadataInt( value ) );
    }

    /**
     * Gets an int stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not an int
     */
    public int getInt( int index ) {
        MetadataValue value = this.get( index );
        if ( value == null ) {
            throw new IllegalArgumentException( "No value stored at index " + index );
        }

        if ( value.getTypeId() != METADATA_INT ) {
            throw new IllegalArgumentException( "Value of different type stored at index " + index );
        }

        return ( (MetadataInt) value ).getValue();
    }

    /**
     * Puts an float value into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void putFloat( int index, float value ) {
        this.entries.put( (byte) index, new MetadataFloat( value ) );
    }

    /**
     * Gets an float stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not an float
     */
    public float getFloat( int index ) {
        MetadataValue value = this.get( index );
        if ( value == null ) {
            throw new IllegalArgumentException( "No value stored at index " + index );
        }

        if ( value.getTypeId() != METADATA_FLOAT ) {
            throw new IllegalArgumentException( "Value of different type stored at index " + index );
        }

        return ( (MetadataFloat) value ).getValue();
    }

    /**
     * Puts a string value into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void putString( int index, String value ) {
        this.entries.put( (byte) index, new MetadataString( value ) );
    }

    /**
     * Gets a string stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not a string
     */
    public String getString( int index ) {
        MetadataValue value = this.get( index );
        if ( value == null ) {
            throw new IllegalArgumentException( "No value stored at index " + index );
        }

        if ( value.getTypeId() != METADATA_STRING ) {
            throw new IllegalArgumentException( "Value of different type stored at index " + index );
        }

        return ( (MetadataString) value ).getValue();
    }

    /**
     * Put a item value into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void putItem( int index, ItemStack value ) {
        this.entries.put( (byte) index, new MetadataItem( value ) );
    }

    /**
     * Gets a item stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not a item
     */
    public ItemStack getItem( int index ) {
        MetadataValue value = this.get( index );
        if ( value == null ) {
            throw new IllegalArgumentException( "No value stored at index " + index );
        }

        if ( value.getTypeId() != METADATA_ITEM ) {
            throw new IllegalArgumentException( "Value of different type stored at index " + index );
        }

        return ( (MetadataItem) value ).getValue();
    }

    /**
     * Puts an position value into the container.
     *
     * @param index The index to put the value into
     * @param x     The x-value of the position to put into the container
     * @param y     The y-value of the position to put into the container
     * @param z     The z-value of the position to put into the container
     */
    public void putPosition( int index, int x, int y, int z ) {
        this.entries.put( (byte) index, new MetadataPosition( x, y, z ) );
    }

    /**
     * Gets an position stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not an position
     */
    public Vector getPosition( int index ) {
        MetadataValue value = this.get( index );
        if ( value == null ) {
            throw new IllegalArgumentException( "No value stored at index " + index );
        }

        if ( value.getTypeId() != METADATA_POSITION ) {
            throw new IllegalArgumentException( "Value of different type stored at index " + index );
        }

        MetadataPosition position = (MetadataPosition) value;
        return new Vector( position.getX(), position.getY(), position.getZ() );
    }

    /**
     * Puts an long value into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void putLong( int index, long value ) {
        this.entries.put( (byte) index, new MetadataLong( value ) );
    }

    /**
     * Gets an long stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not an long
     */
    public long getLong( int index ) {
        MetadataValue value = this.get( index );
        if ( value == null ) {
            throw new IllegalArgumentException( "No value stored at index " + index );
        }

        if ( value.getTypeId() != METADATA_LONG ) {
            throw new IllegalArgumentException( "Value of different type stored at index " + index );
        }

        return ( (MetadataLong) value ).getValue();
    }

    /**
     * Puts an vector value into the container.
     *
     * @param index The index to put the value into
     * @param value The value to put into the container
     */
    public void putVector( int index, Vector value ) {
        this.entries.put( (byte) index, new MetadataVector( value ) );
    }

    /**
     * Gets an vector stored inside the specified index.
     *
     * @param index The index of the value
     * @return The value stored at the specified index
     * @throws IllegalArgumentException Thrown in case no value is stored at the specified index or the value is not an vector
     */
    public Vector getVector( int index ) {
        MetadataValue value = this.get( index );
        if ( value == null ) {
            throw new IllegalArgumentException( "No value stored at index " + index );
        }

        if ( value.getTypeId() != METADATA_VECTOR ) {
            throw new IllegalArgumentException( "Value of different type stored at index " + index );
        }

        return ( (MetadataVector) value ).getValue();
    }

    /**
     * Serializes this metadata container into the specified buffer.
     *
     * @param buffer The buffer to serialize this metadata container into
     */
    public void serialize( PacketBuffer buffer ) {
        buffer.writeUnsignedVarInt( this.entries.size() );
        this.entries.forEach( new ByteObjConsumer<MetadataValue>() {
            @Override
            public void accept( byte id, MetadataValue metadataValue ) {
                metadataValue.serialize( buffer, id );
            }
        } );
    }

    /**
     * Deserializes this metadata container from the specified buffer.
     *
     * @param buffer The buffer to deserialize this metadata container from
     * @return Whether or not the metadata container could be deserialized successfully
     */
    public boolean deserialize( PacketBuffer buffer ) {
        this.entries.clear();

        int size = buffer.readUnsignedVarInt();
        for ( int i = 0; i < size; i++ ) {
            int index = buffer.readUnsignedVarInt();
            int type = buffer.readUnsignedVarInt();

            MetadataValue value = null;
            switch ( type ) {
                case METADATA_BYTE:
                    value = new MetadataByte();
                    break;
                case METADATA_SHORT:
                    value = new MetadataShort();
                    break;
                case METADATA_INT:
                    value = new MetadataInt();
                    break;
                case METADATA_FLOAT:
                    value = new MetadataFloat();
                    break;
                case METADATA_STRING:
                    value = new MetadataString();
                    break;
                case METADATA_ITEM:
                    value = new MetadataItem();
                    break;
                case METADATA_POSITION:
                    value = new MetadataPosition();
                    break;
                case METADATA_LONG:
                    value = new MetadataLong();
                    break;
                case METADATA_VECTOR:
                    value = new MetadataVector();
                    break;
            }

            if ( value == null ) {
                return false;
            }

            value.deserialize( buffer );
            this.entries.put( (byte) index, value );
        }

        return true;
    }

}
