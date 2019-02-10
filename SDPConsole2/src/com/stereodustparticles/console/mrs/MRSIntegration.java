/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017-18
 * 
 * MRSIntegration: Base class for LER MRS integration
 */
package com.stereodustparticles.console.mrs;

import java.util.ArrayList;
import java.util.Base64;

import com.stereodustparticles.console.error.MRSException;
import com.stereodustparticles.console.library.LibraryEntry;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.musicrequestsystem.mri.MRSInterface;
import com.stereodustparticles.musicrequestsystem.mri.Request;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MRSIntegration {
	private static MRSInterface mrs = null;
	private static ObservableList<Request> reqList = FXCollections.observableArrayList();
	
	// (Re-)Initialize MRS Integration by creating a new MRSInterface instance if possible
	public static void init() {
		String url = Prefs.loadString(Prefs.MRS_URL);
		String key = Prefs.loadString(Prefs.MRS_KEY);
		
		if ( url.isEmpty() || key.isEmpty() ) {
			mrs = null;
		}
		else {
			mrs = new MRSInterface(url, key);
		}
	}
	
	// Check whether MRS integration is configured
	public static boolean isConfigured() {
		return ( mrs != null );
	}
	
	// Toggle MRS open/closed status
	// Return new status
	public static boolean toggleStatus() throws MRSException {
		int result = mrs.changeSystemStatus();
		
		if ( result != 200 ) {
			throw new MRSException("Got error code " + result + " from the MRS.");
		}
		
		return mrs.systemStatus();
	}
	
	// Refresh the list of requests
	public static void refresh(boolean showPrev) {
		ArrayList<Request> reqs = mrs.getRequests();
		Platform.runLater(() -> {
			reqList.clear();
			
			// Add only unseen and queued requests to the ObservableList
			// Loop across the request list in reverse, so as to put most recent requests on top
			for ( int i = reqs.size() - 1; i >= 0; i-- ) {
				Request r = reqs.get(i);
				
				if ( showPrev || r.getStatus() <= 1 ) {
					reqList.add(r);
				}
			}
		});
	}
	
	// Run the MRI test sequence
	public static boolean test() {
		return mrs.test();
	}
	
	// Get the observable request list
	public static ObservableList<Request> getRequestList() {
		return reqList;
	}
	
	// Queue the specified request
	public static void queue(Request req, String comment) throws MRSException {
		int result = mrs.queue(req.getID(), comment);
		
		if ( result == 404 ) {
			throw new MRSException("MRS could not be contacted or responded with a 404");
		}
		else if ( result != 200 ) {
			throw new MRSException("MRS responded with code " + result);
		}
	}
	
	// Decline the specified request
	public static void decline(Request req, String comment) throws MRSException {
		int result = mrs.decline(req.getID(), comment);
		
		if ( result == 404 ) {
			throw new MRSException("MRS could not be contacted or responded with a 404");
		}
		else if ( result != 200 ) {
			throw new MRSException("MRS responded with code " + result);
		}
	}
	
	// Mark the specified request as played
	public static void markPlayed(Request req) throws MRSException {
		int result = mrs.mark(req.getID());
		
		if ( result == 404 ) {
			throw new MRSException("MRS could not be contacted or responded with a 404");
		}
		else if ( result != 200 ) {
			throw new MRSException("MRS responded with code " + result);
		}
	}
	
	// Mark the specified ID as played (pass through to MRI)
	public static void markIDPlayed(int id) throws MRSException {
		int result = mrs.mark(id);
		
		if ( result == 404 ) {
			throw new MRSException("MRS could not be contacted or responded with a 404");
		}
		else if ( result != 200 ) {
			throw new MRSException("MRS responded with code " + result);
		}
	}
	
	// Get textual representation of request status
	// Shamelessly adapted from the MRI code
	public static String getTextualStatus(Request req) {
		switch(req.getStatus()) {
			case 0:
				return "Unseen";
				
			case 1:
				return "In queue";
				
			case 2:
				return "Declined";
				
			case 3:
				return "Played";
				
			default:
				return "Indeterminate";
		}
	}
	
	// Recover the LibraryEntry buried inside a Request object
	// Returns null if none found
	public static LibraryEntry getRequestedTrack(Request req) {
		String fn = req.getFilename();
		
		// If the above method returns nothing, no filename is available, so stop here
		if ( fn.isEmpty() ) {
			return null;
		}
		
		String decoded = new String(Base64.getDecoder().decode(fn));
		String[] parts = decoded.split(":", 3); // Limit to 3 elements, so colons in the location don't get picked up
		
		if ( parts[0].equals("MCC") ) {
			return LibraryManager.getLibraryForName(parts[1]).getEntryFromLocation(parts[2]);
		}
		
		// If we reach this point, no entry could be recovered.  Return null.
		return null;
	}
}
