package io.gomint.proxy;

import io.gomint.jraknet.Connection;
import io.gomint.jraknet.PacketBuffer;
import io.gomint.proxy.packet.PacketLogin;
import io.gomint.proxy.packet.Protocol;

/**
 * @author geNAZt
 * @version 1.0
 */
public class KnownPacketHandler implements ProxiedPacketHandler {

    private final Connection connection;
    private static CryptoContext cryptoContext = null;

    public KnownPacketHandler( Connection connection ) {
        this.connection = connection;
    }

    @Override
    public byte[] handlePacket( byte[] packetData, boolean fromServer, boolean batch ) {
        if ( cryptoContext != null ) {
            System.out.println( "Decrypt...." );
            return Crypto.decrypt( cryptoContext, packetData );
        }

        PacketBuffer packetBuffer = new PacketBuffer( packetData, 0 );
        byte packetId = packetBuffer.readByte();
        if ( packetId == Protocol.LOGIN_PACKET ) {
            PacketLogin packetLogin = new PacketLogin();
            packetLogin.deserialize( packetBuffer );
            System.out.println( packetLogin.getUUID() );

            packetLogin.setLoginJWT( Crypto.getLoginJWT( packetLogin.getUserName(), packetLogin.getUUID() ) );
            PacketBuffer packetBuffer1 = new PacketBuffer( 8192 );
            packetBuffer1.writeByte( (byte) 0xFE );
            packetBuffer1.writeByte( (byte) 0x01 );
            packetLogin.serialize( packetBuffer1 );

            byte[] packetData1 = new byte[packetBuffer1.getPosition()];
            System.arraycopy( packetBuffer1.getBuffer(), 0, packetData1, 0, packetBuffer1.getPosition() );
            connection.send( packetData1 );

            return null;
        }

        if ( packetId == 0x03 ) {   // Server public key exchange
            String serverKey = packetBuffer.readString();

            byte[] iv = new byte[packetBuffer.readShort()];
            packetBuffer.readBytes( iv );

            cryptoContext = Crypto.createContext( serverKey, iv );
            System.out.println( "Got crypto context: " + cryptoContext );

            return null;
        }

        return packetData;
    }

}
