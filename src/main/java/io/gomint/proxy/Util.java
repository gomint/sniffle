package io.gomint.proxy;

/**
 * @author geNAZt
 * @version 1.0
 */
public class Util {

    public static String toHexString( byte[] data ) {
        StringBuilder stringBuilder = new StringBuilder();

        for ( byte b : data ) {
            stringBuilder.append( "0x" ).append( Integer.toHexString( b & 0xFF ) ).append( " " );
        }

        return stringBuilder.toString();
    }

}
