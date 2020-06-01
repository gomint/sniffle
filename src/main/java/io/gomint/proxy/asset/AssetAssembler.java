package io.gomint.proxy.asset;

import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.inventory.ItemStack;
import io.gomint.proxy.util.StringShortPair;
import io.gomint.taglib.AllocationLimitReachedException;
import io.gomint.taglib.NBTTagCompound;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author geNAZt
 * @version 1.0
 */
public class AssetAssembler {

    private static NBTTagCompound readFromFile() {
        File file = new File( "assets.dat" );
        try {
            FileInputStream fileInputStream = new FileInputStream( file );
            return NBTTagCompound.readFrom( fileInputStream, true, ByteOrder.BIG_ENDIAN );
        } catch ( IOException e ) {
            return new NBTTagCompound( "assets" );
        } catch (AllocationLimitReachedException e) {
            e.printStackTrace();
            return new NBTTagCompound( "assets" );
        }
    }

    private static void writeToFile( NBTTagCompound compound ) {
        try {
            compound.writeTo( new File( "assets.dat" ), true, ByteOrder.BIG_ENDIAN );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public static void addRecipe( UUID uuid, byte type, ItemStack[] in, ItemStack[] out, int width, int height ) {
        NBTTagCompound compound = readFromFile();

        List<Object> recipes = compound.getList( "recipes", true );

        NBTTagCompound recipeCompound = new NBTTagCompound( "" );
        recipeCompound.addValue( "type", type );

        List<byte[]> input = new ArrayList<>();
        for ( ItemStack itemStack : in ) {
            input.add( serializeItem( itemStack ) );
        }

        recipeCompound.addValue( "i", input );

        List<byte[]> output = new ArrayList<>();
        for ( ItemStack itemStack : out ) {
            output.add( serializeItem( itemStack ) );
        }

        recipeCompound.addValue( "o", output );

        if ( uuid != null ) {
            recipeCompound.addValue( "u", uuid.toString() );
        }

        if ( width != -1 && height != -1 ) {
            recipeCompound.addValue( "w", width );
            recipeCompound.addValue( "h", height );
        }

        recipes.add( recipeCompound );
        writeToFile( compound );
    }

    public static void writeCreativeInventory( ItemStack[] items ) {
        NBTTagCompound compound = readFromFile();

        List<Object> nbtTags = compound.getList( "creativeInventory", true );
        nbtTags.clear();

        for ( ItemStack item : items ) {
            byte[] itemData = serializeItem( item );
            nbtTags.add( itemData );
        }

        writeToFile( compound );
    }

    public static void writeBlockPalette( List<StringShortPair> blockPalette ) {
        NBTTagCompound compound = readFromFile();

        List<Object> nbtTags = compound.getList( "blockPalette", true );
        nbtTags.clear();

        for ( StringShortPair pair : blockPalette ) {
            NBTTagCompound blockCompound = new NBTTagCompound( "" );
            blockCompound.addValue( "id", pair.getBlockId() );
            blockCompound.addValue( "data", pair.getData() );
            nbtTags.add( blockCompound );
        }

        writeToFile( compound );
    }

    private static byte[] serializeItem( ItemStack itemStack ) {
        PacketBuffer buffer = new PacketBuffer( 2 );
        if ( itemStack == null ) {
            System.out.println( "Nulled item" );
        }

        buffer.writeShort( (short) itemStack.getMaterial() );
        buffer.writeByte( itemStack.getAmount() );
        buffer.writeShort( itemStack.getData() );

        if ( itemStack.getNbtData() != null ) {
            ByteArrayOutputStream bon = new ByteArrayOutputStream();
            try {
                itemStack.getNbtData().writeTo( bon, false, ByteOrder.BIG_ENDIAN );
            } catch ( IOException e ) {

            }

            byte[] data = bon.toByteArray();
            buffer.writeShort( (short) data.length );
            buffer.writeBytes( data );
        } else {
            buffer.writeShort( (short) 0 );
        }

        return buffer.getBuffer();
    }

    public static void resetRecipes() {
        NBTTagCompound compound = readFromFile();
        List<Object> recipes = compound.getList( "recipes", false );
        if ( recipes != null ) {
            recipes.clear();
            writeToFile(compound);
        }
    }

}
