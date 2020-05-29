/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Deck: Houses the main logic for a playback deck
 */
package com.stereodustparticles.console.deck;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import com.stereodustparticles.console.SDPConsole2;
import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.ables.Loadable;
import com.stereodustparticles.console.cache.CachedAudio;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventListener;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.library.Library;
import com.stereodustparticles.console.library.LibraryEntry;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.multi.MultiConsole;
import com.stereodustparticles.console.playlist.Playlist;
import com.stereodustparticles.console.playlist.PlaylistEntry;
import com.stereodustparticles.console.pref.Prefs;

public class Deck implements Loadable {
	// Instance variables
	private AudioInputStream audio = null;
	private SourceDataLine line = null;
	private boolean playing = false;
	private boolean poofed = false;
	private ReadThread reader;
	private WriteThread writer;
	private CircularByteBuffer ringBuffer = null;
	private float volume = 0.0f; // + or - dB
	private URL audioFile = null;
	private int deckNum;
	private int confirmedDuration = -1;
	private String title = null;
	private String artist = null;
	private boolean closing = false;
	private boolean ready = false;
	private boolean snpWaiting = false;
	private int requestID = 0;
	
	// TODO make number seconds of buffer adjustable?
	private static final int BUFFER_SIZE_SEC = 5;
	
