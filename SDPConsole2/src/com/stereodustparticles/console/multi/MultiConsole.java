/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * MultiConsole: This class defines the base of the Multi-Console function
 */
package com.stereodustparticles.console.multi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;

public class MultiConsole {
	// Holds the set of active connections
	private static List<EventStream> connTable;
	
	// The "swerver" resouces, if inbound TCP is enabled
	private static Thread swerverThread = null;
	private static ServerSocket swerver = null;
	
	// A place to keep the outbound link as it connects
	private static Link outboundLink = null;
	
	// The serial link, if applicable
	private static Link serialLink = null;
	
	// Flag to mark when we're closing up (to suppress bogus exceptions)
	private static boolean closing = false;
	
	// Called on Console startup
	public static void init() {
		// Initialize the connection table
		connTable = new ArrayList<EventStream>();
		
		// If inbound TCP is enabled...
		if ( Prefs.loadBoolean(Prefs.MC_INBOUND_ENABLE) ) {
			swerverThread = new Thread(() -> {
				try {
					swerver = new ServerSocket(Prefs.loadInt(Prefs.MC_INBOUND_PORT));
				}
				catch (IOException e) {
					// We weren't able to open the swerver.  Bitch to the user about it.
					Platform.runLater(() -> Microwave.showException("Multi-Console Error", "The specified inbound port could not be opened.  Check your settings, then try again.\n\nIf the problem persists, something likely needs to take flight.", e));
					return;
				}
				
				while (true) {
					try {
						Socket micSock = swerver.accept(); // You know I had to.  It was listening after all!
						connTable.add(new EventStream(new IPLink(micSock)));
					}
					catch (IOException e) {
						if ( ! closing ) Platform.runLater(() -> Microwave.showException("Multi-Console Error", "An error occurred while listening for incoming connections.  Check your settings and network connectivity, then try again.\n\nIf the problem persists, throw any related equipment across the room.", e));
						return;
					}
				}
			});
			swerverThread.start();
		}
		
		// If outbound TCP is enabled...
		if ( Prefs.loadBoolean(Prefs.MC_OUTBOUND_ENABLE) ) {
			Utils.runInBackground(() -> {
				outboundLink = new IPLink(Prefs.loadString(Prefs.MC_OUTBOUND_IP), Prefs.loadInt(Prefs.MC_OUTBOUND_PORT));
				try {
					outboundLink.connect();
				}
				catch (InterruptedException e) {
					return;
				}
				connTable.add(new EventStream(outboundLink));
			});
		}
		
		// If serial is enabled...
		if ( Prefs.loadBoolean(Prefs.MC_SERIAL_ENABLE) ) {
			Utils.runInBackground(() -> {
				serialLink = new SerialLink(Prefs.loadString(Prefs.MC_SERIAL_PORT));
				try {
					serialLink.connect();
				}
				catch (InterruptedException e) {
					return;
				}
				connTable.add(new EventStream(serialLink));
			});
		}
	}
	
	// Retrieve the mode of the soundboard
	public static char getSoundboardMode() {
		return Prefs.loadString(Prefs.MC_SOUNDBOARD_MODE).charAt(0);
	}
	
	// Retrieve the mode of the decks
	public static char getDeckMode() {
		return Prefs.loadString(Prefs.MC_DECK_MODE).charAt(0);
	}
	
	// Retrieve the mode of the playlist
	public static char getPlaylistMode() {
		return Prefs.loadString(Prefs.MC_PLAYLIST_MODE).charAt(0);
	}
	
	// All 3 of the following methods will return the local console's name if Slave is not selected
	
	// Retrieve the master for the soundboard
	public static String getSoundboardMaster() {
		if ( getSoundboardMode() == 'S' ) {
			return Prefs.loadString(Prefs.MC_SOUNDBOARD_MODE).substring(1);
		}
		else {
			return Prefs.loadString(Prefs.MC_IDENTITY);
		}
	}
	
	// Retrieve the master for the decks
	public static String getDeckMaster() {
		if ( getDeckMode() == 'S' ) {
			return Prefs.loadString(Prefs.MC_DECK_MODE).substring(1);
		}
		else {
			return Prefs.loadString(Prefs.MC_IDENTITY);
		}
	}
	
	// Retrieve the mode of the playlist
	public static String getPlaylistMaster() {
		if ( getPlaylistMode() == 'S' ) {
			return Prefs.loadString(Prefs.MC_PLAYLIST_MODE).substring(1);
		}
		else {
			return Prefs.loadString(Prefs.MC_IDENTITY);
		}
	}
	
	// Display the class mismatch error message
	public static void displayClassMismatchError(String fName) {
		Platform.runLater(() -> Microwave.showError("Multi-Console Error", "A class mismatch occurred while communicating with the console at " + fName + ".  Are you sure the two consoles are of the same version?"));
	}
	
	// Generate the status text for all connections
	public static String getStatus() {
		if ( connTable.isEmpty() ) {
			return "There are no active connections.";
		}
		
		String stat = "Connected Consoles:\n";
		for ( EventStream es : connTable ) {
			stat += es.getFriendlyName() + ": " + es.getFriendlyStatus() + "\n";
		}
		
		stat += "\nConfigured Connections:\n";
		
		if ( outboundLink != null ) {
			stat += "Outbound IP Link To " + outboundLink.getFriendlyName() + ": " + outboundLink.getFriendlyStatus() + "\n";
		}
		
		if ( serialLink != null ) {
			stat += "Serial Link On " + serialLink.getFriendlyName() + ": " + serialLink.getFriendlyStatus() + "\n";
		}
		
		if ( swerver != null ) {
			stat += "Listening for incoming IP connections";
		}
		
		return stat;
	}
	
	// Forward an event to all other consoles
	public static void forwardEvent(Event e, EventStream origin) {
		for ( EventStream es : connTable ) {
			if ( es != origin ) {
				es.sendEvent(e);
			}
		}
	}
	
	// What do you think this does?
	public static void cleanUp() {
		closing = true;
		
		for ( EventStream es : connTable ) {
			try {
				es.close();
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if ( outboundLink != null ) {
			try {
				outboundLink.close();
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if ( swerverThread != null ) {
			swerverThread.interrupt();
		}
		
		if ( swerver != null ) {
			try {
				swerver.close();
			}
			catch (IOException e) {
				// TODO should we really be swallowing this?
				e.printStackTrace();
			}
		}
		
		if ( serialLink != null ) {
			try {
				serialLink.close();
			}
			catch (Exception e) {
				// TODO should we really be swallowing this?
				e.printStackTrace();
			}
		}
	}
	
	// Handle an event stream with a dead link
	public static void bringOutYourDead(EventStream es) {
		try {
			es.close();
		}
		catch (Exception e) {
			System.err.println("Failed to close dead EventStream: " + e.getMessage());
		}
		
		connTable.remove(es);
		
		// Was this an outbound link?  If so, reconnect it.
		if ( es.getLink() == outboundLink ) {
			outboundLink = new IPLink(Prefs.loadString(Prefs.MC_OUTBOUND_IP), Prefs.loadInt(Prefs.MC_OUTBOUND_PORT));
			try {
				outboundLink.connect();
			}
			catch (InterruptedException e) {
				return;
			}
			connTable.add(new EventStream(outboundLink));
		}
	}
}
