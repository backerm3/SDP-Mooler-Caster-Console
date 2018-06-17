package com.stereodustparticles.console.library;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Library: Defines the basis of a library structure
 */

public interface Library extends Serializable {
	public List<LibraryEntry> getList() throws Exception; // Get a list of the entries in the current directory.  Generally, this will also refresh the list.
	
	// The following methods will return true on success, or false if the action was not performed for any reason
	// Not all libraries will necessarily support these functions.  Those that don't should immediately return false for these.
	public String getName(); // Return this library's name
	public int getDefaultFlags(); // Return this library's default playlist flags
	public boolean includeInSongLists(); // Return whether this library may be included in MRS song lists
	public boolean includeInSnP(); // Return whether this library may be included in Stream 'n' Poop(TM) (when auto-selecting tracks)
	public boolean changeDir(String dir); // Change directories
	public boolean upOneLevel(); // Go up to the parent directory
	public boolean canGoUp(); // Return whether or not we can go up a level
	public boolean isAtRoot(); // Return whether or not we're at the root
	public boolean backToRoot(); // Go back to the library root directory
	public String getPathInLibrary(LibraryEntry entry); // Return the path relative to the library base directory of the specified file in the current directory
	public URL getURLFromPath(String path) throws MalformedURLException; // Return the URL for the specified relative path
	public LibraryEntry getCurrentDirectory(); // Return the current directory as a library entry
	public String getLocationAsString(); // Return the library's path as a string (for graphical editing)
	public LibraryEntry getEntryFromLocation(String loc); // Return a LibraryEntry corresponding to the specified relative path within this library
	public LibraryEntry pickRandomTrack() throws Exception; // Return a randomly-selected track
}
