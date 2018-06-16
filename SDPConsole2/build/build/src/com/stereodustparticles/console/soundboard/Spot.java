/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Spot: Houses the underlying logic for each Spot on the soundboard
 */
package com.stereodustparticles.console.soundboard;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.ables.CSVable;
import com.stereodustparticles.console.ables.Loadable;
import com.stereodustparticles.console.cache.CachedAudio;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventListener;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.multi.MultiConsole;
import com.stereodustparticles.console.pref.Prefs;

public class Spot implements Loadable, CSVable {
	private Clip clip;
	private int row;
	private int col;
	private LineListener startStop;
	private String title;
	private String library;
	private String location;
	private String color = "";
	
	public Spot(int row, int col) {
		this.row = row;
		this.col = col;
		this.clip = null;
		
		// Convert Java Sound line events into event bus events (we'll register this listener later)
		startStop = new LineListener() {
			public void update(LineEvent e) {
				if ( e.getType() == LineEvent.Type.START ) {
					EventBus.fireEvent(new Event(EventType.SPOT_PLAYBACK_STARTED, row, col));
				}
				else if ( e.getType() == LineEvent.Type.STOP ) {
					EventBus.fireEvent(new Event(EventType.SPOT_PLAYBACK_STOPPED, row, col));
				}
			}
		};
		
		// Register the various listeners on the event bus
		// On error, fire a load error event and return
		EventBus.registerListener(EventType.SPOT_POS_CHOSEN, (e) -> {
			if ( MultiConsole.getSoundboardMode() == 'A' && ! e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) ) {
				return;
			}
			
			int cRow = ((Integer)e.getParams()[1]).intValue();
			int cCol = ((Integer)e.getParams()[2]).intValue();
			
			// If this event applies to us...
			if ( row == cRow && col == cCol ) {
				Soundboard.invalidateLayout();
				
				Utils.runInBackground(() -> {
					SpotLoadRequest req = (SpotLoadRequest)e.getParams()[0];
					
					// Extract the URL for the spot to load, then cache and load it
					URL toLoad = null;
					try {
						toLoad = LibraryManager.getLibraryForName(req.getLibrary()).getURLFromPath(req.getLocation());
						CachedAudio.cacheAndLoad(toLoad, this);
						
						title = req.getTitle();
						library = req.getLibrary();
						location = req.getLocation();
					}
					catch (Exception ex) {
						EventBus.fireEvent(new Event(EventType.SPOT_LOAD_ERROR, ex, row, col));
						return;
					}
				});
			}
		});
		
		EventBus.registerListener(EventType.SPOT_CLEAR, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( MultiConsole.getSoundboardMode() == 'A' && ! e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) ) {
					return;
				}
				
				int cRow = ((Integer)e.getParams()[0]).intValue();
				int cCol = ((Integer)e.getParams()[1]).intValue();
				
				// If this event applies to us...
				if ( row == cRow && col == cCol ) {
				
					// If there is an actual clip, stop/close it, then get rid of it
					if ( clip != null ) {
						clip.stop();
						clip.removeLineListener(startStop);
						clip.close();
						clip = null;
						color = "";
						
						Soundboard.invalidateLayout();
					}
				}
			}
			
		});
		
		EventBus.registerListener(EventType.SPOT_BUTTON_PRESSED, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( MultiConsole.getSoundboardMode() == 'A' && ! e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) ) {
					return;
				}
				
				int cRow = ((Integer)e.getParams()[0]).intValue();
				int cCol = ((Integer)e.getParams()[1]).intValue();
				
				// If this event applies to us...
				if ( row == cRow && col == cCol ) {
				
					// If there is actually a clip loaded...
					if ( clip != null ) {
						// If it's playing, stop it
						if ( clip.isActive() ) {
							clip.stop();
						}
						// If it's not playing, cue it to the beginning and play it
						else {
							clip.setFramePosition(0);
							clip.start();
						}
					}
				}
			}
			
		});
		
		// Track color changes so we can save them to the layout files
		EventBus.registerListener(EventType.SPOT_COLOR_CHANGE, (e) -> {
			if ( MultiConsole.getSoundboardMode() == 'A' && ! e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) ) {
				return;
			}
			
			int cRow = ((Integer)e.getParams()[1]).intValue();
			int cCol = ((Integer)e.getParams()[2]).intValue();
			
			// If this event applies to us...
			if ( row == cRow && col == cCol ) {
				color = (String)e.getParams()[0];
				Soundboard.invalidateLayout();
			}
		});
	}
	
	// Load the spot from the specified URL
	public void load(URL file) {
		// If there's a previous clip in the spot, unload it (remove the event handler and close it)
		if ( clip != null ) {
			clip.stop();
			clip.removeLineListener(startStop);
			clip.close();
		}
		
		// Open the URL
		AudioInputStream in;
		try {
			in = AudioSystem.getAudioInputStream(file);
		}
		catch (UnsupportedAudioFileException | IOException e1) {
			EventBus.fireEvent(new Event(EventType.SPOT_LOAD_ERROR, e1, row, col));
			return;
		}
		
		// Load the file and set up decoding
		// (From http://www.javalobby.org/java/forums/t18465.html)
		AudioFormat baseFormat = in.getFormat();
		AudioFormat decodedFormat = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED, // Encoding to use
			baseFormat.getSampleRate(),	  // sample rate (same as base format)
			16,				  // sample size in bits (thx to Javazoom)
			baseFormat.getChannels(),	  // # of Channels
			baseFormat.getChannels()*2,	  // Frame Size
			baseFormat.getSampleRate(),	  // Frame Rate
			false				  // Is Big Endian? (No)
		);
		AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);
		
		// Set up output and decode/load the file into memory
        DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
        try {
        	clip = (Clip)AudioSystem.getLine(info);
        	clip.open(din);
		}
        catch (LineUnavailableException | IOException e1) {
			EventBus.fireEvent(new Event(EventType.SPOT_LOAD_ERROR, e1, row, col));
			return;
		}
        
        // Register the event handler (to send start/stop events)
        clip.addLineListener(startStop);
        
        // We should be done with the input streams - close them
        try {
        	din.close();
        	in.close();
		}
        catch (IOException e1) {
			EventBus.fireEvent(new Event(EventType.SPOT_LOAD_ERROR, e1, row, col));
			return;
		}
        
        // We're done - tell everyone!
        EventBus.fireEvent(new Event(EventType.SPOT_READY, row, col, (int)(clip.getMicrosecondLength() / 100000)));
	}

	@Override
	public String toCSVRow() {
		// Return spaces if this spot isn't loaded
		if ( clip == null ) {
			return row + "|" + col + "| | | | ";
		}
		
		// Again, we're going to cheat and use pipe characters to avoid comma issues
		if ( ! color.equals("") ) {
			return row + "|" + col + "|" + title + "|" + library + "|" + location + "|" + color;
		}
		else {
			return row + "|" + col + "|" + title + "|" + library + "|" + location + "| ";
		}
	}
}
