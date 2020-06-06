package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.network.PacketRegistry;

public class PacketEncryptionReady extends Packet {

  /**
   * Constructor for implemented Packets
  */
  public PacketEncryptionReady() {
    super(PacketRegistry.PACKET_ENCRYPTION_READY);
  }

  @Override
  public void serialize(PacketBuffer buffer) {

  }

  @Override
  public void deserialize(PacketBuffer buffer) {

  }
}
