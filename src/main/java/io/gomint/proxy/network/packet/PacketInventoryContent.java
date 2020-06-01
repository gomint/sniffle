package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.asset.AssetAssembler;
import io.gomint.proxy.inventory.ItemStack;
import io.gomint.proxy.network.PacketRegistry;
import lombok.Data;

/**
 * @author geNAZt
 */
@Data
public class PacketInventoryContent extends Packet {

    private int windowId;
    private ItemStack[] items;

    public PacketInventoryContent() {
        super( PacketRegistry.PACKET_INVENTORY_CONTENT_PACKET );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeUnsignedVarInt( this.windowId );
        writeItemStacks( this.items, buffer );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.windowId = buffer.readUnsignedVarInt();
        this.items = readItemStacks( buffer );

        if ( this.windowId == 121 ) {
            AssetAssembler.writeCreativeInventory( this.items );
        }
    }

}
