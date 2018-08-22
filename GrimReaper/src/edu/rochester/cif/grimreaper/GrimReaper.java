package edu.rochester.cif.grimreaper;

import java.io.IOException;

import edu.rochester.cif.grimreaper.readers.CardReader;
import edu.rochester.cif.grimreaper.readers.ReaderStatus;
import edu.rochester.cif.grimreaper.readers.StatusChangedCallback;
import edu.rochester.cif.grimreaper.readers.elcom.ElcomCardReader;

// GrimReaper - Access controller for the CIF lab
// Written by Ben Ackerman '18 - Summer 2017

// This is the main class for the program, where the startup code resides

public class GrimReaper {

	private static String configPath = "grimreaper.conf";
	
	// I think we all know what this method does...
	public static void main(String[] args) {
		// Parse command-line arguments
		if ( args.length > 0 ) {
			for ( int i = 0; i < args.length; i++ ) {
				switch (args[i]) {
					case "-c":
						// Custom config path
						i++;
						try {
							configPath = args[i];
						}
						catch (ArrayIndexOutOfBoundsException e) {
							System.out.println("(!) -c was specified with no path");
							usage();
						}
						break;
					default:
						System.out.println("(!) Invalid argument \"" + args[i] + "\"");
						usage();
						break;
				}
			}
		}
		
		// Initialize the reader and start things up
		CardReader reader = new ElcomCardReader(); // TODO use dynamic class loading
		reader.registerStatusChangedCallback(new StatusChangedCallback() {
			@Override
			public void statusChanged(ReaderStatus newStatus) {
				// When a change in status occurs, act on the new status
				// Note that the actual state is not stored - only changes are acted upon
				// Any real concept of "state" is maintained by the card reader class
				switch (newStatus) {
					case IDLE:
						System.out.println("Reader is now ready");
						break;
					case CARD_WAITING:
						// TODO put actual checks here
						String id = reader.getID();
						if ( id.equals("9287164951208700000") ) {
							System.out.println("Granted access to " + id);
							reader.grantAccess();
						}
						else {
							System.out.println("Denied access to " + id);
							reader.denyAccess();
						}
						break;
					// TODO send emails in these cases
					case TAMPER:
						System.err.println("(!) Tamper switch has been tripped!");
						break;
					case FORCED_OPEN:
						System.err.println("(!) Door has been forced open!");
						break;
					case LINK_LOST:
						System.err.println("(!) Link to reader has been lost!");
						break;
					case RECOVERED_FROM_POWER_FAILURE:
						System.err.println("(!) The reader has recovered from a power failure!");
						break;
					default:
						break;
				}
			}
		});
		reader.open();
		
		// Add a shutdown hook to close the reader connection when the JVM exits
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					reader.close();
				}
				catch (IOException e) {
					System.err.println("An I/O error occurred while closing the reader connection:");
					e.printStackTrace();
				}
			}
		}));
	}
	
	// Print usage and exit
	public static void usage() {
		System.out.println("Usage: java GrimReaper.class [params], where [params] can be:");
		System.out.println("-c <path> : Read configuration from <path>, rather than ./grimreaper.conf");
		System.exit(10);
	}

}
