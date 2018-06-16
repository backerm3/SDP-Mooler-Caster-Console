/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017-18
 * 
 * MRSIntegration: Base class for LER MRS integration
 */
package com.stereodustparticles.console.mrs;

import com.stereodustparticles.console.error.MRSException;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.musicrequestsystem.mri.MRSInterface;

public class MRSIntegration {
	private static MRSInterface mrs = null;
	
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
}
