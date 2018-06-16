/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * MRSLibrary: Defines a library based around an LER MRS instance
 */
package com.stereodustparticles.console.library;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.stereodustparticles.console.SDPConsole2;
import com.stereodustparticles.console.error.HTTPException;
import com.stereodustparticles.console.error.MRSPasswordException;
import com.stereodustparticles.console.error.ModemDefenestrationException;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;

public class MRSLibrary implements Library {

	// Portions adapted from https://stackoverflow.com/questions/16150089/how-to-handle-cookies-in-httpurlconnection-using-cookiemanager
	
	private static final long serialVersionUID = 1L;
	private static final String COOKIES_HEADER = "Set-Cookie";
	private static final String COOKIE_REQ_HEADER = "Cookie";
	private static final String USER_AGENT_HEADER = "User-Agent";
	private static final String MRS_AUTH_HEADER = "X-MRS-Auth";
	private static final String POST = "POST";
	private static final String LOGIN_PAGE = "login.php";
	private static final String REQUEST_LIST_PAGE = "mrreqlist.php";
	private static final String QUEUE_PAGE = "queue.php";
	private static final String PLAYED_PAGE = "played.php";
	private static final String ADMIN_PAGE = "admin.php";
	
	private String mrsBase;
	private String name;
	private int flags;
	private List<LibraryEntry> list = null;
	private String password = null;
	
	private transient CookieManager cm = null;
	
	public MRSLibrary(String name, String base, int flags) {
		this.name = name;
		this.mrsBase = base;
		this.flags = flags;
	}
	
	// MRS communication functions
	
	// Log into the MRS
	private synchronized void doMRSLogin() throws HTTPException, ModemDefenestrationException, MalformedURLException {
		password = null;
		
		Platform.runLater(() -> {
			synchronized (this) {
				password = Microwave.getPassword("MRS Login", "MRS Admin Password:");
				this.notifyAll();
			}
		});
		
		while ( password == null ) {
			try {
				this.wait();
			}
			catch (InterruptedException e) {
				// Should not happen
				e.printStackTrace();
			}
		}
		
		if ( password.isEmpty() ) {
			return;
		}
		
		if ( cm == null ) {
			cm = new CookieManager();
		}
		
		try {
			// Open connection
			URL login = new URL(mrsBase + LOGIN_PAGE);
			HttpURLConnection connection = (HttpURLConnection)login.openConnection();
			
			// Set User-Agent header
			connection.setRequestProperty(USER_AGENT_HEADER, "SDPConsole/" + SDPConsole2.PROG_VERSION);
			
			// Set request method and add parameters
			connection.setRequestMethod(POST);
			connection.setDoOutput(true);
			OutputStream os = connection.getOutputStream();
			os.write(("s=y&pass=" + password).getBytes());
			os.flush();
			os.close();
			
			// If response is not 200 OK, throw an exception
			if ( connection.getResponseCode() != 200 ) {
				throw new HTTPException(login.toString(), connection.getResponseCode() + " " + connection.getResponseMessage());
			}
			
			// Store the session cookie
			Map<String, List<String>> headerFields = connection.getHeaderFields();
			List<String> cookiesHeader = headerFields.get(COOKIES_HEADER);

			if (cookiesHeader != null) {
			    for (String cookie : cookiesHeader) {
			        cm.getCookieStore().add(null,HttpCookie.parse(cookie).get(0));
			    }
			}
		}
		catch ( IOException e ) {
			if ( e instanceof MalformedURLException) {
				throw (MalformedURLException)e;
			}
			else {
				throw new ModemDefenestrationException(e.getMessage());
			}
		}
	}
	
	// Do an generic POST request - utility function for functions below
	private void doGenericPost(String page, String params) throws HTTPException, ModemDefenestrationException, MalformedURLException {
		doGenericPost(page, params, false);
	}
	
	private void doGenericPost(String page, String params, boolean second) throws HTTPException, ModemDefenestrationException, MalformedURLException {
		if ( cm == null ) {
			cm = new CookieManager();
		}
		
		try {
			// Open connection
			URL login = new URL(mrsBase + page);
			HttpURLConnection connection = (HttpURLConnection)login.openConnection();
			
			// Set User-Agent header
			connection.setRequestProperty(USER_AGENT_HEADER, "SDPConsole/" + SDPConsole2.PROG_VERSION);
			
			// Add cookies
			if (cm.getCookieStore().getCookies().size() > 0) {
				// TODO is it always the first cookie?
			    connection.setRequestProperty(COOKIE_REQ_HEADER, cm.getCookieStore().getCookies().get(0).toString());
			}
			
			// Set request method and add parameters
			connection.setRequestMethod(POST);
			connection.setDoOutput(true);
			OutputStream os = connection.getOutputStream();
			os.write(params.getBytes());
			os.flush();
			os.close();
			
			// If response is not 200 OK, throw an exception
			if ( connection.getResponseCode() != 200 ) {
				throw new HTTPException(login.toString(), connection.getResponseCode() + " " + connection.getResponseMessage());
			}
			
			// Was authentication accepted?
			// Assume yes if auth header was not found, so we don't get thrown by the nasty ballot box stuffer bug in the MRS
			String authRes = connection.getHeaderField(MRS_AUTH_HEADER);
			if ( ! second && authRes != null && ! authRes.equals("OK") ) {
				doMRSLogin();
				doGenericPost(page, params, true);
			}
		}
		catch ( IOException e ) {
			if ( e instanceof MalformedURLException) {
				throw (MalformedURLException)e;
			}
			else {
				throw new ModemDefenestrationException(e.getMessage());
			}
		}
	}
	
