package io.gomint.proxy.network.packet;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.Gamerule;
import io.gomint.proxy.asset.AssetAssembler;
import io.gomint.proxy.math.Location;
import io.gomint.proxy.network.PacketRegistry;
import io.gomint.proxy.util.StringShortPair;
import io.gomint.taglib.AllocationLimitReachedException;
import io.gomint.taglib.NBTReader;
import java.io.IOException;
import java.nio.ByteOrder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author geNAZt
 * @version 1.0
 */
@Data
public class PacketStartGame extends Packet {

    private byte[] data;

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
    private boolean hasEduModeEnabled;
    private float rainLevel;
    private float lightningLevel;
    private boolean isMultiplayerGame = true;
    private boolean hasLANBroadcast = true;
    private boolean hasXboxLiveBroadcast = false;
    private boolean commandsEnabled;
    private boolean isTexturePacksRequired;

    // Gamerule data
    private Map<Gamerule, Object> gamerules;

    // Level data
    private boolean hasBonusChestEnabled;
    private boolean hasStartWithMapEnabled;
    private boolean hasTrustPlayersEnabled;
    private int defaultPlayerPermission = 1;
    private int xboxLiveBroadcastMode = 0;
    private int serverTickRate;
    private boolean hasPlatformBroadcast;
    private int platformBroadcastMode;
    private boolean xboxLiveBroadcastIntent;
    private boolean lockedBehaviour;
    private boolean lockedResource;
    private boolean lockedWorld;

    // World data
    private String levelId;
    private String worldName;
    private String templateName;
    private boolean unknown1;
    private long currentTick;
    private int enchantmentSeed;

    // Block data
    private List<StringShortPair> blockPalette;

    public PacketStartGame() {
        super( PacketRegistry.PACKET_START_GAME );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {
        buffer.writeBytes(this.data);
    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        // Copy first
        int pos = buffer.getReadPosition();
        this.data = new byte[buffer.getRemaining()];
        buffer.readBytes(this.data);
        buffer.setReadPosition(pos);

        this.entityId = buffer.readSignedVarLong().longValue();
        this.runtimeEntityId = buffer.readUnsignedVarLong();
        this.gamemode = buffer.readSignedVarInt();

        this.spawn = new Location( buffer.readLFloat(), buffer.readLFloat(), buffer.readLFloat(), buffer.readLFloat(), buffer.readLFloat() );

        this.seed = buffer.readSignedVarInt();

        short biomeType = buffer.readLShort();
        String biomeName = buffer.readString();
        this.dimension = buffer.readSignedVarInt();

        this.generator = buffer.readSignedVarInt();
        this.worldGamemode = buffer.readSignedVarInt();
        this.difficulty = buffer.readSignedVarInt();

        int spawnX = buffer.readSignedVarInt();
        int spawnY = buffer.readSignedVarInt();
        int spawnZ = buffer.readSignedVarInt();

        this.hasAchievementsDisabled = buffer.readBoolean();
        this.dayCycleStopTime = buffer.readSignedVarInt();
        int eudOffer = buffer.readSignedVarInt();
        this.hasEduModeEnabled = buffer.readBoolean();
        String eduProductID = buffer.readString();
        this.rainLevel = buffer.readLFloat();
        this.lightningLevel = buffer.readLFloat();
        buffer.readBoolean();
        this.isMultiplayerGame = buffer.readBoolean();
        this.hasLANBroadcast = buffer.readBoolean();
        buffer.readSignedVarInt();
        buffer.readSignedVarInt();
        this.commandsEnabled = buffer.readBoolean();
        this.isTexturePacksRequired = buffer.readBoolean();
        this.gamerules = readGamerules( buffer );

        int experiments = buffer.readLInt();
        System.out.println("Amount of experiments: " + experiments);
        buffer.readBoolean();

        this.hasBonusChestEnabled = buffer.readBoolean();
        this.hasStartWithMapEnabled = buffer.readBoolean();

        this.defaultPlayerPermission = buffer.readSignedVarInt();
        this.serverTickRate = buffer.readInt();
        this.lockedBehaviour = buffer.readBoolean();
        this.lockedResource = buffer.readBoolean();
        this.lockedWorld = buffer.readBoolean();
        buffer.readBoolean();
        buffer.readBoolean();
        buffer.readBoolean();
        buffer.readBoolean();

        buffer.readString();

        buffer.readLInt();
        buffer.readLInt();
        buffer.readBoolean();
        if (buffer.readBoolean()) {
            buffer.readBoolean();
        }

        this.levelId = buffer.readString();
        this.worldName = buffer.readString();
        this.templateName = buffer.readString();
        this.unknown1 = buffer.readBoolean();
        buffer.readBoolean();
        this.currentTick = buffer.readLLong();
        this.enchantmentSeed = buffer.readSignedVarInt();

        int amountOfBlocks = buffer.readUnsignedVarInt();
        System.out.println("Amount of blocks: " + amountOfBlocks);
        /*NBTReader readerNoBuffer = new NBTReader(buffer.getBuffer(), ByteOrder.LITTLE_ENDIAN);
        readerNoBuffer.setUseVarint(true);

        try {
            List<Object> compound = readerNoBuffer.parseList();
            AssetAssembler.writeBlockPalette(compound);
        } catch (IOException | AllocationLimitReachedException e) {
            e.printStackTrace();
        }*/

        List<StringShortPair> itemLegacyIds = new ArrayList<>();
        int itemListLength = buffer.readUnsignedVarInt();
        for (int i = 0; i < itemListLength; i++) {
            String itemName = buffer.readString();
            short legacyId = buffer.readLShort();
            boolean itemUsedCompound = buffer.readBoolean();

            System.out.println(itemName + " -> " + legacyId);

            itemLegacyIds.add(new StringShortPair(itemName, legacyId));
        }

        AssetAssembler.writeLegacyItems(itemLegacyIds);
        System.out.println(itemListLength);
    }

}
