package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.network.PacketRegistry;
import lombok.ToString;

@ToString
public class PacketAdventureSettings extends Packet {

    private int flags;
    private int commandPermission;
    private int flags2;
    private int playerPermission;
    private int customFlags;
    private long entityId;

    public PacketAdventureSettings() {
        super( PacketRegistry.PACKET_ADVENTURE_SETTINGS );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeUnsignedVarInt( this.flags );
        buffer.writeUnsignedVarInt( this.commandPermission );
        buffer.writeUnsignedVarInt( this.flags2 );
        buffer.writeUnsignedVarInt( this.playerPermission );
        buffer.writeUnsignedVarInt( this.customFlags );
        buffer.writeLLong( this.entityId );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.flags = buffer.readUnsignedVarInt();
        this.commandPermission = buffer.readUnsignedVarInt();
        this.flags2 = buffer.readUnsignedVarInt();
        this.playerPermission = buffer.readUnsignedVarInt();
        this.customFlags = buffer.readUnsignedVarInt();
        this.entityId = buffer.readLLong();

        System.out.println( this ); // 367015600t
    }

}
