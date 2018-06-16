/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * PlaylistFlags: Defines the possible "flags" that can be set on a playlist entry
 */
package com.stereodustparticles.console.playlist;

import java.util.LinkedHashMap;
import java.util.Map;

import com.stereodustparticles.console.pref.Prefs;

public class PlaylistFlags {
	// Flag constants
	public static final int NONE = 0;
	public static final int AD = 1;
	public static final int REQUEST = 2;
	public static final int CD = 4;
	public static final int CASSETTE = 8;
	public static final int VINYL = 16;
	public static final int EIGHT_TRACK = 32;
	public static final int CUSTOM_1 = 64;
	public static final int CUSTOM_2 = 128;
	public static final int CUSTOM_3 = 256;
	
	// Does the given bit field contain the given flag?
	public static boolean flagIsSet(int flags, int flag) {
		return (flags & flag) != 0;
	}
	
	// Return a map of flags (as strings) to their values
	// Used to iterate across all possible flags
	public static Map<String, Integer> getFlagSet() {
		Map<String, Integer> flagMap = new LinkedHashMap<String, Integer>();
		
		flagMap.put("AD", AD);
		flagMap.put("REQUEST", REQUEST);
		flagMap.put("CD", CD);
		flagMap.put("CASSETTE", CASSETTE);
		flagMap.put("VINYL", VINYL);
		flagMap.put("8-TRACK", EIGHT_TRACK);
		flagMap.put(Prefs.loadString(Prefs.PLAYLIST_CUSTOM_FLAG_1), CUSTOM_1);
		flagMap.put(Prefs.loadString(Prefs.PLAYLIST_CUSTOM_FLAG_2), CUSTOM_2);
		flagMap.put(Prefs.loadString(Prefs.PLAYLIST_CUSTOM_FLAG_3), CUSTOM_3);
		
		return flagMap;
	}
}
