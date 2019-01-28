package com.stereodustparticles.console.library;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.stereodustparticles.console.SDPConsole2;
import com.stereodustparticles.console.Utils;

public class FSLibrary implements Library {

	private static final long serialVersionUID = 1L;
	private File baseDir;
	private File currentDir;
	private String name;
	private int flags;
	private boolean allowMRS;
	private boolean allowSnP;
	private transient Map<File, List<LibraryEntry>> dirListCache;
	
	// Lame hack to allow updating parameters in old serialized instances
	// Despite this assignment here, a deserialized object will have this set
	// to whatever its value was when it was serialized
	private int apiLevel = 2;
	
	// Subclass that defines the comparison rules used to sort a directory listing
	private class FSLibraryComparator implements Comparator<File> {

		@Override
		public int compare(File a, File b) {
			// If both files are or are not directories, use default (alphabetical) sorting
			if ( a.isDirectory() == b.isDirectory() ) {
				return a.compareTo(b);
			}
			// If one file is a directory and the other is not, place the directory first
			else if ( a.isDirectory() && ! b.isDirectory() ) {
				return -1;
			}
			else {
				return 1;
			}
		}
		
	}
	
	public FSLibrary(String name, File baseDir, int flags, boolean allowMRS, boolean allowSnP) {
		this.baseDir = baseDir;
		this.currentDir = baseDir;
		this.name = name;
		this.flags = flags;
		this.allowMRS = allowMRS;
		this.allowSnP = allowSnP;
		this.dirListCache = new HashMap<File, List<LibraryEntry>>();
	}
	
	@Override
	public void resetCache() {
		dirListCache.clear();
	}
	
	@Override
	public List<LibraryEntry> getList() throws Exception {
		// Do we have a cached listing for this directory?
		if ( dirListCache.containsKey(currentDir) ) {
			return dirListCache.get(currentDir);
		}
		else {
			// Get a directory listing
			File[] dirList = currentDir.listFiles();
			
			// If no listing was returned, the user only had one job
			if ( dirList == null ) {
				throw new FileNotFoundException();
			}
			
			// Alphabetize the directory listing (yes, we really need that)
			Arrays.sort(dirList, new FSLibraryComparator());
			
			// Make a list of LibraryEntry objects from it
			List<LibraryEntry> library = new ArrayList<LibraryEntry>();
			for ( File file : dirList ) {
				library.add(new FSLibraryEntry(file, name));
			}
			
			// Store this directory listing in the cache
			dirListCache.put(currentDir, library);
			
			return library;
		}
	}

	@Override
	public boolean changeDir(String dir) {
		File newDir = new File(currentDir, dir);
		
		// Check that everyone did their One Job
		if ( newDir.exists() && newDir.isDirectory() ) {
			currentDir = newDir;
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public boolean upOneLevel() {
		if ( isAtRoot() ) {
			return false;
		}
		
		currentDir = currentDir.getParentFile();
		return true;
	}

	@Override
	public boolean backToRoot() {
		currentDir = baseDir;
		return true;
	}

	@Override
	public boolean canGoUp() {
		return (! isAtRoot());
	}

	@Override
	public boolean isAtRoot() {
		return currentDir.equals(baseDir);
	}

	@Override
	public String getPathInLibrary(LibraryEntry entry) {
		if ( ! (entry instanceof FSLibraryEntry) ) {
			return null;
		}
		FSLibraryEntry fsEntry = (FSLibraryEntry)entry;
		
		File fullPath = fsEntry.getLocationAsFile();
		
		String relative = Paths.get(baseDir.toURI()).relativize(Paths.get(fullPath.toURI())).toString();
		
		return relative;
	}

	@Override
	public URL getURLFromPath(String path) throws MalformedURLException {
		File fullPath = new File(baseDir, path);
		return fullPath.toURI().toURL();
	}

	@Override
	public String getLocationAsString() {
		return baseDir.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LibraryEntry getEntryFromLocation(String loc) {
		File fullPath = new File(baseDir, loc);
		return new FSLibraryEntry(fullPath, name);
	}

	@Override
	public LibraryEntry getCurrentDirectory() {
		return new FSLibraryEntry(currentDir, name);
	}
	
	@Override
	public LibraryEntry pickRandomTrack() throws Exception {
		LibraryEntry ret = null;
		
		File startDir = currentDir;
		
		backToRoot();
		
		while ( ret == null ) {
			List<LibraryEntry> list = getList();
			int chosenIndex = SDPConsole2.random.nextInt(list.size());
			FSLibraryEntry chosen = (FSLibraryEntry)list.get(chosenIndex);
			if ( chosen.isDir() ) {
				currentDir = chosen.getLocationAsFile();
				continue;
			}
			else {
				// Check the file extension against a "shortlist" of permissible ones, to try to avoid problems
				String ext = Utils.getFileExtension(chosen.getLocationAsFile());
				if ( ext.equals("wav") || ext.equals("mp3") || ext.equals("m4a") || ext.equals("flac") || ext.equals("ogg") || ext.equals("aif") ) {
					ret = chosen;
					break;
				}
				else {
					continue;
				}
			}
		}
		
		currentDir = startDir;
		
		return ret;
	}

	@Override
	public int getDefaultFlags() {
		return flags;
	}

	@Override
	public boolean includeInSongLists() {
		// Old instances default to true
		if ( apiLevel < 2 ) {
			return true;
		}
		
		return allowMRS;
	}

	@Override
	public boolean includeInSnP() {
		// Old instances default to true
		if ( apiLevel < 2 ) {
			return true;
		}
		
		return allowSnP;
	}

}
