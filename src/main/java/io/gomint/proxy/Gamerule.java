/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public final class Gamerule<T> {

    public static final Gamerule<Boolean> COMMANDBLOCK_OUTPUT = new Gamerule<>( "commandBlockOutput", Boolean.class );
    public static final Gamerule<Boolean> DO_DAYLIGHT_CYCLE = new Gamerule<>( "doDaylightCycle", Boolean.class );
    public static final Gamerule<Boolean> DO_FIRE_TICK = new Gamerule<>( "doFireTick", Boolean.class );
    public static final Gamerule<Boolean> DO_MOB_LOOT = new Gamerule<>( "doMobLoot", Boolean.class );
    public static final Gamerule<Boolean> DO_MOB_SPAWNING = new Gamerule<>( "doMobSpawning", Boolean.class );
    public static final Gamerule<Boolean> DO_TILE_DROPS = new Gamerule<>( "doTileDrops", Boolean.class );
    public static final Gamerule<Boolean> KEEP_INVENTORY = new Gamerule<>( "keepInventory", Boolean.class );
    public static final Gamerule<Boolean> LOG_ADMIN_COMMANDS = new Gamerule<>( "logAdminCommands", Boolean.class );
    public static final Gamerule<Boolean> MOB_GRIEFING = new Gamerule<>( "mobGriefing", Boolean.class );
    public static final Gamerule<Boolean> NATURAL_REGENERATION = new Gamerule<>( "naturalRegeneration", Boolean.class );
    public static final Gamerule<Integer> RANDOM_TICK_SPEED = new Gamerule<>( "randomTickSpeed", Integer.class );
    public static final Gamerule<Boolean> SEND_COMMAND_FEEDBACK = new Gamerule<>( "sendCommandFeedback", Boolean.class );
    public static final Gamerule<Boolean> SHOW_DEATH_MESSAGES = new Gamerule<>( "showDeathMessages", Boolean.class );

    private static final Map<String, Gamerule> gamerulesByNbtNames = new HashMap<>();
    private final String nbtName;
    private final Class<T> valueType;
    private Method instantiator;

    private Gamerule( final String nbtName, final Class<T> valueType ) {
        this.nbtName = nbtName;
        this.valueType = valueType;

        try {
            this.instantiator = this.valueType.getDeclaredMethod( "valueOf", new Class<?>[]{ String.class } );
            this.instantiator.setAccessible( true );
        } catch ( NoSuchMethodException e ) {
            e.printStackTrace();
            this.instantiator = null;
        }

        gamerulesByNbtNames.put( nbtName.toLowerCase(), this );
    }

    /**
     * Tries to find a gamerule given its NBT name.
     *
     * @param nbtName The NBT name of the gamerule
     * @return The gamerule on success or null if no gamerule according to the NBT name was found
     */
    public static Gamerule getByNbtName( String nbtName ) {
        return gamerulesByNbtNames.get( nbtName.toLowerCase() );
    }

    /**
     * Gets the name of the gamerule as it appears inside NBT files such as level.dat
     *
     * @return The name of the gamerule as it appears inside NBT files
     */
    public String getNbtName() {
        return this.nbtName;
    }

    /**
     * Gets the type of value this gamerule expects.
     *
     * @return The type of value this gamerule expects
     */
    public Class<?> getValueType() {
        return this.valueType;
    }

    /**
     * Creates a value of this gamerule's value type given the string representation of the value.
     *
     * @param value The value as a string
     * @return The value of the gamerule in its appropriate type
     */
    public T createValueFromString( String value ) {
        if ( this.instantiator == null ) {
            return null;
        }

        try {
            return this.valueType.cast( this.instantiator.invoke( null, new Object[]{ value } ) );
        } catch ( IllegalAccessException | InvocationTargetException e ) {
            e.printStackTrace();
        }

        return null;
    }

}
