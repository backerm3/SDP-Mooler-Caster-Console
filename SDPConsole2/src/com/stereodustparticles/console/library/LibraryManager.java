package com.stereodustparticles.console.library;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.stereodustparticles.console.pref.Prefs;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * LibraryManager: maintains the configured libraries
 */

public class LibraryManager {
	private static Map<String, Library> libraryList;
	
	public static void init() {
		// Load the library list from the Prefs table
		libraryList = Prefs.loadLibraryList();
	}
	
	// Save a new or changed library
	public static void putLibrary(String name, Library lib) {
		libraryList.put(name, lib);
		Prefs.saveLibraryList();
	}
	
	// Same as above, but if old and new names are different, delete the old after putting the new
	public static void putLibrary(String name, String oldName, Library lib) {
		if ( oldName != null && ! name.equals(oldName) ) {
			libraryList.remove(oldName);
		}
		
		putLibrary(name, lib);
	}
	
	public static void removeLibrary(String name) {
		libraryList.remove(name);
		Prefs.saveLibraryList();
	}
	
	public static Library getLibraryForName(String name) {
		return libraryList.get(name);
	}
	
	public static Set<String> getAvailableLibraries() {
		return libraryList.keySet();
	}
	
	public static List<Library> getSnPLibraries() {
		ArrayList<Library> libList = new ArrayList<Library>();
		
		for ( Library lib : libraryList.values() ) {
			if ( lib.includeInSnP() ) {
				libList.add(lib);
			}
		}
		
		return libList;
	}
	
	public static Map<String, Library> getLibraryList() {
		return libraryList;
	}
}
