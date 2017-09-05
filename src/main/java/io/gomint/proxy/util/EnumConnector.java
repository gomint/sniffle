package io.gomint.proxy.util;

/**
 * @author geNAZt
 * @version 1.0
 */
public class EnumConnector<E1 extends Enum<E1>, E2 extends Enum<E2>> {

    private Class<E1> enumOne;
    private Class<E2> enumTwo;

    private E1[] enumOneConstants;
    private E2[] enumTwoConstants;

    public EnumConnector( Class<E1> enumOne, Class<E2> enumTwo ) {
        this.enumOne = enumOne;
        this.enumTwo = enumTwo;

        // Get enum constants of both
        this.enumOneConstants = this.enumOne.getEnumConstants();
        this.enumTwoConstants = this.enumTwo.getEnumConstants();
    }

    public E2 convert( E1 e1 ) {
        // Null safety
        if ( e1 == null ) return null;

        String name = e1.name();

        for ( E2 enumTwoConstant : this.enumTwoConstants ) {
            if ( enumTwoConstant.name().equals( name ) ) {
                return enumTwoConstant;
            }
        }

        return null;
    }

    public E1 revert( E2 e2 ) {
        // Null safety
        if ( e2 == null ) return null;

        String name = e2.name();

        for ( E1 enumOneConstant : this.enumOneConstants ) {
            if ( enumOneConstant.name().equals( name ) ) {
                return enumOneConstant;
            }
        }

        return null;
    }

}
