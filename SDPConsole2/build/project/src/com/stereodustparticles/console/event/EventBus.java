package com.stereodustparticles.console.event;

import java.util.ArrayList;
import java.util.HashMap;

import com.stereodustparticles.console.multi.MultiConsole;
import com.stereodustparticles.console.pref.Prefs;

import javafx.application.Platform;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * EventBus: The guts of the Console's event management system - handles routing of events to different places, etc.
 */

public class EventBus {
	// This structure stores the listeners.
	// It maps event types with lists of listeners that listen for said type of event.
	private static HashMap<EventType, ArrayList<EventListener>> listenerDirectory = new HashMap<EventType, ArrayList<EventListener>>();
	
	// Register a new event listener
	public static void registerListener(EventType forType, EventListener listener) {
		ArrayList<EventListener> addItHere = listenerDirectory.get(forType);
		
		// If the appropriate section of the listener directory was not found, create it
		if ( addItHere == null ) {
			addItHere = new ArrayList<EventListener>();
			listenerDirectory.put(forType, addItHere);
		}
		
		addItHere.add(listener);
	}
	
	// Fire an event.  In other words, read the event's type, then loop across the appropriate section of the listener directory, executing each listener's method in sequence.
	public static void fireEvent(Event e) {
		// Run this "later", so that it always runs in the application thread
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				ArrayList<EventListener> toExec = listenerDirectory.get(e.getType());
				
				// If no listeners were found for that event type, swallow the event and do nada
				if ( toExec == null ) {
					return;
				}
				
				// If we're here, presumably we have things to execute.  So let's do it!
				for ( EventListener listener : toExec ) {
					listener.onEvent(e);
				}
			}
			
		});
		
		// If this was a locally-generated event, also forward it to our Multi-Console peers
		// Don't do this, however, if the event matches certain types
		if ( e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) && e.getType() != EventType.SPOT_REQUEST_LOAD && e.getType() != EventType.LIBRARY_LIST_UPDATED ) {
			MultiConsole.forwardEvent(e, null);
		}
	}
}
