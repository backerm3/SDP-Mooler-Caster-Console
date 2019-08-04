/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Playlist: Defines the structure that houses the current playlist
 */
package com.stereodustparticles.console.playlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.error.MRSException;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.library.LibraryEntry;
import com.stereodustparticles.console.mrs.MRSIntegration;
import com.stereodustparticles.console.multi.MultiConsole;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Callback;

public class Playlist {
	private static ObservableList<PlaylistEntry> playlist;
	private static int listPointer = 0;
	private static Timer saveTimer = new Timer(true);
	private static TimerTask saveTimerAction;
	private static File pll = null;
	
	// Return true if we need to act on the given event
	private static boolean eventMatters(Event ev) {
		return (MultiConsole.getPlaylistMode() != 'A' || ev.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)));
	}
	
	// Initialize the playlist subsystem
	@SuppressWarnings("unchecked")
	public static void init() {
		// Create playlist structure
		playlist = FXCollections.observableArrayList(
			new Callback<PlaylistEntry, Observable[]>() {
	            @Override
	            public Observable[] call(PlaylistEntry param) {
	                return new Observable[]{
                        param.titleProperty(),
                        param.artistProperty(),
                        param.flagsProperty(),
                        param.tentativeProperty()
	                };
	            }
	        }
		);
		
		// Create a PLL file
		pll = makeNewPLL();
		
		EventBus.registerListener(EventType.PLAYLIST_ADD, (e) -> {
			if ( eventMatters(e) ) {
				disarmAutoBackup();
				
				PlaylistEntry entry = (PlaylistEntry)e.getParams()[0];
				
				PlaylistEntry existing = findInList(entry, false);
				if ( existing != null ) {
					if ( existing.isTentative() && entry.isTentative() ) {
						return;
					}
					//else if ( ! existing.isTentative() && entry.isTentative() ) {
					else {
						if (! Microwave.confirmAction("Did you have ONE JOB?", "It looks like you've played that song already!  Do you really want to play it again, or did you only have ONE JOB?")) {
							return;
						}
					}
				}
				
				if ( entry.isTentative() ) {
					playlist.add(entry);
				}
				else {
					playlist.add(listPointer, entry);
					listPointer++;
				}
				
				armAutoBackup();
			}
		});
		
		EventBus.registerListener(EventType.PLAYLIST_MARK_PLAYED, (e) -> {
			if ( eventMatters(e) ) {
				disarmAutoBackup();
				
				PlaylistEntry entry = (PlaylistEntry)e.getParams()[0];
				
				if ( entry == null ) return;
				
				PlaylistEntry found = findInList(entry, true);
				
				// If either the specified entry or the found entry has a request ID tied to it,
				// break off another thread to mark that request played in the MRS
				int foundID = entry.getRequestID();
				if ( foundID == 0 && found != null ) {
					foundID = found.getRequestID();
				}
				if ( foundID > 0 ) {
					final int id = foundID; // To make Java shut up
					
					Utils.runInBackground(() -> {
						try {
							MRSIntegration.markIDPlayed(id);
						}
						catch (MRSException e1) {
							Platform.runLater(() -> Microwave.showError("The MRS Doesn't Like You", "Marking the request played failed with the following error:\n\n" + e1.getMessage() + "\n\nSend your MRS on a trip to the pool, then try again."));
						}
					});
				}
				
				// If the specified entry is in the list, set its tentative flag to false
				// If it's not in the list, add it (after setting its tentative flag to false)
				if ( found == null ) {
					entry.setTentative(false);
					playlist.add(listPointer, entry);
					listPointer++;
					
					return;
				}
				else {
					found.setTentative(false);
				}
				
				// If the affected track is located past the location of the playlist pointer
				// (presumably the point corresponding to "now"), move it up to the location
				// of the pointer, and increment the pointer
				int cIndex = playlist.lastIndexOf(found);
				if ( cIndex > listPointer ) {
					playlist.remove(cIndex);
					playlist.add(listPointer, found);
					listPointer++;
				}
				
				// If the current index equals the list pointer, just increment the pointer
				else if ( cIndex == listPointer ) listPointer++;
				
				armAutoBackup();
			}
		});
		
		EventBus.registerListener(EventType.PLAYLIST_EDIT, (e) -> {
			if ( eventMatters(e) ) {
				disarmAutoBackup();
				
				int index = (int)e.getParams()[0];
				
				// Idiot proofing
				if ( index < 0 || index > playlist.size() ) return;
				
				PlaylistEntry oldEntry = playlist.get(index);
				PlaylistEntry newEntry = (PlaylistEntry)e.getParams()[1];
				
				// Figure out where to place the new entry based on changes in tentative flag
				int newIndex;
				if ( oldEntry.isTentative() && ! newEntry.isTentative() ) {
					newIndex = listPointer;
					listPointer++;
				}
				else if ( ! oldEntry.isTentative() && newEntry.isTentative() ) {
					listPointer--;
					newIndex = listPointer;
				}
				else {
					newIndex = index;
				}
				
				playlist.remove(index);
				playlist.add(newIndex, newEntry);
				
				armAutoBackup();
			}
		});
		
		EventBus.registerListener(EventType.PLAYLIST_DELETE, (e) -> {
			if ( eventMatters(e) ) {
				disarmAutoBackup();
				
				int index = (int)e.getParams()[0];
				playlist.remove(index);
				
				// If the delete occurred before the pointer, decrement the pointer
				if ( index < listPointer ) listPointer--;
				
				armAutoBackup();
			}
		});
		
		EventBus.registerListener(EventType.PLAYLIST_MOVE_UP, (e) -> {
			if ( eventMatters(e) ) {
				disarmAutoBackup();
				
				int index = (int)e.getParams()[0];
				if ( index <= 0 || ( playlist.get(index).isTentative() && index == listPointer ) ) {
					return;
				}
				PlaylistEntry target = playlist.get(index - 1);
				playlist.remove(index - 1);
				playlist.add(index, target);
				
				armAutoBackup();
			}
		});
		
		EventBus.registerListener(EventType.PLAYLIST_MOVE_DOWN, (e) -> {
			if ( eventMatters(e) ) {
				disarmAutoBackup();
				
				int index = (int)e.getParams()[0];
				if ( index + 1 >= playlist.size() || ( ! playlist.get(index).isTentative() && index == listPointer ) ) {
					return;
				}
				PlaylistEntry target = playlist.get(index + 1);
				playlist.remove(index + 1);
				playlist.add(index, target);
				
				armAutoBackup();
			}
		});
		
		EventBus.registerListener(EventType.PLAYLIST_CLEAR, (e) -> {
			if ( eventMatters(e) ) {
				disarmAutoBackup();
				playlist.clear();
				listPointer = 0;
				pll = makeNewPLL(); // Start a new PLL on playlist clear
			}
		});
		
		EventBus.registerListener(EventType.PLAYLIST_SYNC, (e) -> {
			if ( MultiConsole.getPlaylistMode() == 'S' && e.getOriginator().equals(MultiConsole.getPlaylistMaster()) ) {
				synchronized (playlist) {
					playlist.setAll((List<PlaylistEntry>) e.getParams()[0]);
				}
			}
		});
	}
	
	// Return the observable list
	public static ObservableList<PlaylistEntry> getObservableList() {
		return playlist;
	}
	
	// Find and return a particular entry in the playlist
	// Return null if none found
	// If onlyTentative is specified, we will return the first tentative entry, unless no tentative entry is found
	private static PlaylistEntry findInList(PlaylistEntry in, boolean onlyTentative) {
		PlaylistEntry out = null;
		
		for ( PlaylistEntry check : playlist ) {
			if ( check.equals(in) ) {
				out = check;
				if ( onlyTentative && ! check.isTentative() ) {
					continue;
				}
				else {
					break;
				}
			}
		}
		
		return out;
	}
	
	// Determine if a particular track has been played already
	public static boolean alreadyPlayed(LibraryEntry track) {
		PlaylistEntry check = new PlaylistEntry(track, false, 0);
		return (findInList(check, false) != null);
	}
	
	// Return the total number of tentative tracks in the playlist
	// We'll assume that they're all below the list pointer AS IS RIGHT AND PROPER...
	/*public static int tentativeTrackCount() {
		return playlist.size() - listPointer;
	}*/
	
	// Find and return the next tentative track that can be automatically loaded to a deck
	// Return null if none found
	public static LibraryEntry nextLoadableTentative() {
		LibraryEntry ret = null;
		PlaylistEntry entry = null;
		int i = 0;
		while ( ret == null ) {
			if ( listPointer + i >= playlist.size() ) {
				return null;
			}
			
			entry = playlist.get(listPointer + i);
			if ( ! entry.autoLoaded ) {
				ret = entry.getLocation();
			}
			i++;
		}
		entry.autoLoaded = true;
		return ret;
	}
	
	// Export the playlist to the specified text file
	public static void export(File to) throws IOException {
		final String SYSTEM_LINE_END = System.getProperty("line.separator");
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		
		try {
			// Create the file if it doesn't exist yet
			if ( ! to.exists() ) {
				to.createNewFile();
			}

			fw = new FileWriter(to.getAbsoluteFile(), true);
			bw = new BufferedWriter(fw);

			for ( PlaylistEntry entry : playlist ) {
				bw.write(entry.toString() + SYSTEM_LINE_END);
			}
		}
		finally {
			try {
				if (bw != null) {
					bw.close();
				}
				if (fw != null) {
					fw.close();
				}
			}
			catch (IOException e) {
				System.err.println("I'm having really bad luck today - I couldn't even close the log stream!");
				e.printStackTrace();
			}
		}
	}
	
	// Make a new PLL (playlist log) File object
	private static File makeNewPLL() {
		int index = 1;
		String prefix = "SDP_Playlist_" + new SimpleDateFormat("yyyy_MM_dd").format(new Date()) + "_session";
		File pll = null;
		while ( pll == null || pll.exists() ) {
			pll = new File(System.getProperty("java.io.tmpdir"), prefix + index + ".pll");
			index++;
		}
		return pll;
	}
	
	// Arm the auto-backup timer
	private static void armAutoBackup() {
		// (Isn't it my luck that this is about the only place I can't use a lambda...)
		saveTimerAction = new TimerTask() {

			@Override
			public void run() {
				try {
					Utils.writeCSV(playlist, pll);
				}
				catch (IOException e) {
					Platform.runLater(() -> Microwave.showException("Error Writing Playlist Backup", "An error occurred while backing up the current playlist state to disk.  See details below for more information.\n\nIf the problem persists, a defenestration is likely in order.", e));
				}
				
				// If we're in MC Master mode, fire a Playlist Sync event too
				if ( MultiConsole.getPlaylistMode() == 'M' ) {
					EventBus.fireEvent(new Event(EventType.PLAYLIST_SYNC, new ArrayList<PlaylistEntry>(playlist)));
				}
			}
			
		};
		
		saveTimer.schedule(saveTimerAction, Prefs.loadInt(Prefs.PLAYLIST_SAVE_TIMEOUT) * 1000);
	}
	
	// Disarm the auto-backup timer
	private static void disarmAutoBackup() {
		if ( saveTimerAction != null ) {
			saveTimerAction.cancel();
		}
	}
	
	// Restore the playlist from the specified PLL file
	public static void restoreFromPLL(File pll) throws IOException {
		// Clear the existing list
		EventBus.fireEvent(new Event(EventType.PLAYLIST_CLEAR, 0));
		
		// Read the file
		FileReader fr = new FileReader(pll);
		BufferedReader br = new BufferedReader(fr);
		String row;
		while ( (row = br.readLine()) != null ) {
			EventBus.fireEvent(new Event(EventType.PLAYLIST_ADD, PlaylistEntry.fromCSVRow(row)));
		}
		
		// Close things up
		br.close();
		fr.close();
		
		// Reset the list pointer to the correct position
		resetPointer();
	}

	// Recalculate the list pointer position
	private static void resetPointer() {
		listPointer = 0;
		for ( PlaylistEntry entry : playlist ) {
			if ( entry.isTentative() ) {
				break;
			}
			else {
				listPointer++;
			}
		}
	}
}
