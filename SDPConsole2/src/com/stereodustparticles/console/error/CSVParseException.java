package com.stereodustparticles.console.error;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * CSVParseException: Thrown when a bad line is encountered in parsing a CSV library
 */

public class CSVParseException extends Exception {
	private static final long serialVersionUID = 1L;
	private String line;
	private String file = null;
	
	public CSVParseException(String line) {
		super("A malformed line was encountered while parsing the specified CSV library");
		this.line = line;
	}
	
	public String getOffendingLine() {
		return line;
	}
	
	public void setFile(String file) {
		this.file = file;
	}
	
	public String getFile() {
		return file;
	}
}
