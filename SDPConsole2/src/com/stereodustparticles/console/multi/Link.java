/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Link: Defines the basic structure of an arbitrary data link for Multi-Console
 */
package com.stereodustparticles.console.multi;

import java.io.InputStream;
import java.io.OutputStream;

public interface Link extends AutoCloseable {
	public String getFriendlyName(); // Get a human-readable name for this link
	public String getFriendlyStatus(); // Get a human-readable description of this link's current status
	public InputStream getInputStream(); // Get this link's InputStream
	public OutputStream getOutputStream(); // Get this link's OutputStream
	public void connect() throws InterruptedException; // Start connecting
}
