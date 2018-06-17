package com.stereodustparticles.console.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.stereodustparticles.console.SDPConsole2;
import com.stereodustparticles.console.error.CSVParseException;
import com.stereodustparticles.console.error.HTTPException;
import com.stereodustparticles.console.error.ModemDefenestrationException;
import com.stereodustparticles.console.ui.Microwave;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * CSVLibrary: Defines a library sourced from a (remote) CSV file, e.g. the SDP libraries
 */

public class CSVLibrary implements Library {

	private static final long serialVersionUID = 1L;
	private URL csv;
	private String name;
	private int flags;
	private List<LibraryEntry> list = null;
	private boolean allowMRS;
	private boolean allowSnP;
	
	// Lame hack to allow updating parameters in old serialized instances
	// Despite this assignment here, a deserialized object will have this set
	// to whatever its value was when it was serialized
	private int apiLevel = 2;
	
	public CSVLibrary(String name, URL csv, int flags, boolean allowMRS, boolean allowSnP) {
		this.name = name;
		this.csv = csv;
		this.flags = flags;
		this.allowMRS = allowMRS;
		this.allowSnP = allowSnP;
	}
	
	@Override
	public List<LibraryEntry> getList() throws Exception {
		list = new ArrayList<LibraryEntry>();
		
		try {
			// Open connection
			HttpURLConnection connection = (HttpURLConnection)csv.openConnection();
			
			// Set User-Agent header
			connection.setRequestProperty("User-Agent", "SDPConsole/" + SDPConsole2.PROG_VERSION);
			
			// If response is not 200 OK, pop up an error and return
			if ( connection.getResponseCode() != 200 ) {
				throw new HTTPException(csv.toString(), connection.getResponseCode() + " " + connection.getResponseMessage());
			}
			
			// Create a buffered reader, then read and parse the response body line-by-line
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			while ( (line = in.readLine()) != null ) {
				try {
					LibraryEntry entry = new CSVLibraryEntry(line, name);
					list.add(entry);
				}
				catch (CSVParseException e) {
					e.setFile(csv.toString());
					
					// Show the error here, rather than throw the exception up the chain, so we can continue trying to parse the rest of the file
					Microwave.showWarning("Error Loading Library", "Got a bad line while parsing the SDP spot list.  Tell Weasel he only had ONE JOB!\n\nLine: " + e.getOffendingLine() + "\nFile: " + e.getFile());
				}
			}
			
			// We're done here, close the connection
			in.close();
		}
		catch (IOException e) {
			throw new ModemDefenestrationException(e.getMessage());
		}
		
		return list;
	}

	@Override
	public boolean changeDir(String dir) {
		// Not supported
		return false;
	}

	@Override
	public boolean upOneLevel() {
		// Not supported
		return false;
	}

	@Override
	public boolean backToRoot() {
		// Not supported
		return false;
	}

	@Override
	public boolean canGoUp() {
		// Since changing dirs isn't supported, neither is this
		return false;
	}

	@Override
	public boolean isAtRoot() {
		// Since changing dirs isn't supported, we're always at the root
		return true;
	}

	@Override
	public String getPathInLibrary(LibraryEntry entry) {
		// For remote libraries, we can just return the URL, at least with our current implementation
		return entry.getLocationAsURL().toString();
	}

	@Override
	public URL getURLFromPath(String path) throws MalformedURLException {
		return new URL(path);
	}

	@Override
	public String getLocationAsString() {
		return csv.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LibraryEntry getEntryFromLocation(String loc) {
		if ( list == null ) {
			try {
				getList();
			}
			catch (Exception e) {
				// If refresh fails, just return null for now
				e.printStackTrace();
				return null;
			}
		}
		
		for ( LibraryEntry entry : list ) {
			if ( entry.getLocationAsURL().toString().equals(loc) ) {
				return entry;
			}
		}
		
		// If we reach this point, nothing was found.  Return null.
		return null;
	}

	@Override
	public LibraryEntry getCurrentDirectory() {
		// Since CSV libraries don't support directories, we don't need to do anything for this
		return null;
	}

	@Override
	public LibraryEntry pickRandomTrack() throws Exception {
		List<LibraryEntry> list = getList();
		int chosenIndex = ThreadLocalRandom.current().nextInt(0, list.size() - 1);
		return list.get(chosenIndex);
	}

	@Override
	public int getDefaultFlags() {
		return flags;
	}
	
	@Override
	public boolean includeInSongLists() {
		// Old instances default to true, except for SDP Other Media
		if ( apiLevel < 2 ) {
			return (! name.equals("SDP Other Media"));
		}
		
		return allowMRS;
	}

	@Override
	public boolean includeInSnP() {
		// Old instances default to true, except for SDP Other Media
		if ( apiLevel < 2 ) {
			return (! name.equals("SDP Other Media"));
		}
		
		return allowSnP;
	}

}
