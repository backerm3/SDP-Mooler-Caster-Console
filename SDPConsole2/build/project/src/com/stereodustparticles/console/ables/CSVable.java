/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * CSVable: Defines the methods a class needs to implement to be able to load/save to CSV files
 */
package com.stereodustparticles.console.ables;

public interface CSVable {
	public String toCSVRow(); // Return a CSV row corresponding to this object
}
