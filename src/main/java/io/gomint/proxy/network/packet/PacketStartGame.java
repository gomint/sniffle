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
    // Entity data
    private long entityId;
    private long runtimeEntityId;
    private int gamemode;
    private Location spawn;

    // Level data
    private int seed;
    private int dimension;
    private int generator;
    private int worldGamemode;
    private int difficulty;
    private int x;
    private int y;
    private int z;
    private boolean hasAchievementsDisabled = true;
    private int dayCycleStopTime;
    private boolean eduMode;
    private float rainLevel;
    private float lightningLevel;
    private boolean isMultiplayerGame = true;
    private boolean hasLANBroadcast = true;
    private boolean hasXboxLiveBroadcast = false;
    private boolean commandsEnabled;
    private boolean isTexturePacksRequired;

    // Gamerule data
    private Map<Gamerule, Object> gamerules;
    private boolean hasBonusChestEnabled;
    private boolean hasStartWithMapEnabled;
    private boolean hasTrustPlayersEnabled;
    private int defaultPlayerPermission = 1;
    private int xboxLiveBroadcastMode = 0;

    // World data
    private String levelId;
    private String worldName;
    private String templateName;
    private boolean unknown1;
    private long currentTick;
    private int enchantmentSeed;

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
        buffer.writeBoolean( this.isMultiplayerGame );
        buffer.writeBoolean( this.hasLANBroadcast );
        buffer.writeBoolean( this.hasXboxLiveBroadcast );
        buffer.writeBoolean( this.commandsEnabled );
        buffer.writeBoolean( this.isTexturePacksRequired );
        writeGamerules( this.gamerules, buffer );
        buffer.writeBoolean( this.hasBonusChestEnabled );
        buffer.writeBoolean( this.hasStartWithMapEnabled );
        buffer.writeBoolean( this.hasTrustPlayersEnabled );
        buffer.writeSignedVarInt( this.defaultPlayerPermission );
        buffer.writeSignedVarInt( this.xboxLiveBroadcastMode );

        buffer.writeString( this.levelId );
        buffer.writeString( this.worldName );
        buffer.writeString( this.templateName );
        buffer.writeBoolean( this.unknown1 );
        buffer.writeLLong( this.currentTick );
        buffer.writeSignedVarInt( this.enchantmentSeed );
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        this.entityId = buffer.readSignedVarLong().longValue();
        this.runtimeEntityId = buffer.readUnsignedVarLong();
        this.gamemode = buffer.readSignedVarInt();

        this.spawn = new Location( buffer.readLFloat(), buffer.readLFloat(), buffer.readLFloat(), buffer.readLFloat(), buffer.readLFloat() );
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
        this.isMultiplayerGame = buffer.readBoolean();
        this.hasLANBroadcast = buffer.readBoolean();
        this.hasXboxLiveBroadcast = buffer.readBoolean();
        this.commandsEnabled = buffer.readBoolean();
        this.isTexturePacksRequired = buffer.readBoolean();
        this.gamerules = readGamerules( buffer );
        this.hasBonusChestEnabled = buffer.readBoolean();
        this.hasStartWithMapEnabled = buffer.readBoolean();
        this.hasTrustPlayersEnabled = buffer.readBoolean();

        this.defaultPlayerPermission = buffer.readSignedVarInt();
        this.xboxLiveBroadcastMode = buffer.readSignedVarInt();

        this.levelId = buffer.readString();
        this.worldName = buffer.readString();
        this.templateName = buffer.readString();
        this.unknown1 = buffer.readBoolean();
        this.currentTick = buffer.readLLong();
        this.enchantmentSeed = buffer.readSignedVarInt();

        System.out.println( this );
    }
}
