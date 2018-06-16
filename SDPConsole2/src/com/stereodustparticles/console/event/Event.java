package com.stereodustparticles.console.event;

import java.io.Serializable;

import com.stereodustparticles.console.pref.Prefs;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Event: defines the structure of an event that can be fired/received
 * This class is serializable, allowing instances to be sent over network links, etc. to other Consoles
 */

public class Event implements Serializable {
	// This will need to be incremented with new versions of this class
	private static final long serialVersionUID = 1L;
	
	private EventType type;
	private Object[] params;
	private String originator;
	
	public Event(EventType type, Object... params) {
		this.type = type;
		this.params = params;
		this.originator = Prefs.loadString(Prefs.MC_IDENTITY);
	}
	
	// Return the event type
	public EventType getType() {
		return type;
	}
	
	// Return the event parameters object
	public Object[] getParams() {
		return params;
	}
	
	// Return the event originator's identity
	public String getOriginator() {
		return originator;
	}
}
