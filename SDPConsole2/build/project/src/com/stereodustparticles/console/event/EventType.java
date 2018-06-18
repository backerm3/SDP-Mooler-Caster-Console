package com.stereodustparticles.console.event;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * EventType: defines the different types of events that the Console's components can fire
 * Most of the entries should be pretty self-explanatory - comments below indicate the expected parameter type (Objects in the params field)
 */

public enum EventType {
	DECK_PLAYBACK_STARTED, // Integer - deck number
	DECK_PLAYBACK_STOPPED, // Integer - deck number
	DECK_PLAYBACK_ERROR, // Exception that was thrown
	DECK_COUNTER_TICK, // Integer - deck number
	DECK_PLAY_PRESSED, // Integer - deck number
	DECK_CUE_PRESSED, // Integer - deck number
	DECK_REQUEST_LOAD, // DeckLoadRequest object
	DECK_READY, // Deck number (int), confirmed duration (int)
	DECK_LOAD_ERROR, // Exception that was thrown, associated deck number (int)
	DECK_VOLUME_ADJUST, // Deck number (Integer), new volume level (Float)
	SNP_TRIGGER, // Number of the deck that *triggered* SnP (Integer)
	SPOT_PLAYBACK_STARTED, // Integers - Row, Column
	SPOT_PLAYBACK_STOPPED, // Integers - Row, Column
	SPOT_BUTTON_PRESSED, // Integers - Row, Column
	SPOT_REQUEST_LOAD, // SpotLoadRequest object
	SPOT_POS_CHOSEN, // SpotLoadRequest object, Row (int), Column (int)
	SPOT_READY, // Integers - Row, Column, Duration in tenths
	SPOT_CLEAR, // Integers - Row, Column
	SPOT_LOAD_ERROR, // Exception, Row, Column
	SPOT_COLOR_CHANGE, // Color string, Row, Column
	LIBRARY_LIST_UPDATED, // No params
	PLAYLIST_ADD, // PlaylistEntry to add
	PLAYLIST_MARK_PLAYED, // PlaylistEntry corresponding to song to mark played
	PLAYLIST_EDIT, // Index to edit (Integer), PlaylistEntry to put there
	PLAYLIST_MOVE_UP, // Index to move (Integer)
	PLAYLIST_MOVE_DOWN, // Index to move (Integer)
	PLAYLIST_DELETE, // Index to delete (Integer)
	PLAYLIST_CLEAR, // No params
	PLAYLIST_SYNC; // Playlist structure
}
