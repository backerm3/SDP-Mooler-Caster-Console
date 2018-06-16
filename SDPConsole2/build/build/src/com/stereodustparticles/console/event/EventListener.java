package com.stereodustparticles.console.event;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * EventListener: defines the structure of (you guessed it!) an event listener!
 */

@FunctionalInterface
public interface EventListener {
	// This method is what's called when the event occurs
	public void onEvent(Event e);
}
