package com.stereodustparticles.console.pref;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.stereodustparticles.console.SDPConsole2;
import com.stereodustparticles.console.library.CSVLibrary;
import com.stereodustparticles.console.library.Library;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.multi.SerialUtils;
import com.stereodustparticles.console.playlist.PlaylistFlags;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Prefs: Handles storing and loading of user preferences
 */

public class Prefs {
	private static Preferences prefs;
	private static final HashMap<String, Object> defaults = new HashMap<String, Object>();
	
	// Preference key constants
	public static final String LIBRARY_LIST = "library_list";
	public static final String SOUNDBOARD_ROWS = "sb_rows";
	public static final String SOUNDBOARD_COLS = "sb_cols";
	public static final String CACHE_DIR = "cache_dir";
	public static final String PLAYLIST_CUSTOM_FLAG_1 = "custom_flag_1";
	public static final String PLAYLIST_CUSTOM_FLAG_2 = "custom_flag_2";
	public static final String PLAYLIST_CUSTOM_FLAG_3 = "custom_flag_3";
	public static final String LAST_PLAYLIST_EXPORT_DIR = "last_playlist_export_dir";
	public static final String LAST_LAYOUT_DIR = "last_layout_dir";
	public static final String STARTUP_LAYOUT = "startup_layout";
	public static final String FLASH_POINT = "deck_flash_point";
	public static final String AUTO_ADD_TENTATIVE = "auto_add_tentative";
	public static final String PLAYLIST_SAVE_TIMEOUT = "playlist_save_timeout";
	public static final String MC_IDENTITY = "mc_identity";
	public static final String MC_OUTBOUND_IP = "mc_outbound_ip";
	public static final String MC_OUTBOUND_PORT = "mc_outbound_port";
	public static final String MC_INBOUND_PORT = "mc_inbound_port";
	public static final String MC_SERIAL_PORT = "mc_serial_port";
	public static final String MC_OUTBOUND_ENABLE = "mc_outbound_enable";
	public static final String MC_INBOUND_ENABLE = "mc_inbound_enable";
	public static final String MC_SERIAL_ENABLE = "mc_serial_enable";
	public static final String MC_SOUNDBOARD_MODE = "mc_soundboard_mode";
	public static final String MC_DECK_MODE = "mc_deck_mode";
	public static final String MC_PLAYLIST_MODE = "mc_playlist_mode";
	public static final String MRS_URL = "mrs_url";
	public static final String MRS_KEY = "mrs_key";
	
	public static void init() {
		prefs = Preferences.userNodeForPackage(SDPConsole2.class);
		
		// Build the defaults table
		defaults.put(SOUNDBOARD_ROWS, 4);
		defaults.put(SOUNDBOARD_COLS, 4);
		defaults.put(CACHE_DIR, System.getProperty("user.home") + File.separator + "Downloads" + File.separator + "SDP");
		defaults.put(PLAYLIST_CUSTOM_FLAG_1, "Custom #1");
		defaults.put(PLAYLIST_CUSTOM_FLAG_2, "Custom #2");
		defaults.put(PLAYLIST_CUSTOM_FLAG_3, "Custom #3");
		defaults.put(LAST_PLAYLIST_EXPORT_DIR, System.getProperty("user.home"));
		defaults.put(LAST_LAYOUT_DIR, System.getProperty("user.home"));
		defaults.put(STARTUP_LAYOUT, "");
		defaults.put(FLASH_POINT, 30);
		defaults.put(AUTO_ADD_TENTATIVE, true);
		defaults.put(PLAYLIST_SAVE_TIMEOUT, 3);
		defaults.put(MC_IDENTITY, "I_HAZ_BAD_IDEAZ");
		defaults.put(MC_OUTBOUND_IP, "192.168.1.1");
		defaults.put(MC_OUTBOUND_PORT, 9994);
		defaults.put(MC_INBOUND_PORT, 9994);
		defaults.put(MC_SERIAL_PORT, SerialUtils.getDefaultPort());
		defaults.put(MC_OUTBOUND_ENABLE, false);
		defaults.put(MC_INBOUND_ENABLE, false);
		defaults.put(MC_SERIAL_ENABLE, false);
		defaults.put(MC_SOUNDBOARD_MODE, "A"); // First char = "A" for stand-alone, "M" for master, "S" for slave - further chars are slave-to ID
		defaults.put(MC_DECK_MODE, "A");
		defaults.put(MC_PLAYLIST_MODE, "A");
		defaults.put(MRS_URL, "");
		defaults.put(MRS_KEY, "");
	}
	
	public static void saveLibraryList() {
		Map<String, Library> libList = LibraryManager.getLibraryList();
		try {
			PrefObj.putObject(prefs, LIBRARY_LIST, libList);
		}
		catch (ClassNotFoundException | IOException | BackingStoreException e) {
			// TODO improve this?  move it into UI class?
			Platform.runLater(() -> Microwave.showException("Error Saving Preferences", "Saving the library list failed.  Something most definitely needs to be microwaved.", e));
		}
	}
	
	public static Map<String, Library> loadLibraryList() {
		try {
			// Warnings must be suppressed here because the generics are not saved in serialization
			// However, we do actually know what they are (as long as no one has f'ed with the prefs table)
			@SuppressWarnings("unchecked")
			Map<String, Library> libList = (Map<String, Library>) PrefObj.getObject(prefs, LIBRARY_LIST);
			
			if ( libList == null ) {
				return getDefaultLibraryList();
			}
			else {
				// Update all retrieved libraries to the latest API level
				for ( Library lib : libList.values() ) {
					lib.updateAPILevel();
				}
				
				return libList;
			}
		}
		catch (EOFException e) {
			// If this happens, presumably no library list has been saved yet
			// In this case, just return the default, without complaining
			return getDefaultLibraryList();
		}
		catch (ClassNotFoundException | IOException | BackingStoreException e) {
			Microwave.showException("Error Loading Preferences", "Loading the library list failed.  Something most definitely needs to be microwaved.\n\nFor now, the default library list has been loaded.", e);
			return getDefaultLibraryList();
		}
	}
	
	// Build and return the default library list
	private static HashMap<String, Library> getDefaultLibraryList() {
		HashMap<String, Library> defaultLibs = new HashMap<String, Library>();
		try {
			defaultLibs.put("SDP Ads", new CSVLibrary("SDP Ads", new URL("http://www.stereodustparticles.com/sdp-ads/ads.csv"), PlaylistFlags.AD, true, true));
			defaultLibs.put("SDP Other Media", new CSVLibrary("SDP Other Media", new URL("http://www.stereodustparticles.com/othermedia/othermedia.csv"), PlaylistFlags.NONE, false, false));
		}
		catch (MalformedURLException e) {
			// This should never happen.  If it does, IYLGI must report to the red courtesy club at once!
			e.printStackTrace();
		}
		return defaultLibs;
	}
	
	// Load/Save int
	public static int loadInt(String key) {
		return prefs.getInt(key, (int)defaults.get(key));
	}
	
	public static void saveInt(String key, int val) {
		prefs.putInt(key, val);
	}
	
	// Load/Save String
	public static String loadString(String key) {
		return prefs.get(key, (String)defaults.get(key));
	}
	
	public static void saveString(String key, String val) {
		prefs.put(key, val);
	}
	
	// Load/Save Boolean
	public static boolean loadBoolean(String key) {
		return prefs.getBoolean(key, (boolean)defaults.get(key));
	}
	
	public static void saveBoolean(String key, boolean val) {
		prefs.putBoolean(key, val);
	}
}
