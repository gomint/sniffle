package io.gomint.proxy.asset;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.inventory.ItemStack;
import io.gomint.proxy.util.DumpUtil;
import io.gomint.proxy.util.StringShortPair;
import io.gomint.taglib.AllocationLimitReachedException;
import io.gomint.taglib.NBTReader;
import io.gomint.taglib.NBTTagCompound;

import io.gomint.taglib.NBTWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author geNAZt
 * @version 1.0
 */
public class AssetAssembler {

  private static final NBTTagCompound compound = new NBTTagCompound("");

  static {
    File file = new File("assets.dat");
    if (file.exists()) {
      file.delete();
    }

    try {
      readBlockPalette();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void readBlockPalette() throws IOException {
    File nbtPalette = new File("palette.nbt");
    byte[] data = Files.readAllBytes(nbtPalette.toPath());
    NBTReader nbtReader = new NBTReader(Unpooled.wrappedBuffer(data), ByteOrder.BIG_ENDIAN);
    nbtReader.setUseVarint(true);

    List<Object> blockPalette = new ArrayList<>();

    while (true) {
      try {
        NBTTagCompound compound = nbtReader.parse();
        DumpUtil.dumpNBTCompund(compound);
        blockPalette.add(compound);
      } catch (AllocationLimitReachedException | IOException e) {
        List<Object> nbtTags = compound.getList("blockPalette", true);
        nbtTags.clear();
        nbtTags.addAll(blockPalette);
        return;
      }
    }
  }

  public static void writeToFile() {
    ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
    NBTWriter nbtWriter = new NBTWriter(buf, ByteOrder.BIG_ENDIAN);

    try {
      nbtWriter.write(compound);
    } catch (IOException e) {
      e.printStackTrace();
    }

    try (FileOutputStream out = new FileOutputStream(new File("assets.dat"))) {
      byte[] data = new byte[buf.readableBytes()];
      buf.readBytes(data);
      out.write(data);
    } catch (IOException e) {
      e.printStackTrace();
    }

    buf.release();
  }

  public static synchronized void addRecipe(String name, UUID uuid, byte type, ItemStack[] in, ItemStack[] out,
      int width, int height, String block, int prio) {
    List<Object> recipes = compound.getList("recipes", true);

    NBTTagCompound recipeCompound = new NBTTagCompound("");

    if (name != null) {
      recipeCompound.addValue("name", name);
    }

    if (block != null) {
      recipeCompound.addValue("block", block);
    }

    recipeCompound.addValue("prio", prio);
    recipeCompound.addValue("type", type);

    List<byte[]> input = new ArrayList<>();
    for (ItemStack itemStack : in) {
      input.add(serializeItem(itemStack));
    }

    recipeCompound.addValue("i", input);

    List<byte[]> output = new ArrayList<>();
    for (ItemStack itemStack : out) {
      output.add(serializeItem(itemStack));
    }

    recipeCompound.addValue("o", output);

    if (uuid != null) {
      recipeCompound.addValue("u", uuid.toString());
    }

    if (width != -1 && height != -1) {
      recipeCompound.addValue("w", width);
      recipeCompound.addValue("h", height);
    }

    recipes.add(recipeCompound);
  }

  public static synchronized void writeCreativeInventory(ItemStack[] items) {
    List<Object> nbtTags = compound.getList("creativeInventory", true);
    nbtTags.clear();

    for (ItemStack item : items) {
      byte[] itemData = serializeItem(item);
      nbtTags.add(itemData);
    }
  }

  public static synchronized void writeLegacyItems(List<StringShortPair> itemLegacyIds) {
    List<Object> nbtTags = compound.getList("itemLegacyIDs", true);
    nbtTags.clear();

    for (StringShortPair itemLegacyId : itemLegacyIds) {
      NBTTagCompound l = new NBTTagCompound("");
      l.addValue("name", itemLegacyId.getBlockId());
      l.addValue("id", itemLegacyId.getData());
      nbtTags.add(l);
    }
  }

  public static synchronized void writeBlockPalette(List<Object> blockPalette) {
    List<Object> nbtTags = compound.getList("blockPalette", true);
    nbtTags.clear();
    nbtTags.addAll(blockPalette);
  }

  private static byte[] serializeItem(ItemStack itemStack) {
    PacketBuffer buffer = new PacketBuffer(2);
    if (itemStack == null) {
      System.out.println("Nulled item");
    }

    buffer.writeShort((short) itemStack.getMaterial());
    buffer.writeByte(itemStack.getAmount());
    buffer.writeShort(itemStack.getData());

    if (itemStack.getNbtData() != null) {
      ByteBuf nbtBuf = PooledByteBufAllocator.DEFAULT.directBuffer();
      try {
        itemStack.getNbtData().writeTo(nbtBuf, false, ByteOrder.BIG_ENDIAN);
      } catch (IOException e) {

      }

      buffer.writeShort((short) nbtBuf.readableBytes());
      buffer.writeBytes(nbtBuf);
    } else {
      buffer.writeShort((short) 0);
    }

    byte[] data = new byte[buffer.getRemaining()];
    buffer.readBytes(data);
    buffer.release();
    return data;
  }

  public static synchronized void writeBiomeDefinition(NBTTagCompound nbt) {
    compound.addValue("biomeDefinitions", nbt.deepClone("biomeDefinitions"));
  }

}
