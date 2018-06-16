/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * DTC Playout Engine
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * ReadThread: Defines the thread that reads data into the ring buffer
 */
package com.stereodustparticles.console.deck;

import java.io.IOException;
import java.io.OutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;

public class ReadThread extends Thread {
	
	private AudioInputStream audio = null;
	private Deck deck;
	private CircularByteBuffer ringBuffer;
	private OutputStream rbOut;
	private boolean cued = false;
	private int remainingTime;
	private int bytesPerTenth;
	private float peak = 0f;
	private boolean snpArmed = false;
	
	// Constants
	private static final float SNP_TRIP_POINT = 0.12f;
	private static final int SNP_WINDOW = 170; // tenths of seconds
	private static final float CUE_THRESHOLD = 0.01f;

	public ReadThread(Deck deck) {
		this.deck = deck;
		start();
	}
	
	// Set the audio input stream to read from
	// Also set the confirmed duration of the track (which we will perform further calculations on later)
	public synchronized void setSource(AudioInputStream audio, int duration) {
		this.audio = audio;
		this.remainingTime = duration;
		
		peak = 0f;
		snpArmed = false;
		
		// Get info about the stream for use in calculating the current time position (for time display)
		AudioFormat format = audio.getFormat();		
		bytesPerTenth = ((int)format.getSampleRate() / 10) * (format.getSampleSizeInBits() / 8) * format.getChannels();
		
		ringBuffer = deck.getRingBuffer();
		rbOut = ringBuffer.getOutputStream();
		cued = false;
	}
	
	@Override
	public void run() {
		byte[] readBuffer = new byte[4096];
		
		int nBytesRead = 0;
		int byteCounter = 0;
		
		while (true) {
			try {
				// If the deck is closing, break
				if ( deck.isClosing() ) {
					break;
				}
				
				// If no file has been loaded yet, wait for that to happen
				if ( audio == null ) {
					waitForNewFile();
					continue;
				}
				
	    		synchronized (this) {
	    			nBytesRead = audio.read(readBuffer, 0, readBuffer.length);
		    		
		    		// If we got EOF, wait for a new file to be loaded
		    		if ( nBytesRead == -1 ) {
		    			deck.notifyEOF();
		    			waitForNewFile();
		    		}
		    		
		    		// Write the data we got (if any) into the ring buffer, assuming we've reached music
		    		else if ( nBytesRead > 0 ) {
	    				byteCounter += nBytesRead;
	    				
	    				if ( byteCounter >= bytesPerTenth ) {
	    					remainingTime--;
	    					byteCounter -= bytesPerTenth;
	    				}
	    				
		    			if ( ! cued ) {
		    				if ( AudioKungFu.getPeak(readBuffer, nBytesRead, audio.getFormat()) > CUE_THRESHOLD ) {
		    					cued = true;
		    					
		    					// Fire a Deck Ready event
		    					deck.notifyReady();
		    					EventBus.fireEvent(new Event(EventType.DECK_READY, deck.getNumber(), remainingTime));
		    				}
		    				else {
		    					continue;
		    				}
			    		}
		    			
		    			rbOut.write(readBuffer, 0, nBytesRead);
		    			
		    			// Do Stream 'n' Poop(TM) stuff if we have to
		    			if ( Decks.snpIsEnabled() ) {
		    				float samplePeak = AudioKungFu.getPeak(readBuffer, nBytesRead, audio.getFormat());
							
							// If the peak of the current sample is higher than our record for the track, make it our new record
							if ( samplePeak > peak ) {
								peak = samplePeak;
							}
							
							if ( remainingTime < SNP_WINDOW && samplePeak < (SNP_TRIP_POINT * peak) ) {
								if ( ! snpArmed ) {
									ringBuffer.dropSNPMarker();
									snpArmed = true;
								}
							}
							else if ( snpArmed ) {
								ringBuffer.pickUpSNPMarker();
								snpArmed = false;
							}
		    			}
		    		}
	    		}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
	}
	
	// Because I'm lazy and don't feel like typing "synchronized" a million times...
	private synchronized void waitForNewFile() throws InterruptedException {
		wait();
	}
}
