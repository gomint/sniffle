package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.Gamerule;
import io.gomint.proxy.math.Location;
import io.gomint.proxy.network.PacketRegistry;
import lombok.Data;

import java.util.Map;

/**
 * @author geNAZt
 * @version 1.0
 */
@Data
public class PacketStartGame extends Packet {

    private long entityId;
    private long runtimeEntityId;
    private int gamemode;
    private Location spawn;
    private int seed;
    private int dimension;
    private int generator;
    private int worldGamemode;
    private int difficulty;
    private boolean hasAchievementsDisabled = true;
    private int dayCycleStopTime;
    private boolean eduMode;
    private float rainLevel;
    private float lightningLevel;
    private boolean commandsEnabled;
    private boolean isTexturePacksRequired;
    private Map<Gamerule, Object> gamerules;
    private String secret;
    private String worldName;

    public PacketStartGame() {
        super( PacketRegistry.PACKET_START_GAME );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeSignedVarLong( this.entityId );
        buffer.writeUnsignedVarLong( this.runtimeEntityId );
        buffer.writeSignedVarInt( this.gamemode );

        buffer.writeLFloat( this.spawn.getX() );
        buffer.writeLFloat( this.spawn.getY() );
        buffer.writeLFloat( this.spawn.getZ() );
        buffer.writeLFloat( this.spawn.getYaw() );
        buffer.writeLFloat( this.spawn.getPitch() );

        buffer.writeSignedVarInt( this.seed );
        buffer.writeSignedVarInt( this.dimension );
        buffer.writeSignedVarInt( this.generator );
        buffer.writeSignedVarInt( this.worldGamemode );
        buffer.writeSignedVarInt( this.difficulty );
        buffer.writeSignedVarInt( (int) this.spawn.getX() );
        buffer.writeSignedVarInt( (int) this.spawn.getY() );
        buffer.writeSignedVarInt( (int) this.spawn.getZ() );
        buffer.writeBoolean( this.hasAchievementsDisabled );
        buffer.writeSignedVarInt( this.dayCycleStopTime );
        buffer.writeBoolean( this.eduMode );
        buffer.writeLFloat( this.rainLevel );
        buffer.writeLFloat( this.lightningLevel );
        buffer.writeBoolean( this.commandsEnabled );
        buffer.writeBoolean( this.isTexturePacksRequired );
        writeGamerules( this.gamerules, buffer );
        buffer.writeString( this.secret );
        buffer.writeString( this.worldName );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.entityId = buffer.readSignedVarLong().longValue();
        this.runtimeEntityId = buffer.readUnsignedVarLong();
        this.gamemode = buffer.readSignedVarInt();

        this.spawn = new Location( buffer.readLFloat(),
                buffer.readLFloat(),
                buffer.readLFloat(),
                buffer.readLFloat(),
                buffer.readLFloat() );

        this.seed = buffer.readSignedVarInt();
        this.dimension = buffer.readSignedVarInt();
        this.generator = buffer.readSignedVarInt();
        this.worldGamemode = buffer.readSignedVarInt();
        this.difficulty = buffer.readSignedVarInt();

        buffer.readSignedVarInt();
        buffer.readSignedVarInt();
        buffer.readSignedVarInt();

        this.hasAchievementsDisabled = buffer.readBoolean();
        this.dayCycleStopTime = buffer.readSignedVarInt();
        this.eduMode = buffer.readBoolean();
        this.rainLevel = buffer.readLFloat();
        this.lightningLevel = buffer.readLFloat();
        this.commandsEnabled = buffer.readBoolean();
        this.isTexturePacksRequired = buffer.readBoolean();
        this.gamerules = readGamerules( buffer );
        this.secret = buffer.readString();
        this.worldName = buffer.readString();

        System.out.println( this );
    }

}
