package edu.rochester.cif.grimreaper.readers;

import java.io.Closeable;
import java.util.Properties;

// GrimReaper - Access controller for the CIF lab
// Written by Ben Ackerman '18 - Summer 2017

// CardReader interface - defines specifications for card reader classes
// This allows classes for new types of readers to be added without changing a million things

public interface CardReader extends Closeable {
	// This method is called when an ID has been verified and the door needs to be unlocked
	void grantAccess();
	
	// This method is called when an ID is rejected, and the reader needs to flash its red light
	void denyAccess();
	
	// Call this method to register your "status changed" callback function on the reader
	void registerStatusChangedCallback(StatusChangedCallback callback);
	
	// This method returns the ID that the reader has in memory, awaiting verification
	String getID();
	
	// Use this to pass the reader-specific parameters from the config file
	void parseReaderParams(Properties config);
	
	// When all is ready to start, call this to open the connection to the reader
	void open();
}
