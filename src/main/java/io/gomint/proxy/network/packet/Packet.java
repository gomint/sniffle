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
import io.gomint.proxy.math.BlockPosition;
import io.gomint.proxy.math.Vector;
import io.gomint.taglib.AllocationLimitReachedException;
import io.gomint.taglib.NBTReader;
import io.gomint.taglib.NBTTagCompound;

import io.gomint.taglib.NBTWriter;
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
    protected final int id;

    /**
     * Constructor for implemented Packets
     *
     * @param id The id which the Packet should use
     */
    protected Packet(int id) {
        this.id = id;
    }

    /**
     * Gets the packet's ID.
     *
     * @return The packet's ID
     */
    public int getId() {
        return this.id;
    }

    /**
     * Serializes this packet into the given buffer.
     *
     * @param buffer The buffer to serialize this packet into
     */
    public abstract void serialize(PacketBuffer buffer);

    /**
     * Deserializes this packet from the given buffer.
     *
     * @param buffer The buffer to deserialize this packet from
     */
    public abstract void deserialize(PacketBuffer buffer);

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

    public void writeGamerules(Map<Gamerule, Object> gamerules, PacketBuffer buffer) {
        if (gamerules == null) {
            buffer.writeUnsignedVarInt(0);
            return;
        }

        buffer.writeUnsignedVarInt(gamerules.size());
        gamerules.forEach(new BiConsumer<Gamerule, Object>() {
            @Override
            public void accept(Gamerule gamerule, Object value) {
                buffer.writeString(gamerule.getNbtName().toLowerCase());

                if (gamerule.getValueType() == Boolean.class) {
                    buffer.writeByte((byte) 1);
                    buffer.writeBoolean((Boolean) value);
                } else if (gamerule.getValueType() == Integer.class) {
                    buffer.writeByte((byte) 2);
                    buffer.writeUnsignedVarInt((Integer) value);
                } else if (gamerule.getValueType() == Float.class) {
                    buffer.writeByte((byte) 3);
                    buffer.writeLFloat((Float) value);
                }
            }
        });
    }

    public Map<Gamerule, Object> readGamerules(PacketBuffer buffer) {
        int amount = buffer.readUnsignedVarInt();
        if (amount == 0) {
            return null;
        }

        Map<Gamerule, Object> gamerules = new HashMap<>();
        for (int i = 0; i < amount; i++) {
            String name = buffer.readString();
            byte type = buffer.readByte();

            Object val = null;
            switch (type) {
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
        }

        return gamerules;
    }

    public static void writeItemStack(ItemStack itemStack, PacketBuffer buffer) {
        if (itemStack == null || itemStack.getMaterial() == 0) {
            buffer.writeSignedVarInt(0);
            return;
        }

        buffer.writeSignedVarInt(itemStack.getMaterial());
        buffer.writeSignedVarInt((itemStack.getData() << 8) + (itemStack.getAmount() & 0xff));

        NBTTagCompound compound = itemStack.getNbtData();
        if (compound == null) {
            buffer.writeLShort((short) 0);
        } else {
            try {
                // Vanilla currently only writes one nbt tag (this is hardcoded)
                buffer.writeLShort((short) 0xFFFF);
                buffer.writeByte((byte) 1);

                // NBT Tag
                NBTWriter nbtWriter = new NBTWriter(buffer.getBuffer(), ByteOrder.LITTLE_ENDIAN);
                nbtWriter.setUseVarint(true);
                nbtWriter.write(compound);
            } catch (IOException e) {
                buffer.writeLShort((short) 0);
            }
        }

        // canPlace and canBreak
        buffer.writeSignedVarInt(0);
        buffer.writeSignedVarInt(0);
    }

    public static void writeItemStacks(ItemStack[] itemStacks, PacketBuffer buffer) {
        if (itemStacks == null || itemStacks.length == 0) {
            buffer.writeUnsignedVarInt(0);
            return;
        }

        buffer.writeUnsignedVarInt(itemStacks.length);

        for (ItemStack itemStack : itemStacks) {
            writeItemStack(itemStack, buffer);
        }
    }

    /**
     * Read in a variable amount of itemstacks
     *
     * @param buffer The buffer to read from
     * @return a list of itemstacks
     */
    public static ItemStack[] readItemStacks(PacketBuffer buffer) {
        int count = buffer.readUnsignedVarInt();
        ItemStack[] itemStacks = new ItemStack[count];

        for (int i = 0; i < count; i++) {
            itemStacks[i] = readItemStack(buffer);
        }

        return itemStacks;
    }

    public static ItemStack readRecipeIngredient(PacketBuffer buffer) {
        int id = buffer.readSignedVarInt();
        if (id == 0) {
            return new ItemStack(0, (short) 0, 0, null);
        }

        int meta = buffer.readSignedVarInt();
        if (meta == 0x7fff) {
            meta = -1;
        }

        int count = buffer.readSignedVarInt();
        return new ItemStack(id, (short) meta, count, null);
    }

    public static ItemStack readItemStack(PacketBuffer buffer) {
        int id = buffer.readSignedVarInt();
        if (id == 0) {
            return new ItemStack(0, (short) 0, 0, null);
        }

        int temp = buffer.readSignedVarInt();
        byte amount = (byte) (temp & 0xFF);
        short data = (short) (temp >> 8);

        NBTTagCompound nbt = null;
        short extraLen = buffer.readLShort();
        if (extraLen > 0) {
            try {
                NBTReader nbtReader = new NBTReader(buffer.getBuffer(), ByteOrder.LITTLE_ENDIAN);
                nbtReader.setUseVarint(true);
                // There is no alloc limit needed here, you can't write so much shit in 32kb, so thats ok
                nbt = nbtReader.parse();
            } catch (IOException | AllocationLimitReachedException e) {
                return null;
            }
        } else if (extraLen == -1) {
            // New system uses a byte as amount of nbt tags
            byte count = buffer.readByte();
            for (byte i = 0; i < count; i++) {
                try {
                    NBTReader nbtReader = new NBTReader(buffer.getBuffer(), ByteOrder.LITTLE_ENDIAN);
                    nbtReader.setUseVarint(true);
                    // There is no alloc limit needed here, you can't write so much shit in 32kb, so thats ok
                    nbt = nbtReader.parse();
                } catch (IOException | AllocationLimitReachedException e) {
                    return null;
                }
            }
        }

        // They implemented additional data for item stacks aside from nbt
        int countPlacedOn = buffer.readSignedVarInt();
        for (int i = 0; i < countPlacedOn; i++) {
            String a = buffer.readString();    // TODO: Implement proper support once we know the string values
            System.out.println(a);
        }

        int countCanBreak = buffer.readSignedVarInt();
        for (int i = 0; i < countCanBreak; i++) {
            buffer.readString();
        }

        // Special case shield?
        if (id == 513) {
            buffer.readSignedVarInt();
        }

        return new ItemStack(id, data, amount, nbt);
    }

    public BlockPosition readBlockPosition(PacketBuffer buffer) {
        return new BlockPosition(buffer.readSignedVarInt(), buffer.readUnsignedVarInt(), buffer.readSignedVarInt());
    }

    public void writeBlockPosition(BlockPosition position, PacketBuffer buffer) {
        buffer.writeSignedVarInt(position.getX());
        buffer.writeUnsignedVarInt(position.getY());
        buffer.writeSignedVarInt(position.getZ());
    }

    void writeVector(Vector vector, PacketBuffer buffer) {
        buffer.writeLFloat(vector.getX());
        buffer.writeLFloat(vector.getY());
        buffer.writeLFloat(vector.getZ());
    }

    Vector readVector(PacketBuffer buffer) {
        return new Vector(buffer.readLFloat(), buffer.readLFloat(), buffer.readLFloat());
    }

    public void serializeHeader(PacketBuffer buffer) {
        buffer.writeUnsignedVarInt(this.id);
    }

    /**
     * Write a array of item stacks to the buffer
     *
     * @param itemStacks which should be written to the buffer
     * @param buffer     which should be written to
     */
    void writeItemStacksWithIDs(ItemStack[] itemStacks, PacketBuffer buffer) {
        if (itemStacks == null || itemStacks.length == 0) {
            buffer.writeUnsignedVarInt(0);
            return;
        }

        buffer.writeUnsignedVarInt(itemStacks.length);

        for (ItemStack itemStack : itemStacks) {
            writeItemStackWithID(itemStack, buffer);
        }
    }

    public static ItemStack readItemStackWithID(PacketBuffer buffer) {
        int id = buffer.readSignedVarInt();
        ItemStack itemStack = readItemStack(buffer);
        if (itemStack != null) {
            itemStack.setId(id);
        }

        return itemStack;
    }

    public static void writeItemStackWithID(ItemStack itemStack, PacketBuffer buffer) {
        buffer.writeSignedVarInt(itemStack.getId());
        writeItemStack(itemStack, buffer);
    }

    /**
     * Read in a variable amount of itemstacks
     *
     * @param buffer The buffer to read from
     * @return a list of item stacks
     */
    ItemStack[] readItemStacksWithIDs(PacketBuffer buffer) {
        int count = buffer.readUnsignedVarInt();
        ItemStack[] itemStacks = new ItemStack[count];

        for (int i = 0; i < count; i++) {
            itemStacks[i] = readItemStackWithID(buffer);
        }

        return itemStacks;
    }


}