	public Deck(int deckNum) {
		this.deckNum = deckNum;
		
		// Initialize the read/write threads
		reader = new ReadThread(this);
		writer = new WriteThread(this);
		
		// Register the necessary event listeners to take input from the UI
		EventBus.registerListener(EventType.DECK_PLAY_PRESSED, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( eventMatters(e) && ((Integer)e.getParams()[0]).intValue() == deckNum ) {
					ready = false;
					playButton();
				}
			}
			
		});
		
		EventBus.registerListener(EventType.DECK_CUE_PRESSED, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( eventMatters(e) && ((Integer)e.getParams()[0]).intValue() == deckNum ) {
					cue();
				}
			}
			
		});
		
		EventBus.registerListener(EventType.DECK_REQUEST_LOAD, (e) -> {
			if ( eventMatters(e) ) {
				DeckLoadRequest req = (DeckLoadRequest)e.getParams()[0];
				if ( req.getDeckNum() == deckNum ) {
					Utils.runInBackground(() -> {
						try {
							// Check if we have a valid duration in the request
							// If not, set confirmedDuration to 0 so the loader will calculate it
							// Otherwise, set confirmedDuration to the known duration
							if ( req.getDuration() == -1 ) {
								confirmedDuration = 0;
							}
							else {
								confirmedDuration = req.getDuration();
							}
							
							URL toLoad = LibraryManager.getLibraryForName(req.getLibrary()).getURLFromPath(req.getLocation());
							CachedAudio.cacheAndLoad(toLoad, this);
						}
						catch (Exception e1) {
							onLoadError(e1);
							return;
						}
						
						artist = req.getArtist();
						title = req.getTitle();
						requestID = req.getRequestID();
					});
				}
			}
		});
		
		EventBus.registerListener(EventType.DECK_VOLUME_ADJUST, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( eventMatters(e) ) {
					Object[] params = e.getParams();
					
					if ( ((Integer)params[0]).intValue() == deckNum ) {
						float newVolume = ((Float)params[1]).floatValue();
						
						if ( newVolume != volume ) {
							setVolume(newVolume);
						}
					}
				}
			}
			
		});
		
		EventBus.registerListener(EventType.SNP_TRIGGER, (e) -> {
			if ( Decks.snpIsEnabled() ) { // This check may not strictly be necessary, but it may prevent some stupidity somewhere...
				int deckOrig = ((Integer)e.getParams()[0]).intValue();
				if ( deckOrig != deckNum ) {
					if ( ready ) {
						ready = false;
						playButton();
					}
					else {
						snpWaiting = true;
					}
				}
			}
		});
	}
	
	// Return the ring buffer
	protected CircularByteBuffer getRingBuffer() {
		return ringBuffer;
	}
	
	// Check if the specified event applies to us
	private boolean eventMatters(Event ev) {
		char mcMode = MultiConsole.getDeckMode();
		if ( mcMode == 'M' ) {
			return true;
		}
		else if ( mcMode == 'A' && ev.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) ) {
			return true;
		}
		else {
			return false;
		}
	}
	
	// Load a file into this Deck
	public void load(URL file) {
		// If there is a file playing on this deck, the user has not done their One Job(TM).  Return.
		if ( playing ) {
			return;
		}
		
		// If there was a previously loaded file, close its stream and line
		if ( line != null ) {
			synchronized (line) {
				line.close();
			}
		}
		if ( audio != null ) {
			try {
				audio.close();
			}
			catch (IOException e) {
				// If something goes wrong, throw an error event, but try to continue (unless SnP is enabled)
				if ( Decks.snpIsEnabled() ) {
					autoLoad();
					return;
				}
				else {
					EventBus.fireEvent(new Event(EventType.DECK_LOAD_ERROR, e, deckNum));
				}
			}
			System.gc(); // For safe measure
		}
		
		// Load the file and set up decoding
		// (From http://www.javalobby.org/java/forums/t18465.html)
		AudioInputStream in;
		try {
			in = AudioSystem.getAudioInputStream(file);
		} catch (Exception e) {
			onLoadError(e);
			return;
		}
		
		AudioFormat baseFormat = in.getFormat();
		AudioFormat decodedFormat = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED, // Encoding to use
			baseFormat.getSampleRate(),	  // sample rate (same as base format)
			16,			// sample size in bits (force to 16, maybe make this adjustable eventually?)
			baseFormat.getChannels(),	  // # of Channels
			baseFormat.getChannels()*2,	  // Frame Size
			baseFormat.getSampleRate(),	  // Frame Rate
			false				  // Is Big Endian? (No)
		);
		
		audio = AudioSystem.getAudioInputStream(decodedFormat, in);
		
		// Get the output line (using the method below)
		try {
			line = getLine(decodedFormat);
		}
		catch (LineUnavailableException e) {
			onLoadError(e);
			return;
		}
		
		// Set initial output volume
		setVolume(volume);
		
		// Store the new file name
		audioFile = file;
		
		// Check if the request contains a valid duration
		// If not, calculate one and be ready to tell the control pane (via the Deck Ready event)
		if ( confirmedDuration == 0 ) {
			// Open the audio file using JAudiotagger
			// Since JAudioTagger only takes Files, we need to use the mild hack from https://community.oracle.com/blogs/kohsuke/2007/04/25/how-convert-javaneturl-javaiofile
			File f;
			AudioFile af;
			
			try {
			  f = new File(file.toURI());
			}
			catch (URISyntaxException e) {
			  f = new File(file.getPath());
			}
			
			try {
				af = AudioFileIO.read(f);
				
				AudioHeader header = af.getAudioHeader();
				
				// Get duration (in tenths of seconds)
				confirmedDuration = (int)(header.getPreciseTrackLength() * 10.0);
			}
			catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e1) {
				e1.printStackTrace();
			}
		}
		
		// Prepare the ring buffer and read/write threads
		if ( ringBuffer != null ) {
			try {
				// Force pending writes to the bugger to stop by closing the output stream
				ringBuffer.getOutputStream().close();
				
				// And, for safe measure
				ringBuffer.getInputStream().close();
			}
			catch (IOException e) {
				// Should not happen
				e.printStackTrace();
			}
		}
		
		// Calculate ring buffer size
		AudioFormat format = audio.getFormat();
		int ringBufferSize = ((int)format.getSampleRate() * BUFFER_SIZE_SEC) * (format.getSampleSizeInBits() / 8) * format.getChannels();
		
		ringBuffer = new CircularByteBuffer(ringBufferSize, true);
		
		synchronized (reader) {
			reader.setSource(audio, confirmedDuration);
			reader.notifyAll();
		}
		
		writer.setLine(line);
		
		poofed = false;
	}
	
	// Start/stop playback
	public void playButton() {
		// If there's actually a file loaded...
		if ( ! poofed && line != null && audio != null ) {
			// If we aren't currently playing...
			if ( ! playing ) {
				// Start playing
				synchronized (writer) {
					playing = true;
					writer.notifyAll();
				}
				
				// Mark track played in Playlist Manager
				if ( title != null ) {
					EventBus.fireEvent(new Event(EventType.PLAYLIST_MARK_PLAYED, new PlaylistEntry(artist, title, false, 0, requestID)));
				}
			}
			else {
				// Tell the write thread to stop playback
				playing = false;
			}
		}
	}
	
	// From http://www.javazoom.net/mp3spi/documents.html: Get a line for a given format
	private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
	  SourceDataLine res = null;
	  DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
	  res = (SourceDataLine) AudioSystem.getLine(info);
	  res.open(audioFormat);
	  return res;
	}
	
	// Adjust the playback volume (gain)
	public void setVolume(float newVolume) {
		volume = newVolume;
		
		if ( line != null ) {
			FloatControl volumeCtrl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
			volumeCtrl.setValue(volume);
		}
	}
	
	// Re-cue the current song (a.k.a. reload the file)
	public void cue() {
		if ( audioFile != null ) {
			load(audioFile);
		}
	}
	
	// Return whether or not we're (supposed to be) playing
	public boolean isPlaying() {
		return playing;
	}
	
	// Return whether or not we've been told to close
	public boolean isClosing() {
		return closing;
	}
	
	// Return whether or not this deck is ready to play
	public boolean isReady() {
		return ready;
	}
	
	// Return our deck number
	public int getNumber() {
		return deckNum;
	}
	
	// Clean up
	public void cleanUp() {
		closing = true;
		reader.interrupt();
		writer.interrupt();
		try {
			if ( ringBuffer != null ) {
				ringBuffer.getInputStream().close();
				ringBuffer.getOutputStream().close();
			}
			if ( line != null ) {
				line.close();
			}
			if ( audio != null ) {
				audio.close();
			}
		}
		catch (IOException e) {
			// Shouldn't happen
			e.printStackTrace();
		}
	}
	
	// Notify this deck that EOF has been reached
	protected void notifyEOF() {
		writer.notifyEOF();
	}
	
	// Automatically load a track from... somewhere
	// Used for Stream 'n' Poop(TM)
	public void autoLoad() {
		LibraryEntry nextTrack = Playlist.nextLoadableTentative();
		
		while ( nextTrack == null ) {
			// If there is no tentative track to load, pick something randomly
			// I can see this leading to all kinds of fun, given I'm picking from a random library too!
			List<Library> libList = LibraryManager.getSnPLibraries();
			int chosenLibIndex = SDPConsole2.random.nextInt(libList.size());
			try {
				nextTrack = libList.get(chosenLibIndex).pickRandomTrack();
				if ( Playlist.alreadyPlayed(nextTrack) ) {
					nextTrack = null;
					continue;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		
		DeckLoadRequest req = new DeckLoadRequest(nextTrack.getTitle(), nextTrack.getArtist(), nextTrack.getDuration(), nextTrack.getLibraryName(), LibraryManager.getLibraryForName(nextTrack.getLibraryName()).getPathInLibrary(nextTrack), deckNum);
		EventBus.fireEvent(new Event(EventType.DECK_REQUEST_LOAD, req));
	}
	
	// Handle a load error
	private void onLoadError(Exception e) {
		if ( Decks.snpIsEnabled() ) {
			autoLoad();
		}
		else {
			EventBus.fireEvent(new Event(EventType.DECK_LOAD_ERROR, e, deckNum));
		}
	}
	
	// Notify this deck that, after EOF, all buffered audio has played out
	protected void notifyAudioPoof() {
		playing = false;
		poofed = true;
	}
	
	// Notify this deck that it's ready to play
	// You'd think a deck ought to know this for itself, but no...
	protected void notifyReady() {
		if ( snpWaiting ) {
			snpWaiting = false;
			playButton();
		}
		else {
			ready = true;
		}
	}
}