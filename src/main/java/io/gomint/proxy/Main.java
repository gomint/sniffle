package io.gomint.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public class Main {

	private static final Logger logger = LoggerFactory.getLogger( Main.class );

	private static String ip;
	private static int port = 19132;
	private static int listenPort = 19132;

	/**
	 * Main entry point for the application.
	 *
	 * @param args Command-Line arguments passed to the application
	 */
	public static void main( String[] args ) {
		if ( !parseCommandLineArguments( args ) ) {
			return;
		}

		Proxy proxy = new Proxy( ip, port );
		try {
			proxy.bind( "0.0.0.0", listenPort );
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Parses command line arguments and sets the respective fields of this class.
	 *
	 * @param args Command-Line arguments passed to the application
	 * @return Returns true on success or false if any obligatory arguments are missing
	 */
	private static boolean parseCommandLineArguments( String[] args ) {
		for ( int i = 0; i < args.length; ++i ) {
			if ( args[i].startsWith( "--ip" ) ) {
				String[] split = args[i].split( "=" );
				if ( split.length == 2 ) {
					ip = split[1];
				} else {
					logger.error( "Malformed '--ip' command line option: Please specify actual IP value" );
					return false;
				}
			} else if ( args[i].startsWith( "--port" ) ) {
				String[] split = args[i].split( "=" );
				if ( split.length == 2 ) {
					try {
						port = Integer.valueOf( split[1] );
						if ( port < 0 || port > 65535 ) {
							throw new NumberFormatException();
						}
					} catch ( NumberFormatException e ) {
						logger.error( "Malformed '--port' command line option: Please specify valid integer port value" );
						return false;
					}
				} else {
					logger.error( "Malformed '--port' command line option: Please specify actual IP value" );
					return false;
				}
			} else if ( args[i].startsWith( "--lport" ) ) {
				String[] split = args[i].split( "=" );
				if ( split.length == 2 ) {
					try {
						listenPort = Integer.valueOf( split[1] );
						if ( port < 0 || port > 65535 ) {
							throw new NumberFormatException();
						}
					} catch ( NumberFormatException e ) {
						logger.error( "Malformed '--lport' command line option: Please specify valid integer port value" );
						return false;
					}
				} else {
					logger.error( "Malformed '--lport' command line option: Please specify actual IP value" );
					return false;
				}
			} else {
				logger.error( "Unknown command line option '" + args[i] + "'" );
				return false;
			}
		}

		if ( ip == null ) {
			logger.error( "Missing obligatory command-line parameter '--ip'" );
			return false;
		}

		return true;
	}

}
