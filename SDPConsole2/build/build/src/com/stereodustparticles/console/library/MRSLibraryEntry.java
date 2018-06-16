/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * MRSLibraryEntry: Defines an entry in an MRS library
 */
package com.stereodustparticles.console.library;

import java.net.MalformedURLException;
import java.net.URL;

import com.stereodustparticles.console.error.HTTPException;
import com.stereodustparticles.console.error.ModemDefenestrationException;

public class MRSLibraryEntry implements LibraryEntry {

	private static final long serialVersionUID = 1L;
	private LibraryEntry src = null;
	private int id;
	private String requester;
	private String ip;
	private String date;
	private String reqText;
	private int status;
	private MRSLibrary lib;
	
	public MRSLibraryEntry(String mrsRes, MRSLibrary lib) {
		this.lib = lib;
		
		// Decode the MRS response row
		String[] mrsContents = mrsRes.split("\\|");
		id = Integer.parseInt(mrsContents[0]);
		requester = mrsContents[1];
		ip = mrsContents[2];
		date = mrsContents[3];
		reqText = mrsContents[4];
		status = Integer.parseInt(mrsContents[5]);
		// For now, we'll assume that the metadata will always reside in the last field
		if ( ! mrsContents[mrsContents.length - 1].isEmpty() ) {
			String[] parts = mrsContents[mrsContents.length - 1].split(":", 3); // Limit to 3 elements, so colons in the location don't get picked up
			if ( parts[0].equals("MCC") ) {
				src = LibraryManager.getLibraryForName(parts[1]).getEntryFromLocation(parts[2]);
			}
		}
	}
	
	@Override
	public String toString() {
		if ( status == 0 ) {
			return "* " + reqText;
		}
		else {
			return reqText;
		}
	}
	
	// Mark this request as queued
	public void markQueued() throws MalformedURLException, HTTPException, ModemDefenestrationException {
		lib.markQueued(id);
	}
	
	// Mark this request as played
	public void markPlayed() throws MalformedURLException, HTTPException, ModemDefenestrationException {
		lib.markPlayed(id);
	}
	
	public String getSourcePath() {
		if ( src == null ) {
			return null;
		}
		
		return src.getLibraryName() + ":" + LibraryManager.getLibraryForName(src.getLibraryName()).getPathInLibrary(src);
	}
	
	public int getID() {
		return id;
	}
	
	public String getRequester() {
		return requester;
	}
	
	public String getIP() {
		return ip;
	}
	
	public String getDate() {
		return date;
	}

	@Override
	public URL getLocationAsURL() {
		if ( src == null ) {
			return null;
		}
		
		return src.getLocationAsURL();
	}

	@Override
	public String getLibraryName() {
		return lib.getName();
	}

	@Override
	public boolean isDir() {
		return false;
	}

	@Override
	public String getTitle() {
		if ( src == null ) {
			return reqText;
		}
		
		return src.getTitle();
	}

	@Override
	public String getArtist() {
		if ( src == null ) {
			return "(Request did not contain metadata)";
		}
		
		return src.getArtist();
	}

	@Override
	public int getDuration() {
		if ( src == null ) {
			return 0;
		}
		
		return src.getDuration();
	}

	@Override
	public String getDurationPreview() {
		if ( src == null ) {
			return "0:00";
		}
		
		return src.getDurationPreview();
	}

	@Override
	public String toMRSData() {
		// This is not needed for MRS entries, since it's used only to populate MRS song lists
		return null;
	}

	@Override
	public boolean isLoadable() {
		// Loadable only if there is a backing LibraryEntry stored
		return (src != null);
	}

}
