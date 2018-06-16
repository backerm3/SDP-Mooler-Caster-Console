/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * SerialUtils: Wrangles serial ports for other classes
 */
package com.stereodustparticles.console.multi;

import com.fazecast.jSerialComm.SerialPort;

public class SerialUtils {
	private static SerialPort[] masterPortList = SerialPort.getCommPorts();
	
	// Return an array of serial ports (as strings) present on the machine
	public static String[] getPortNames() {
		if ( masterPortList == null || masterPortList.length == 0 ) {
			return null;
		}
		
		String[] ports = new String[masterPortList.length];
		for ( int i = 0; i < masterPortList.length; i++ ) {
			ports[i] = masterPortList[i].getSystemPortName();
		}
		return ports;
	}
	
	// Return the "default" (read: first) port (well, its name anyway)
	// Return null if an error occurs, since 9/10 times it's because we're on a weeny modern computer with no legacy ports!
	public static String getDefaultPort() {
		try {
			return masterPortList[0].getSystemPortName();
		}
		catch (Exception e) {
			return null;
		}
	}
}
