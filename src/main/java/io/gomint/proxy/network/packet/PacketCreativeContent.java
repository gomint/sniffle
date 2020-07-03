/*
 * Copyright (c) 2018 Gomint team
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.asset.AssetAssembler;
import io.gomint.proxy.inventory.ItemStack;
import io.gomint.proxy.network.PacketRegistry;
import lombok.Data;

/**
 * @author geNAZt
 * @version 1.0
 */
@Data
public class PacketCreativeContent extends Packet {

    private ItemStack[] items;

    public PacketCreativeContent() {
        super(PacketRegistry.PACKET_CREATIVE_CONTENT);
    }

    @Override
    public void serialize(PacketBuffer buffer) {
        writeItemStacksWithIDs(this.items, buffer);
    }

    @Override
    public void deserialize(PacketBuffer buffer) {
        this.items = readItemStacksWithIDs(buffer);
        AssetAssembler.writeCreativeInventory(this.items);
    }

}
