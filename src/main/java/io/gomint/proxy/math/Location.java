/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.proxy.math;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * <p>
 * A Location defines a point a world with three coordinates relative to a
 * specific world it is placed in.
 * </p>
 *
 * @author Digot
 * @author geNAZt
 * @author BlackyPaw
 * @version 1.2
 */
@EqualsAndHashCode( callSuper = true )
@ToString
public class Location extends Vector implements Cloneable {

    @Getter
    @Setter
    private float yaw;
    @Getter
    @Setter
    private float headYaw;
    @Getter
    @Setter
    private float pitch;
    
    public Location( float x, float y, float z ) {
        super( x, y, z );
        
    }

    public Location( Vector vector ) {
        super( vector.getX(), vector.getY(), vector.getZ() );
        
    }

    public Location( float x, float y, float z, float yaw, float pitch ) {
        super( x, y, z );
        
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Location( Vector vector, float yaw, float pitch ) {
        super( vector.getX(), vector.getY(), vector.getZ() );
        
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Location( float x, float y, float z, float headYaw, float yaw, float pitch ) {
        this( x, y, z, yaw, pitch );
        this.headYaw = headYaw;
    }

    @Override
    public Location add( float x, float y, float z ) {
        super.add( x, y, z );
        return this;
    }

    @Override
    public Location add( Vector v ) {
        super.add( v );
        return this;
    }

    @Override
    public Location subtract( float x, float y, float z ) {
        super.subtract( x, y, z );
        return this;
    }

    @Override
    public Location subtract( Vector v ) {
        super.subtract( v );
        return this;
    }

    @Override
    public Location multiply( float x, float y, float z ) {
        super.multiply( x, y, z );
        return this;
    }

    @Override
    public Location multiply( Vector v ) {
        super.multiply( v );
        return this;
    }

    @Override
    public Location divide( float x, float y, float z ) {
        super.divide( x, y, z );
        return this;
    }

    @Override
    public Location divide( Vector v ) {
        super.divide( v );
        return this;
    }

    @Override
    public Location multiply( float scalar ) {
        super.multiply( scalar );
        return this;
    }

    @Override
    public String toString() {
        return String.format( "[x=%.3f, y=%.3f, z=%.3f, yaw=%.3f, pitch=%.3f]", x, y, z, yaw, pitch );
    }

    @Override
    public Location clone() {
        Location location = (Location) super.clone();
        location.yaw = this.yaw;
        location.pitch = this.pitch;
        return location;
    }

    public Vector toVector() {
        return new Vector( this.x, this.y, this.z );
    }

}