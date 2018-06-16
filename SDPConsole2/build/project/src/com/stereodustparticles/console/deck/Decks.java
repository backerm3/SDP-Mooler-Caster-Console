/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Decks: Houses the actual instances of the Deck class
 */
package com.stereodustparticles.console.deck;

import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.multi.MultiConsole;

public class Decks {
	public static Deck deck1 = null;
	public static Deck deck2 = null;
	
	private static boolean snpEnabled = false;
	
	// These booleans only operate in the Slave mode
	private static boolean deck1Playing = false;
	private static boolean deck2Playing = false;
	
	public static void init() {
		// Don't initialize backend in MC Slave mode
		if ( MultiConsole.getDeckMode() != 'S' ) {
			deck1 = new Deck(1);
			deck2 = new Deck(2);
		}
		else {
			EventBus.registerListener(EventType.DECK_PLAYBACK_STARTED, (e) -> {
				int deckNum = ((Integer)e.getParams()[0]).intValue();
				
				if ( deckNum == 1 ) {
					deck1Playing = true;
				}
				
				else if ( deckNum == 2 ) {
					deck2Playing = true;
				}
			});
			
			EventBus.registerListener(EventType.DECK_PLAYBACK_STOPPED, (e) -> {
				int deckNum = ((Integer)e.getParams()[0]).intValue();
				
				if ( deckNum == 1 ) {
					deck1Playing = false;
				}
				
				else if ( deckNum == 2 ) {
					deck2Playing = false;
				}
			});
		}
	}
	
	public static void cleanUp() {
		if ( deck1 != null ) {
			deck1.cleanUp();
		}
		if ( deck2 != null ) {
			deck2.cleanUp();
		}
	}
	
	public static boolean deck1IsPlaying() {
		if ( MultiConsole.getDeckMode() == 'S' ) {
			return deck1Playing;
		}
		else {
			return deck1.isPlaying();
		}
	}
	
	public static boolean deck2IsPlaying() {
		if ( MultiConsole.getDeckMode() == 'S' ) {
			return deck2Playing;
		}
		else {
			return deck2.isPlaying();
		}
	}
	
	// Enable/disable Stream 'n' Poop(TM)
	public static void setSNPEnabled(boolean enable) {
		snpEnabled = enable;
	}
	
	// Return the current state of Stream 'n' Poop(TM)
	public static boolean snpIsEnabled() {
		return snpEnabled;
	}
}
