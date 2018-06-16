/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Soundboard: Houses the backend of the soundboard
 */
package com.stereodustparticles.console.soundboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.multi.MultiConsole;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;

public class Soundboard {
	private static Spot[][] board;
	private static File layoutFile = null;
	private static boolean fileIsValid = true;
	private static SpotLoadRequest queuedReq = null;
	private static int rows;
	private static int cols;
	private static boolean clearing = false;
	
	// Initialize the soundboard (called from main)
	public static void init() {
		// Initialize the backend only if we're not in slave mode
		if ( MultiConsole.getSoundboardMode() != 'S' ) {
			// Get soundboard dimensions
			rows = Prefs.loadInt(Prefs.SOUNDBOARD_ROWS);
			cols = Prefs.loadInt(Prefs.SOUNDBOARD_COLS);
			
			// Initialize the soundboard
			board = new Spot[rows][cols];
			
			// Populate the board with Spot objects
			for ( int i = 0; i < rows; i++ ) {
				for ( int j = 0; j < cols; j++ ) {
					board[i][j] = new Spot(i, j);
				}
			}
		}
		
		// Register event listeners
		EventBus.registerListener(EventType.SPOT_REQUEST_LOAD, (e) -> {
			queuedReq = (SpotLoadRequest) e.getParams()[0];
		});
	}
	
	// Get the queued load request
	public static SpotLoadRequest getQueuedRequest() {
		return queuedReq;
	}
	
	// Invalidate the current layout (i.e. flag it as needing to be saved)
	public static void invalidateLayout() {
		if ( ! clearing ) fileIsValid = false;
	}
	
	// Check if the current layout file is valid (i.e. no unsaved changes)
	public static boolean layoutIsValid() {
		return fileIsValid;
	}
	
	// Return the current layout file
	public static File getCurrentLayout() {
		return layoutFile;
	}
	
	// Write the soundboard layout to a file
	public static void writeLayout(File to) throws IOException {
		Utils.writeCSV2D(board, to);
		layoutFile = to;
		fileIsValid = true;
	}
	
	// Write the soundboard layout to the last file
	public static void writeLayout() throws IOException {
		// Idiot-proofing
		if ( layoutFile == null ) return;
		
		Utils.writeCSV2D(board, layoutFile);
		fileIsValid = true;
	}
	
	// Load the specified layout file (BL2 format)
	// Return a boolean indicating whether or not all spots could be loaded (i.e. board was big enough), at least at the local console
	public static boolean loadLayout(File from) throws IOException {
		clearing = true;
		
		// Track if we have gone over the board size while loading
		boolean over = false;
		
		// Read the file
		FileReader fr = new FileReader(from);
		BufferedReader br = new BufferedReader(fr);
		String row;
		while ( (row = br.readLine()) != null ) {
			
			// Parse CSV (well, "CSV"...)
			String[] parts = row.split("\\|");
			int r = Integer.parseInt(parts[0]);
			int c = Integer.parseInt(parts[1]);
			
			// Will this spot fit on our board?
			if ( r >= rows || c >= cols ) {
				over = true;
			}
			
			// Load the spot (or clear, if necessary)
			// Do this even if it won't fit, in case a remote console has a larger board
			if ( parts[2].equals(" ") ) {
				EventBus.fireEvent(new Event(EventType.SPOT_CLEAR, r, c));
			}
			else {
				EventBus.fireEvent(new Event(EventType.SPOT_POS_CHOSEN, new SpotLoadRequest(parts[2], parts[3], parts[4]), r, c));
				try {
					if ( parts[5].equals(" ") ) {
						EventBus.fireEvent(new Event(EventType.SPOT_COLOR_CHANGE, "", r, c));
					}
					else {
						EventBus.fireEvent(new Event(EventType.SPOT_COLOR_CHANGE, parts[5], r, c));
					}
				}
				catch ( ArrayIndexOutOfBoundsException e ) { // i.e. catch my earlier bug...
					EventBus.fireEvent(new Event(EventType.SPOT_COLOR_CHANGE, "", r, c));
				}
			}
		}
		
		// Close things up
		br.close();
		fr.close();
		
		// Set new layout file and mark valid
		// Run later to avoid race conditions
		Platform.runLater(() -> {
			layoutFile = from;
			fileIsValid = true;
			clearing = false;
		});
		
		return (! over);
	}
	
	// Load the specified layout file (old BLA format) (TODO)
	public static void loadLegacyLayout(File from) {
		Microwave.showError("I Haven't Gotten That Far Yet...", "Loading of MCC 1.x layout files (legacy BLA format) is not yet supported.  Go complain to IfYouLikeGoodIdeas about it.");
	}
	
	// Clear all spots
	public static void clearAll() {
		layoutFile = null;
		clearing = true;
		
		for ( int i = 0; i < rows; i++ ) {
			for ( int j = 0; j < cols; j++ ) {
				EventBus.fireEvent(new Event(EventType.SPOT_CLEAR, i, j));
			}
		}
		
		// Run later to avoid race condition
		Platform.runLater(() -> {
			fileIsValid = true;
			clearing = false;
		});
	}
}
