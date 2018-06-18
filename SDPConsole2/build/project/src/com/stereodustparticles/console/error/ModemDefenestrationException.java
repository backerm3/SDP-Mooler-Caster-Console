package com.stereodustparticles.console.error;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * ModemDefenestrationException: Thrown when the user's modem or router is likely in need of defenestration (i.e. the connection to a remote server has failed)
 */

public class ModemDefenestrationException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public ModemDefenestrationException(String msg) {
		super(msg);
	}

}
