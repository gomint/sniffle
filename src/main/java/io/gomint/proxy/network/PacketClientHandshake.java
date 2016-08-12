package io.gomint.proxy.network;

import io.gomint.jraknet.PacketBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class PacketClientHandshake extends Packet {
	
	private int    protocolVersion;
	private String jwt;
	private byte[] skin;
	
	public PacketClientHandshake() {
		super( PacketRegistry.PACKET_CLIENT_HANDSHAKE );
	}
	
	public int getProtocolVersion() {
		return this.protocolVersion;
	}
	
	public String getJwt() {
		return this.jwt;
	}
	
	public byte[] getSkin() {
		return this.skin;
	}
	
	public void setProtocolVersion( int protocolVersion ) {
		this.protocolVersion = protocolVersion;
	}
	
	public void setJwt( String jwt ) {
		this.jwt = jwt;
	}
	
	public void setSkin( byte[] skin ) {
		this.skin = skin;
	}
	
	@Override
	public void serialize( PacketBuffer buffer ) {
		buffer.writeInt( this.protocolVersion );
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		try ( DeflaterOutputStream deflater = new DeflaterOutputStream( bout, new Deflater( Deflater.BEST_COMPRESSION ) ) ) {
			byte[] jwtBytes = this.jwt.getBytes( StandardCharsets.UTF_8 );
			this.writeIntLE( deflater, jwtBytes.length );
			deflater.write( jwtBytes );
			
			this.writeIntLE( deflater, this.skin.length );
			deflater.write( this.skin );
			
			deflater.finish();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		
		byte[] compressed = bout.toByteArray();
		
		buffer.writeInt( compressed.length );
		buffer.writeBytes( compressed );
	}
	
	@Override
	public void deserialize( PacketBuffer buffer ) {
		this.protocolVersion = buffer.readInt();
		
		buffer = this.extractCompressedPayload( buffer );
		if ( buffer == null ) {
			throw new IllegalArgumentException( "Failed to decode compressed handshake payload" );
		}
		
		int jwtLength = this.extractIntLE( buffer );
		this.jwt = new String( buffer.getBuffer(), buffer.getBufferOffset() + buffer.getPosition(), jwtLength, StandardCharsets.UTF_8 );
		buffer.skip( jwtLength );
		
		int skinLength = this.extractIntLE( buffer );
		this.skin = new byte[skinLength];
		buffer.readBytes( this.skin );
	}
	
	private PacketBuffer extractCompressedPayload( PacketBuffer buffer ) {
		int payloadLength = buffer.readInt();
		if ( buffer.getRemaining() < payloadLength ) {
			return null;
		}
		
		byte[] intermediate = new byte[256];
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		try {
			Inflater inflater = new Inflater();
			inflater.setInput( buffer.getBuffer(), buffer.getBufferOffset() + buffer.getPosition(), payloadLength );
			while ( !inflater.finished() ) {
				int decompressed = inflater.inflate( intermediate );
				bout.write( intermediate, 0, decompressed );
			}
		} catch ( DataFormatException e ) {
			e.printStackTrace();
			return null;
		}
		
		// Skip payload in original buffer as decoding algorithm depends on automatic incrementation of buffer's position:
		buffer.skip( payloadLength );
		
		return new PacketBuffer( bout.toByteArray(), 0 );
	}
	
	private int extractIntLE( PacketBuffer buffer ) {
		return ( buffer.readByte() & 0xFF ) | ( buffer.readByte() & 0xFF ) << 8 | ( buffer.readByte() & 0xFF ) << 16 | ( buffer.readByte() & 0xFF ) << 24;
	}
	
	private void writeIntLE( OutputStream out, int value ) throws IOException {
		out.write( value & 0xFF );
		out.write( ( value & 0xFF00 ) >> 8 );
		out.write( ( value & 0xFF0000 ) >> 16 );
		out.write( ( value & 0xFF000000 ) >> 24 );
	}
	
}
