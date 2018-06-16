/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * DTC Playout Engine
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * WriteThread: Defines the thread that writes data from the ring buffer out to
 * the sound card
 */
package com.stereodustparticles.console.deck;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;

public class WriteThread extends Thread {
	private SourceDataLine line = null;
	private Deck deck;
	private int bytesPerTenth;
	private int byteCounter;
	private boolean eofReached = false;
	private CircularByteBuffer ringBuffer;
	private InputStream rbIn;
	private boolean snpTripped = false;

	public WriteThread(Deck deck) {
		this.deck = deck;
		
		start();
	}
	
	// Set the output line that will be written to
	public synchronized void setLine(SourceDataLine line) {
		this.line = line;
		
		snpTripped = false;
		
		// Get info about the stream for use in calculating the current time position (for time display)
		AudioFormat format = line.getFormat();		
		bytesPerTenth = ((int)format.getSampleRate() / 10) * (format.getSampleSizeInBits() / 8) * format.getChannels();
		
		byteCounter = 0;
		eofReached = false;
		
		ringBuffer = deck.getRingBuffer();
		rbIn = ringBuffer.getInputStream();
	}
	
	@Override
	public void run() {
		byte[] playBuffer = new byte[4096];
		
		int nBytesRetrieved = 0;
		int nBytesWritten = 0;
		
		while (true) {
			try {
				// If the deck is closing, break
				if ( deck.isClosing() ) {
					break;
				}
				
				synchronized (this) {
					if (! deck.isPlaying()) {
						if ( line != null ) {
							synchronized (line) {
								// Drain the line and stop
							    line.drain();
							    line.stop();
							}
						    
						    // Fire the deck stopped event
						    EventBus.fireEvent(new Event(EventType.DECK_PLAYBACK_STOPPED, deck.getNumber()));
						    
						    // If Stream 'n' Poop(TM) is running, load the next track (if there is one)
							if ( Decks.snpIsEnabled() ) {
								// If Stream 'n' Poop(TM) has not tripped by this point, trip it now
								if ( ! snpTripped ) {
									snpTripped = true;
									
									EventBus.fireEvent(new Event(EventType.SNP_TRIGGER, deck.getNumber()));
								}
								
								Utils.runInBackground(() -> {
									// Wait a moment so that SnP doesn't load next tentative before the upcoming track is marked played
									// TODO this solution is really lame
									try {
										Thread.sleep(500);
									}
									catch (InterruptedException e) {
										// This shouldn't happen
										e.printStackTrace();
									}
									
									deck.autoLoad();
								});
							}
						}
						
						waitUntilPlaying();
						
						if ( deck.isPlaying() ) {				
							line.start();
							
							// Fire a deck playback started event
							EventBus.fireEvent(new Event(EventType.DECK_PLAYBACK_STARTED, deck.getNumber()));
						}
						else {
							continue;
						}
					}
					
					if ( eofReached && rbIn.available() == 0 ) {
						deck.notifyAudioPoof();
						continue;
					}
					nBytesRetrieved = rbIn.read(playBuffer, 0, playBuffer.length);
	    			nBytesWritten = line.write(playBuffer, 0, nBytesRetrieved);
	    			
	    			// Add the number of bytes written to our running tally
					byteCounter += nBytesWritten;
					
					// If our running tally has reached a tenth of a second (or more) worth of data...
					if ( byteCounter > bytesPerTenth ) {
						// Decrement a tenth of a second off our remaining time
						EventBus.fireEvent(new Event(EventType.DECK_COUNTER_TICK, deck.getNumber()));
						
						// Subtract that tenth of a second from our byte count
						byteCounter -= bytesPerTenth;
					}
					
					// Do Stream 'n' Poop(TM)-related stuff if we have to
					if ( Decks.snpIsEnabled() && ! snpTripped ) {
						if ( ringBuffer.snpTripped() ) {
							snpTripped = true;
							
							EventBus.fireEvent(new Event(EventType.SNP_TRIGGER, deck.getNumber()));
						}
					}
				}
			}
			catch (InterruptedException e) {
				// TODO figure out what lands us here
				e.printStackTrace();
				break;
			}
			catch (IOException e) {
				// TODO same here
	        	e.printStackTrace();
			}
		}
	}
	
	// Once again, I'm freakin' lazy...
	private synchronized void waitUntilPlaying() throws InterruptedException {
		wait();
	}
	
	// Notify this thread that EOF has been reached in the read thread
	protected void notifyEOF() {
		eofReached = true;
	}
}
