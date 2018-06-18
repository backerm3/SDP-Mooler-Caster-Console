/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Loadable: Used to denote that a class implements something that can be "loaded", e.g. a soundboard slot or deck
 */
package com.stereodustparticles.console.ables;

import java.net.URL;

public interface Loadable {
	public void load(URL file);
}
