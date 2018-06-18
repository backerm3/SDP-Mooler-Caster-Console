/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * MRSPasswordException: Thrown when an MRS access fails due to a login problem
 */
package com.stereodustparticles.console.error;

public class MRSException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public MRSException(String msg) {
		super(msg);
	}

}
