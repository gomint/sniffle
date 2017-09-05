package io.gomint.proxy.entity.metadata;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.inventory.ItemStack;
import io.gomint.proxy.inventory.MaterialMagicNumbers;
import io.gomint.proxy.network.packet.Packet;
import io.gomint.proxy.util.EnumConnectors;

/**
 * @author geNAZt
 * @version 1.0
 */
public class MetadataItem extends MetadataValue {

    private ItemStack value;

    /**
     * Constructs a new metadata item
     */
    public MetadataItem() {

    }

    /**
     * Constructs a new metadata item and initializes it with the specified value.
     *
     * @param value The value to initialize the metadata item with
     */
    public MetadataItem( ItemStack value ) {
        this.value = value;
    }

    /**
     * Gets the value of this metadata item.
     *
     * @return The value of this metadata item
     */
    public ItemStack getValue() {
        return this.value;
    }

    /**
     * Sets the value of this metadata item.
     *
     * @param value The value of this metadata item
     */
    public void setValue( ItemStack value ) {
        this.value = value;
    }

    @Override
    void serialize( PacketBuffer buffer, int index ) {
        super.serialize( buffer, index );
        Packet.writeItemStack( this.value, buffer );
    }

    @Override
    void deserialize( PacketBuffer buffer ) {
        this.value = new ItemStack( EnumConnectors.MATERIAL_CONNECTOR.revert( MaterialMagicNumbers.valueOfWithId( buffer.readLShort() ) ), buffer.readByte(), buffer.readLShort() );
    }

    @Override
    byte getTypeId() {
        return MetadataContainer.METADATA_ITEM;
    }

}
