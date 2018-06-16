package com.stereodustparticles.console.library;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.KeyNotFoundException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

import com.stereodustparticles.console.Utils;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * FSLibraryEntry: This defines a library entry read from the file system
 */

public class FSLibraryEntry implements LibraryEntry {

	private static final long serialVersionUID = 1L;
	
	private File location;
	private String libName;
	private String title = null;
	private String artist = null;
	private int duration = -1;
	
	public FSLibraryEntry(File location, String libName) {
		this.location = location;
		this.libName = libName;
	}
	
	@Override
	public String toString() {
		if ( location.isDirectory() ) {
			return "[" + location.getName() + "]";
		}
		else {
			return location.getName();
		}
	}
	
	@Override
	public URL getLocationAsURL() {
		try {
			return location.toURI().toURL();
		} catch (MalformedURLException e) {
			// TODO real error message?
			System.err.println("Someone only had ONE JOB:");
			e.printStackTrace();
			return null;
		}
	}
	
	// Get the location of this entry as a file
	public File getLocationAsFile() {
		return location;
	}
	
	// Load the track's metadata with JAudioTagger, if this has not been done already
	private void loadMetadata() {
		if ( title != null ) {
			return;
		}
		
		if ( location.isDirectory() ) {
			this.title = location.getName();
			this.artist = "(Directory)";
			return;
		}
		
		// Open the audio file using JAudiotagger
		AudioFile af;
		try {
			af = AudioFileIO.read(location);
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e1) {
			e1.printStackTrace();
			
			// Set metadata to values that indicate an unrecognized file
			this.title = location.getName();
			this.artist = "(Unrecognized File!)";
			
			return;
		}
		AudioHeader header = af.getAudioHeader();
		Tag tag = af.getTag();
		
		// Check if we actually have a tag to parse
		if ( tag != null ) {
			// Looks like we do have a tag - let's parse it
			
			// Get artist
			// Use generic data if nothing gets returned
			try {
				String artist = tag.getFirst(FieldKey.ARTIST);
				if ( artist.equals("") ) {
					throw new KeyNotFoundException();
				}
				this.artist = artist;
			}
			catch ( KeyNotFoundException e ) {
				this.artist = "(Unknown Artist)";
			}
			
			// Get title
			// Use generic data (filename) if nothing gets returned
			try {
				String title = tag.getFirst(FieldKey.TITLE);
				if ( title.equals("") ) {
					throw new KeyNotFoundException();
				}
				this.title = title;
			}
			catch ( KeyNotFoundException e ) {
				this.title = location.getName();
			}
		}
		else {
			// We have no tag present - use generic title/artist
			this.artist = "(Unknown Artist)";
			this.title = location.getName();
		}
		
		// Get duration (in tenths of seconds)
		duration = (int)(header.getPreciseTrackLength() * 10.0);
	}

	@Override
	public boolean isDir() {
		return location.isDirectory();
	}

	@Override
	public String getTitle() {
		loadMetadata();
		return title;
	}

	@Override
	public String getArtist() {
		loadMetadata();
		return artist;
	}

	@Override
	public int getDuration() {
		loadMetadata();
		return duration;
	}

	@Override
	public String getDurationPreview() {
		loadMetadata();
		
		// If no duration is available, return a dash
		if ( duration == -1 ) {
			return "-";
		}
		
		return Utils.tenthsToStringShort(duration);
	}

	@Override
	public String getLibraryName() {
		return libName;
	}
	
	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof FSLibraryEntry ) {
			FSLibraryEntry other = (FSLibraryEntry) obj;
			return other.getLocationAsFile().equals(location);
		}
		else {
			return false;
		}
	}

	@Override
	public String toMRSData() {
		String title;
		String artist;
		String album;
		String year;
		
		AudioFile af;
		try {
			af = AudioFileIO.read(location);
		} catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException e1) {
			e1.printStackTrace();
			return null;
		}
		
		Tag tag = af.getTag();
		
		// Check if we actually have a tag to parse
		if ( tag != null ) {
			// Looks like we do have a tag - let's parse it
			
			// Get artist
			// Use generic data if nothing gets returned
			try {
				artist = tag.getFirst(FieldKey.ARTIST);
				if ( artist.equals("") ) {
					artist = "Unknown Artist";
				}
			}
			catch ( KeyNotFoundException e ) {
				artist = "Unknown Artist";
			}
			
			// Get title
			// Use generic data (filename) if nothing gets returned
			try {
				title = tag.getFirst(FieldKey.TITLE);
				if ( title.equals("") ) {
					throw new KeyNotFoundException();
				}
			}
			catch ( KeyNotFoundException e ) {
				title = location.getName();
			}
			
			// Get album
			// Use generic data if nothing gets returned
			try {
				album = tag.getFirst(FieldKey.ALBUM);
				if ( album.equals("") ) {
					album = "Unknown Album";
				}
			}
			catch ( KeyNotFoundException e ) {
				album = "Unknown Album";
			}
			
			// Get year
			// Use generic data if nothing gets returned
			try {
				year = tag.getFirst(FieldKey.YEAR);
				if ( year.equals("") ) {
					year = "Unknown Year";
				}
			}
			catch ( KeyNotFoundException e ) {
				year = "Unknown Year";
			}
			
			// Sanitize as best we can
			artist = Utils.sanitizeForMRS(artist);
			title = Utils.sanitizeForMRS(title);
			album = Utils.sanitizeForMRS(album);
			year = Utils.sanitizeForMRS(year);
			
			// Build hidden metadata string
			String meta = "MCC:" + libName + ":" + LibraryManager.getLibraryForName(libName).getPathInLibrary(this);
			
			// Output the final string
			return artist + "|" + title + "|" + album + "|" + year + "|" + meta;
		}
		else {
			return null;
		}
	}

	@Override
	public boolean isLoadable() {
		// TODO return false for non-audio files
		return true;
	}

}