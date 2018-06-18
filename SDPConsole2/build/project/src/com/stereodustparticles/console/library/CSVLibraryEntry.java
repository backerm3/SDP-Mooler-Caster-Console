package com.stereodustparticles.console.library;

import java.net.URL;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.error.CSVParseException;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * CSVLibraryEntry: defines the structure of a library entry sourced from a CSV library
 */

public class CSVLibraryEntry implements LibraryEntry {

	private static final long serialVersionUID = 1L;
	
	private URL location;
	private String libName;
	private String title;
	private String artist;
	private String duration;
	
	public CSVLibraryEntry(String csvRow, String libName) throws CSVParseException {
		try {
			String[] params = csvRow.split(",");
			this.title = params[0];
			this.artist = params[1];
			this.duration = params[2];
			this.location = new URL(params[3]);
			this.libName = libName;
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw new CSVParseException(csvRow);
		}
	}
	
	// Return the title when queried for a display name
	public String toString() {
		return title;
	}
	
	@Override
	public URL getLocationAsURL() {
		return location;
	}

	@Override
	public boolean isDir() {
		// CSV libraries don't support directories (at this time, at least)
		return false;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getArtist() {
		return artist;
	}

	@Override
	public int getDuration() {
		// The CSV does not include exact duration, so we'll return -1
		// to indicate that the proper value must be calculated separately
		return -1;
	}

	@Override
	public String getDurationPreview() {
		return duration;
	}

	@Override
	public String getLibraryName() {
		return libName;
	}

	@Override
	public String toMRSData() {
		// Sanitize as best we can
		artist = Utils.sanitizeForMRS(artist);
		title = Utils.sanitizeForMRS(title);
		
		String meta = "MCC:" + libName + ":" + location.toString();
		return artist + "|" + title + "|" + libName + "||" + meta; // Use library name as album for now
	}

	@Override
	public boolean isLoadable() {
		// Should always be loadable
		return true;
	}

}
