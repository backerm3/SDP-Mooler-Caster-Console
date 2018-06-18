package com.stereodustparticles.console.soundboard;

import java.io.Serializable;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * SpotLoadRequest: Data structure used to store parameters for loading a spot to the soundboard
 */

public class SpotLoadRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String title;
	private String library;
	private String location;
	
	public SpotLoadRequest(String title, String library, String location) {
		this.title = title;
		this.library = library;
		this.location = location;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getLibrary() {
		return library;
	}
	
	public String getLocation() {
		return location;
	}
}
