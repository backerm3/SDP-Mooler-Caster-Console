package com.stereodustparticles.console.error;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * HTTPException: thrown when CSV loading fails due to a non-200 response code from the server
 */

public class HTTPException extends Exception {
	private static final long serialVersionUID = 1L;
	private String file;
	
	public HTTPException(String file, String httpMessage) {
		super(httpMessage);
		this.file = file;
	}
	
	public String getOffendingFile() {
		return file;
	}
}
