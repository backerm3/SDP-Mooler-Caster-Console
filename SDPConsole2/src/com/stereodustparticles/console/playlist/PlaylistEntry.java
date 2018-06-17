/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * PlaylistEntry: Defines the structure of an entry in the playlist
 */
package com.stereodustparticles.console.playlist;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import com.stereodustparticles.console.ables.CSVable;
import com.stereodustparticles.console.library.Library;
import com.stereodustparticles.console.library.LibraryEntry;
import com.stereodustparticles.console.library.LibraryManager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PlaylistEntry implements CSVable, Serializable {
	private static final long serialVersionUID = 1L;
	
	private transient StringProperty artist = new SimpleStringProperty();
	private transient StringProperty title = new SimpleStringProperty();
	private LibraryEntry location;
	private transient BooleanProperty isTentative = new SimpleBooleanProperty();
	private transient IntegerProperty flags = new SimpleIntegerProperty();
	private transient int requestID = 0;
	boolean autoLoaded = false;
	
	public PlaylistEntry(LibraryEntry location, boolean isTentative, int flags) {
		this.artist.setValue(location.getArtist());
		this.title.setValue(location.getTitle());
		this.location = location;
		this.isTentative.setValue(isTentative);
		this.flags.setValue(flags);
	}
	
	public PlaylistEntry(LibraryEntry location, boolean isTentative, int flags, int requestID) {
		this(location, isTentative, flags);
		this.requestID = requestID;
	}
	
	public PlaylistEntry(String artist, String title, boolean isTentative, int flags) {
		this.artist.setValue(artist);
		this.title.setValue(title);
		this.location = null;
		this.isTentative.setValue(isTentative);
		this.flags.setValue(flags);
	}
	
	public PlaylistEntry(String artist, String title, boolean isTentative, int flags, int requestID) {
		this(artist, title, isTentative, flags);
		this.requestID = requestID;
	}
	
	public PlaylistEntry() {
		// Do nothing - this constructor is only used for CSV decoding and deserialization
	}
	
	public PlaylistEntry(String artist, String title, LibraryEntry location, boolean isTentative, int flags) {
		this.artist.setValue(artist);
		this.title.setValue(title);
		this.location = location;
		this.isTentative.setValue(isTentative);
		this.flags.setValue(flags);
	}

	public String getArtist() {
		return artist.getValue();
	}
	
	public StringProperty artistProperty() {
		return artist;
	}
	
	public void setArtist(String artist) {
		this.artist.setValue(artist);
	}
	
	public String getTitle() {
		return title.getValue();
	}
	
	public StringProperty titleProperty() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title.setValue(title);
	}
	
	public LibraryEntry getLocation() {
		return location;
	}
	
	public boolean isTentative() {
		return isTentative.getValue();
	}
	
	public BooleanProperty tentativeProperty() {
		return isTentative;
	}
	
	public void setTentative(boolean isTentative) {
		this.isTentative.setValue(isTentative);
	}
	
	public int getFlags() {
		return flags.getValue();
	}
	
	public IntegerProperty flagsProperty() {
		return flags;
	}
	
	public void setFlags(int flags) {
		this.flags.setValue(flags);
	}
	
	public int getRequestID() {
		return requestID;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		// If this entry is tentative, start with an asterisk (*)
		if ( isTentative.getValue() ) {
			sb.append("* ");
		}
		
		// Now, add flags in brackets
		if ( flags.getValue() != 0 ) {
			Map<String, Integer> flagMap = PlaylistFlags.getFlagSet();
			boolean first = true;
			
			sb.append('[');
			
			for ( String flag : flagMap.keySet() ) {
				if ( PlaylistFlags.flagIsSet(flags.getValue(), flagMap.get(flag)) ) {
					if ( ! first ) {
						sb.append(", ");
					}
					else {
						first = false;
					}
					
					sb.append(flag);
				}
			}
			
			sb.append("] ");
		}
		
		// Finally, add title and artist
		sb.append(artist.getValue());
		sb.append(" - ");
		sb.append(title.getValue());
		
		return sb.toString();
	}
	
	public boolean equals(PlaylistEntry other) {
		return (title.getValue().equals(other.title.getValue())) && (artist.getValue().equals(other.artist.getValue()));
	}

	@Override
	public String toCSVRow() {
		// OK, technically this isn't really CSV, 'cause we're using the pipe character instead of commas,
		// but we need to do that to get around file names with commas.  We'll just decode these "CSV" rows
		// the same way
		if ( location != null ) {
			String path = LibraryManager.getLibraryForName(location.getLibraryName()).getPathInLibrary(location);
			if ( path != null ) {
				return artist.getValue() + "|" + title.getValue() + "|" + location.getLibraryName() + "|" + path + "|" + (isTentative.getValue() ? "1" : "0") + "|" + flags.getValue();
			}
			else {
				return artist.getValue() + "|" + title.getValue() + "| | |" + (isTentative.getValue() ? "1" : "0") + "|" + flags.getValue();
			}
		}
		else {
			return artist.getValue() + "|" + title.getValue() + "| | |" + (isTentative.getValue() ? "1" : "0") + "|" + flags.getValue();
		}
	}
	
	public static PlaylistEntry fromCSVRow(String row) {
		PlaylistEntry newEntry = new PlaylistEntry();
		newEntry.setFromCSV(row);
		return newEntry;
	}
	
	// Hidden method used for CSV decoding and serialization
	private void setFromCSV(String row) {
		String[] parts = row.split("\\|");
		
		LibraryEntry entry;
		if ( parts[2].equals(" ") ) {
			entry = null;
		}
		else {
			try {
				Library lib = LibraryManager.getLibraryForName(parts[2]);
				entry = lib.getEntryFromLocation(parts[3]);
			}
			catch (Exception e) {
				entry = null;
			}
		}
		
		this.artist.setValue(parts[0]);
		this.title.setValue(parts[1]);
		this.location = entry;
		this.isTentative.setValue(parts[4].equals("1"));
		this.flags.setValue(Integer.parseInt(parts[5]));
	}
	
	// "Hidden" methods needed for serialization of JavaFX properties
	private void writeObject(ObjectOutputStream s) throws IOException {
		s.writeUTF(toCSVRow());
	}
	
	private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
		artist = new SimpleStringProperty();
		title = new SimpleStringProperty();
		isTentative = new SimpleBooleanProperty();
		flags = new SimpleIntegerProperty();
		setFromCSV(s.readUTF());
	}
}
