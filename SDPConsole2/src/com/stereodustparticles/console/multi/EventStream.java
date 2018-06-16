/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * EventStream: Wrangles the object I/O streams used to send/receive events over a Link
 */
package com.stereodustparticles.console.multi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.NotSerializableException;
import java.io.InvalidClassException;
import java.io.StreamCorruptedException;
import java.io.OptionalDataException;
import java.io.EOFException;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;

public class EventStream implements AutoCloseable {
	private Link link = null;
	private ObjectOutputStream oos = null;
	private ObjectInputStream ois = null;
	private Thread rxThread = null;
	private Thread txThread = null;
	private String remoteID = null;
	private BlockingQueue<Event> txQueue;
	private boolean closing = false;
	
	// Runnable used by the receiver thread
	private final Runnable rxRun = () -> {
		// Sanity check
		if ( link == null ) return;
		
		try {
			ois = new ObjectInputStream(link.getInputStream());
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		// Prepare to start listening for events
		while (true) {
			Event incoming;
			try {
				incoming = (Event) ois.readObject();
			}
			catch (ClassNotFoundException e) {
				MultiConsole.displayClassMismatchError(getFriendlyName());
				return;
			}
			catch (InvalidClassException e) { // Problem with an object being sent
				Platform.runLater(() -> Microwave.showException("Multi-Console Error", "An internal error occurred while decoding an incoming event.  Either something requires percussive maintenance, or IfYouLikeGoodIdeas has done something very dumb.", e));
				return;
			}
			catch (StreamCorruptedException | OptionalDataException e) { // Data corruption
				Platform.runLater(() -> Microwave.showException("Multi-Console Error", "The incoming event stream has become corrupt (and has been impeached).  A computer, piece of network equipment, or other device involved may need to be microwaved.", e));
				return;
			}
			catch (EOFException e) { // Usually an intentional disconnect - only log to console
				System.err.println("EOF received, stopping event stream");
				Utils.runInBackground(() -> MultiConsole.bringOutYourDead(this));
				return;
			}
			catch (IOException e) { // Link failure - suppress message if we're closing the link
				if ( ! closing ) {
					Platform.runLater(() -> Microwave.showException("Multi-Console Error", "A network error occurred while reading an event from another console.  Something is likely in need of a prompt microwaving.\n\nIf this was an outbound connection, we will attempt to restore it.", e));
					Utils.runInBackground(() -> MultiConsole.bringOutYourDead(this));
				}
				return;
			}
			
			// We should have an event now, let's start processing it
			
			// If we don't have the remote console's ID yet, store it
			if ( remoteID == null ) {
				remoteID = incoming.getOriginator();
			}
			
			// Forward the event to other consoles
			MultiConsole.forwardEvent(incoming, this);
			
			// Fire the event locally
			EventBus.fireEvent(incoming);
			
			// Rinse, repeat!
		}
	};
	
	// Runnable used by the transmit thread
	private final Runnable txRun = () -> {
		// Sanity check
		if ( link == null ) return;
		
		try {
			oos = new ObjectOutputStream(link.getOutputStream());
			oos.flush();
		}
		catch (IOException e1) { // Problem with the underlying link
			Platform.runLater(() -> Microwave.showException("Multi-Console Error", "A network error occurred while initializing the network link.  Something likely needs to be microwaved.", e1));
			Utils.runInBackground(() -> MultiConsole.bringOutYourDead(this));
			return;
		}
		
		// Initialize queue and wait for events to arrive
		txQueue = new LinkedBlockingQueue<Event>();
		
		while (true) {
			try {
				Event nextEvent = txQueue.take();
				
				try {
					oos.writeObject(nextEvent);
				}
				catch (NotSerializableException | InvalidClassException e) { // Problem with an object being sent
					Platform.runLater(() -> Microwave.showException("Multi-Console Error", "An internal error occurred while forwarding an event to another console.  IfYouLikeGoodIdeas must report to the Red Courtesy Club immediately, if not sooner.", e));
					return;
				}
				catch (IOException e1) { // Problem with the underlying link
					if ( ! closing ) {
						Platform.runLater(() -> Microwave.showException("Multi-Console Error", "A network error occurred while attempting to forward an event to another console.  Something is likely in need of a prompt microwaving.\n\nIf this was an outbound connection, we will attempt to restore it.", e1));
						Utils.runInBackground(() -> MultiConsole.bringOutYourDead(this));
					}
					return;
				}
			}
			catch (InterruptedException e) {
				// We've been interrupted - exit
				return;
			}
		}
	};
	
	// Constructor
	public EventStream(Link lk) {
		link = lk;
		rxThread = new Thread(rxRun);
		txThread = new Thread(txRun);
		txThread.start();
		rxThread.start();
	}
	
	// Send an event
	public synchronized void sendEvent(Event e) {
		try {
			txQueue.put(e);
		}
		catch (InterruptedException e1) {
			System.out.println("Event transmission interrupted");
			return;
		}
	}

	@Override
	public void close() throws Exception {
		closing = true;
		
		if ( txThread.isAlive() ) {
			txThread.interrupt();
		}
		
		if ( rxThread.isAlive() ) {
			rxThread.interrupt();
		}
		
		oos.close();
		ois.close();
		link.close();
	}
	
	// Get a friendly name for this stream
	public String getFriendlyName() {
		if ( remoteID == null ) {
			return link.getFriendlyName();
		}
		else {
			return remoteID;
		}
	}
	
	// Get friendly status (of the underlying link)
	public String getFriendlyStatus() {
		return link.getFriendlyStatus();
	}
	
	// Return our link
	public Link getLink() {
		return link;
	}
}
