/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
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
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * @author BlackyPaw
 * @version 1.0
 */
@Data
@EqualsAndHashCode( callSuper = false )
public class PacketCraftingRecipes extends Packet {

    /**
     * Construct new crafting recipe packet
     */
    public PacketCraftingRecipes() {
        super( PacketRegistry.PACKET_CRAFTING_RECIPES );
    }

    @Override
    public void serialize( PacketBuffer buffer ) {

    }

    @Override
    public void deserialize( PacketBuffer buffer ) {
        AssetAssembler.resetRecipes();

        int count = buffer.readUnsignedVarInt();
        for ( int i = 0; i < count; i++ ) {
            int recipeType = buffer.readSignedVarInt();
            if ( recipeType < 0 ) {
                continue;
            }

            switch ( recipeType ) {
                case 6:
                case 5:
                case 0:
                    // Shapeless
                    String name = buffer.readString();

                    // Read input side
                    int inputCount = buffer.readUnsignedVarInt();

                    ItemStack[] input = new ItemStack[inputCount];
                    for ( int i1 = 0; i1 < inputCount; i1++ ) {
                        input[i1] = Packet.readRecipeIngredient( buffer );
                    }

                    // Read output side
                    int outputCount = buffer.readUnsignedVarInt();
                    ItemStack[] output = new ItemStack[outputCount];
                    for ( int i1 = 0; i1 < outputCount; i1++ ) {
                        output[i1] = Packet.readItemStack( buffer );
                    }

                    // Read uuid
                    UUID uuid = buffer.readUUID();
                    String block = buffer.readString();
                    int prio = buffer.readSignedVarInt();
                    AssetAssembler.addRecipe( name, uuid, (byte) recipeType, input, output, -1, -1, block, prio );
                    break;
                case 7:
                case 1:
                    // Shaped
                    name = buffer.readString();

                    // Read size of shape
                    int width = buffer.readSignedVarInt();
                    int height = buffer.readSignedVarInt();

                    // Read input side
                    input = new ItemStack[width * height];
                    for ( int i1 = 0; i1 < input.length; i1++ ) {
                        input[i1] = Packet.readRecipeIngredient( buffer );
                    }

                    // Read output side
                    outputCount = buffer.readUnsignedVarInt();
                    output = new ItemStack[outputCount];
                    for ( int i1 = 0; i1 < outputCount; i1++ ) {
                        output[i1] = Packet.readItemStack( buffer );
                    }

                    // Read uuid
                    uuid = buffer.readUUID();
                    block = buffer.readString();
                    prio = buffer.readSignedVarInt();
                    AssetAssembler.addRecipe( name, uuid, (byte) recipeType, input, output, width, height, block, prio );
                    break;

                case 3:
                    // Smelting with metadata

                    int id = buffer.readSignedVarInt();
                    short data = (short) buffer.readSignedVarInt();
                    ItemStack result = Packet.readItemStack( buffer );

                    block = buffer.readString();

                    AssetAssembler.addRecipe( null, null, (byte) 2, new ItemStack[]{ new ItemStack( id, data, 1 ) }, new ItemStack[]{ result }, -1, -1, block, 0 );
                    break;

                case 2:
                    // Smelting without metadata

                    id = buffer.readSignedVarInt();
                    result = Packet.readItemStack( buffer );

                    block = buffer.readString();

                    AssetAssembler.addRecipe( null, null, (byte) 2, new ItemStack[]{ new ItemStack( id, (short) 0, 1 ) }, new ItemStack[]{ result }, -1, -1, block, 0 );
                    break;

                case 4:
                    UUID uuid1 = buffer.readUUID();
                    // TODO: What is a multi recipe?
                    break;
                default:
                    System.out.println( "New recipe type: " + recipeType );
            }
        }

        for(int i = 0, count1 = buffer.readUnsignedVarInt(); i < count1; ++i){
            int input = buffer.readSignedVarInt();
            int ingredient = buffer.readSignedVarInt();
            int output = buffer.readSignedVarInt();

            AssetAssembler.addRecipe( null, null, (byte) 101, new ItemStack[]{ new ItemStack( input, (short) 0, 1 ), new ItemStack( ingredient, (short) 0, 1) }, new ItemStack[]{ new ItemStack(output, (short)0, 1) }, -1, -1, null, 0 );
        }

        for(int i = 0, count1 = buffer.readUnsignedVarInt(); i < count1; ++i){
            int input = buffer.readSignedVarInt();
            int ingredient = buffer.readSignedVarInt();
            int output = buffer.readSignedVarInt();

            AssetAssembler.addRecipe( null, null, (byte) 102, new ItemStack[]{ new ItemStack( input, (short) 0, 1 ), new ItemStack( ingredient, (short) 0, 1) }, new ItemStack[]{ new ItemStack(output, (short)0, 1) }, -1, -1, null, 0 );
        }

        buffer.readBoolean();
        System.out.println("Read all recipes");
    }

}
