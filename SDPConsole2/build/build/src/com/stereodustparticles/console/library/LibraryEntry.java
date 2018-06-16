package com.stereodustparticles.console.library;

import java.io.Serializable;
import java.net.URL;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * LibraryEntry: This interface sets the methods required for a library entry (either FS or CSV)
 */

public interface LibraryEntry extends Serializable {
	public URL getLocationAsURL(); // Return the location of the referenced file as a URL object
	public String getLibraryName(); // Return the name of the library this entry belongs to
	public boolean isDir(); // Return true if this entry represents a (sub-)directory
	public boolean isLoadable(); // Return true if this entry points to a valid audio file
	public String getTitle(); // Return the "title" field of the track's metadata
	public String getArtist(); // Return the "artist" field of the track's metadata
	public int getDuration(); // Return the actual duration of the track in tenths of seconds
	public String getDurationPreview(); // Return a string representation of the track's duration (can be only to the whole second)
	public String toMRSData(); // Return an MRS-compatible metadata string for this entry
	
	// NOTE: Classes implementing this interface should also override the toString() method so it returns
	// an appropriate display name for the track.  (This cannot be mandated by the interface because the
	// java.lang.Object class implements it.  Thus, all classes automatically implement it, whether usefully
	// or not.)
}
