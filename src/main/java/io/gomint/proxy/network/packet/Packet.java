/*
 * Copyright (c) 2016, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.Gamerule;
import io.gomint.proxy.inventory.ItemStack;
import io.gomint.proxy.inventory.Material;
import io.gomint.proxy.inventory.MaterialMagicNumbers;
import io.gomint.proxy.util.EnumConnectors;
import io.gomint.taglib.NBTTagCompound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author geNAZt
 * @version 1.0
 */
public abstract class Packet {

    /**
     * Internal MC:PE id of this packet
     */
	protected final byte id;

	/**
	 * Constructor for implemented Packets
	 *
	 * @param id The id which the Packet should use
     */
	protected Packet( byte id ) {
		this.id = id;
	}

	/**
	 * Gets the packet's ID.
	 *
	 * @return The packet's ID
	 */
	public byte getId() {
		return this.id;
	}

	/**
	 * Serializes this packet into the given buffer.
	 *
	 * @param buffer The buffer to serialize this packet into
	 */
	public abstract void serialize( PacketBuffer buffer );

	/**
	 * Deserializes this packet from the given buffer.
	 *
	 * @param buffer The buffer to deserialize this packet from
	 */
	public abstract void deserialize( PacketBuffer buffer );

	/**
	 * Returns an estimate length of the packet (used for pre-allocation).
	 *
	 * @return The estimate length of the packet or -1 if unknown
	 */
	public int estimateLength() {
		return -1;
	}

	/**
	 * Returns the ordering channel to send the packet on.
	 *
	 * @return The ordering channel of the packet
	 */
	public int orderingChannel() {
		return 0;
	}

	public void writeGamerules( Map<Gamerule, Object> gamerules, PacketBuffer buffer ) {
		if ( gamerules == null ) {
			buffer.writeUnsignedVarInt( 0 );
			return;
		}

		buffer.writeUnsignedVarInt( gamerules.size() );
		gamerules.forEach( new BiConsumer<Gamerule, Object>() {
			@Override
			public void accept( Gamerule gamerule, Object value ) {
				buffer.writeString( gamerule.getNbtName().toLowerCase() );

				if ( gamerule.getValueType() == Boolean.class ) {
					buffer.writeByte( (byte) 1 );
					buffer.writeBoolean( (Boolean) value );
				} else if ( gamerule.getValueType() == Integer.class ) {
					buffer.writeByte( (byte) 2 );
					buffer.writeUnsignedVarInt( (Integer) value );
				} else if ( gamerule.getValueType() == Float.class ) {
					buffer.writeByte( (byte) 3 );
					buffer.writeLFloat( (Float) value );
				}
			}
		} );
	}

	public Map<Gamerule, Object> readGamerules( PacketBuffer buffer ) {
		int amount = buffer.readUnsignedVarInt();
		if ( amount == 0 ) {
			return null;
		}

		Map<Gamerule, Object> gamerules = new HashMap<>();
		for ( int i = 0; i < amount; i++ ) {
			String name = buffer.readString();
			byte type = buffer.readByte();

			Object val = null;
			switch ( type ) {
				case 1:
					val = buffer.readBoolean();
					break;
				case 2:
					val = buffer.readUnsignedVarInt();
					break;
				case 3:
					val = buffer.readLFloat();
					break;
			}

			System.out.println( name + " -> " + String.valueOf( val ) );
		}

		return gamerules;
	}

	public static void writeItemStack( ItemStack itemStack, PacketBuffer buffer ) {
		if ( itemStack == null || itemStack.getMaterial() == Material.AIR ) {
			buffer.writeSignedVarInt( 0 );
			return;
		}

		buffer.writeSignedVarInt( EnumConnectors.MATERIAL_CONNECTOR.convert( itemStack.getMaterial() ).getOldId() );
		buffer.writeSignedVarInt( ( itemStack.getData() << 8 ) + ( itemStack.getAmount() & 0xff ) );

		NBTTagCompound compound = itemStack.getNbtData();
		if ( compound == null ) {
			buffer.writeLShort( (short) 0 );
		} else {
			try {
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
				compound.writeTo( byteArrayOutputStream, false, ByteOrder.LITTLE_ENDIAN );
				buffer.writeLShort( (short) byteArrayOutputStream.size() );
				buffer.writeBytes( byteArrayOutputStream.toByteArray() );
			} catch ( IOException e ) {
				e.printStackTrace();
				buffer.writeLShort( (short) 0 );
			}
		}

		// canPlace and canBreak
		buffer.writeSignedVarInt( 0 );
		buffer.writeSignedVarInt( 0 );
	}

	public static void writeItemStacks( ItemStack[] itemStacks, PacketBuffer buffer ) {
		if ( itemStacks == null || itemStacks.length == 0 ) {
			buffer.writeUnsignedVarInt( 0 );
			return;
		}

		buffer.writeUnsignedVarInt( itemStacks.length );

		for ( ItemStack itemStack : itemStacks ) {
			writeItemStack( itemStack, buffer );
		}
	}

	/**
	 * Read in a variable amount of itemstacks
	 *
	 * @param buffer The buffer to read from
	 * @return a list of itemstacks
	 */
	public static ItemStack[] readItemStacks( PacketBuffer buffer ) {
		int count = buffer.readUnsignedVarInt();
		ItemStack[] itemStacks = new ItemStack[count];

		for ( int i = 0; i < count; i++ ) {
			itemStacks[i] = readItemStack( buffer );
		}

		return itemStacks;
	}
	public static ItemStack readItemStack( PacketBuffer buffer ) {
		int id = buffer.readSignedVarInt();
		if ( id == 0 ) {
			return new ItemStack( Material.AIR, (short) 0, 0, null );
		}

		int temp = buffer.readSignedVarInt();
		byte amount = (byte) ( temp & 0xFF );
		short data = (short) ( temp >> 8 );

		NBTTagCompound nbt = null;
		short extraLen = buffer.readLShort();
		if ( extraLen > 0 ) {
			ByteArrayInputStream bin = new ByteArrayInputStream( buffer.getBuffer(), buffer.getPosition(), extraLen );
			try {
				nbt = NBTTagCompound.readFrom( bin, false, ByteOrder.LITTLE_ENDIAN );
			} catch ( IOException e ) {
				e.printStackTrace();
			}

			buffer.skip( extraLen );
		}

		// They implemented additional data for item stacks aside from nbt
		int countPlacedOn = buffer.readSignedVarInt();
		for ( int i = 0; i < countPlacedOn; i++ ) {
			buffer.readString();    // TODO: Implement proper support once we know the string values
		}

		int countCanBreak = buffer.readSignedVarInt();
		for ( int i = 0; i < countCanBreak; i++ ) {
			buffer.readString();    // TODO: Implement proper support once we know the string values
		}

		return new ItemStack( EnumConnectors.MATERIAL_CONNECTOR.revert( MaterialMagicNumbers.valueOfWithId( id ) ), data, amount, nbt );
	}

}