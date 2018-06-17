package com.stereodustparticles.console.deck;

import java.io.Serializable;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * DeckLoadRequest: data structure for the parameters for deck load events
 */

public class DeckLoadRequest implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String title;
	private String artist;
	private int duration;
	private String library;
	private String location;
	private int deckNum;
	private int requestID = 0;
	
	public DeckLoadRequest(String title, String artist, int duration, String library, String location, int deckNum) {
		this.title = title;
		this.artist = artist;
		this.duration = duration;
		this.library = library;
		this.location = location;
		this.deckNum = deckNum;
	}
	
	public DeckLoadRequest(String title, String artist, int duration, String library, String location, int deckNum, int requestID) {
		this(title, artist, duration, library, location, deckNum);
		this.requestID = requestID;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getArtist() {
		return artist;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public String getLibrary() {
		return library;
	}
	
	public String getLocation() {
		return location;
	}
	
	public int getDeckNum() {
		return deckNum;
	}
	
	public int getRequestID() {
		return requestID;
	}
}