	// Mark a request as queued
	public void markQueued(int id) throws HTTPException, ModemDefenestrationException, MalformedURLException {
		// TODO support comments
		doGenericPost(QUEUE_PAGE, "confirm=y&p=" + id + "&comment=");
	}
	
	// Mark a request as played
	public void markPlayed(int id) throws HTTPException, ModemDefenestrationException, MalformedURLException {
		doGenericPost(PLAYED_PAGE, "confirm=y&p=" + id);
	}
	
	// Open the request lines
	public void open() throws HTTPException, ModemDefenestrationException, MalformedURLException {
		doGenericPost(ADMIN_PAGE, "s=y&posting=yes&cposting=");
	}
	
	// Close the request lines
	public void close() throws HTTPException, ModemDefenestrationException, MalformedURLException {
		doGenericPost(ADMIN_PAGE, "s=y&posting=no&cposting=");
	}
	
	private List<String> getRequestList() throws HTTPException, ModemDefenestrationException, MalformedURLException {
		if ( cm == null ) {
			cm = new CookieManager();
		}
		
		try {
			// Open connection
			URL login = new URL(mrsBase + REQUEST_LIST_PAGE);
			HttpURLConnection connection = (HttpURLConnection)login.openConnection();
			
			// Set User-Agent header
			connection.setRequestProperty(USER_AGENT_HEADER, "SDPConsole/" + SDPConsole2.PROG_VERSION);
			
			// Add cookies
			if (cm.getCookieStore().getCookies().size() > 0) {
				// TODO is it always the first cookie?
			    connection.setRequestProperty(COOKIE_REQ_HEADER, cm.getCookieStore().getCookies().get(0).toString());
			}
			
			// If response is not 200 OK, throw an exception
			if ( connection.getResponseCode() != 200 ) {
				throw new HTTPException(login.toString(), connection.getResponseCode() + " " + connection.getResponseMessage());
			}
			
			// Create a buffered reader, then read the response body line-by-line
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
			String line;
			List<String> lines = new ArrayList<String>();
			boolean okFound = false;
			while ( (line = in.readLine()) != null ) {
				if ( ! okFound ) {
					if ( line.equals("OK") ) {
						okFound = true;
						continue;
					}
					else {
						return null;
					}
				}
				else {
					lines.add(line);
				}
			}
			return lines;
		}
		catch ( IOException e ) {
			if ( e instanceof MalformedURLException) {
				throw (MalformedURLException)e;
			}
			else {
				throw new ModemDefenestrationException(e.getMessage());
			}
		}
	}

	@Override
	public List<LibraryEntry> getList() throws Exception {
		list = new ArrayList<LibraryEntry>();
		
		List<String> requests = getRequestList();
		if ( requests == null ) {
			doMRSLogin();
			requests = getRequestList();
			if ( requests == null ) {
				throw new MRSPasswordException("MRS login failed");
			}
		}
		
		for ( String req : requests ) {
			list.add(new MRSLibraryEntry(req, this));
		}
		
		return list;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean changeDir(String dir) {
		// Not supported (or needed, for that matter)
		return false;
	}

	@Override
	public boolean upOneLevel() {
		// Also not supported
		return false;
	}

	@Override
	public boolean canGoUp() {
		// Still not supported
		return false;
	}

	@Override
	public boolean isAtRoot() {
		// This also isn't supported, but it defaults to true 'cause it's somehow different
		return true;
	}

	@Override
	public boolean backToRoot() {
		// What do you think this one isn't?
		return false;
	}

	@Override
	public String getPathInLibrary(LibraryEntry entry) {
		MRSLibraryEntry rEntry = (MRSLibraryEntry) entry;
		return rEntry.getSourcePath();
	}

	@Override
	public URL getURLFromPath(String path) throws MalformedURLException {
		String[] parts = path.split(":", 2);
		return LibraryManager.getLibraryForName(parts[0]).getURLFromPath(parts[1]);
	}

	@Override
	public LibraryEntry getCurrentDirectory() {
		// You can probably guess by now...
		return null;
	}

	@Override
	public String getLocationAsString() {
		return mrsBase;
	}

	@Override
	public LibraryEntry getEntryFromLocation(String loc) {
		String[] parts = loc.split(":", 2);
		return LibraryManager.getLibraryForName(parts[0]).getEntryFromLocation(parts[1]);
	}

	@Override
	public LibraryEntry pickRandomTrack() throws Exception {
		// TODO restrict to unplayed tracks?
		List<LibraryEntry> list = getList();
		int chosenIndex = ThreadLocalRandom.current().nextInt(0, list.size() - 1);
		return list.get(chosenIndex);
	}

	@Override
	public int getDefaultFlags() {
		return flags;
	}

}
