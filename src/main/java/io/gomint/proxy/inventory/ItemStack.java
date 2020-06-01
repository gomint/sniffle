/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.inventory;

import io.gomint.taglib.NBTTagCompound;
import lombok.ToString;

/**
 * Represents a stack of up to 255 items of the same type which may
 * optionally also have an additional data value. May be cloned.
 *
 * @author BlackyPaw
 * @version 1.0
 */
@ToString
public class ItemStack implements Cloneable {

    private int material;
    private short data;
    private byte amount;
    private NBTTagCompound nbt;

    /**
     * Constructs a new item stack that will hold the given amount
     * of items of the specified type. Additionally specifies the
     * data value of the item.
     *
     * @param material The material of the item
     * @param data     The data value of the item
     * @param amount   The number of items on this stack (255 max)
     */
    public ItemStack( int material, short data, int amount ) {
        this.material = material;
        this.data = data;
        this.amount = (byte) ( amount > Byte.MAX_VALUE ? Byte.MAX_VALUE : amount );
    }

    /**
     * Constructs a new item stack that will hold the given amount
     * of items of the specified type. Additionally specifies the
     * data value of the item as well as raw NBT data that resembles
     * additional required storage such as a chest's inventory.
     *
     * @param material The material of the item
     * @param data     The data value of the item
     * @param amount   The number of items on this stack (255 max)
     * @param nbt      The additional raw NBT data of the item
     */
    public ItemStack( int material, short data, int amount, NBTTagCompound nbt ) {
        this( material, data, amount );
        this.nbt = nbt;
    }

    /**
     * Gets the material of the item(s) on this stack.
     *
     * @return The material of the item(s) on this stack
     */
    public int getMaterial() {
        return this.material;
    }

    /**
     * The data value of the item(s) on this stack.
     *
     * @return The data value of the item(s) on this stack
     */
    public short getData() {
        return this.data;
    }

    /**
     * Sets the additional data value of the item(s) on this stack.
     *
     * @param data The data value of the item(s) on this stack
     */
    public void setData( short data ) {
        this.data = data;
    }

    /**
     * Gets the number of items on this stack.
     *
     * @return The number of items on this stack
     */
    public byte getAmount() {
        return this.amount;
    }

    /**
     * Gets the raw NBT data of the item(s) on this stack.
     *
     * @return The raw NBT data of the item(s) on this stack or null
     */
    public NBTTagCompound getNbtData() {
        return this.nbt;
    }

    /**
     * Set new nbt data into the itemstack
     *
     * @param compound The raw NBT data of this item
     */
    public void setNbtData( NBTTagCompound compound ) {
        this.nbt = compound;
    }

    @Override
    public final boolean equals( Object other ) {
        if ( !( other instanceof ItemStack ) ) return false;
        ItemStack otherItemStack = (ItemStack) other;
        return this.getMaterial() == otherItemStack.getMaterial() &&
                this.getData() == otherItemStack.getData() &&
                ( this.nbt == otherItemStack.nbt || this.nbt.equals( otherItemStack.nbt ) );
    }

}
