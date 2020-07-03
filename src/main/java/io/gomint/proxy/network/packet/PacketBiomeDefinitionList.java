package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.asset.AssetAssembler;
import io.gomint.proxy.network.PacketRegistry;
import io.gomint.proxy.util.DumpUtil;
import io.gomint.taglib.AllocationLimitReachedException;
import io.gomint.taglib.NBTReader;
import io.gomint.taglib.NBTTagCompound;
import io.gomint.taglib.NBTWriter;
import java.io.IOException;
import java.nio.ByteOrder;
import lombok.Data;

/**
 * @author HerryYT
 * @version 1.0
 */
@Data
public class PacketBiomeDefinitionList extends Packet {

  private NBTTagCompound nbt;

  public PacketBiomeDefinitionList() {
    super(PacketRegistry.PACKET_BIOME_DEFINITION_LIST);
  }

  @Override
  public void serialize(PacketBuffer buffer) {
    try {
      NBTWriter nbtWriter = new NBTWriter(buffer.getBuffer(), ByteOrder.LITTLE_ENDIAN);
      nbtWriter.setUseVarint(true);
      nbtWriter.write(this.nbt);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void deserialize(PacketBuffer buffer) {
    NBTReader nbtReader = new NBTReader(buffer.getBuffer(), ByteOrder.LITTLE_ENDIAN);
    nbtReader.setUseVarint(true);

    try {
      this.nbt = nbtReader.parse();
    } catch (IOException | AllocationLimitReachedException e) {
      e.printStackTrace();
      return;
    }

    AssetAssembler.writeBiomeDefinition(this.nbt);
  }

}
