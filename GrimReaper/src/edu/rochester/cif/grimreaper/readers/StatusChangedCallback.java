package edu.rochester.cif.grimreaper.readers;

// GrimReaper - Access controller for the CIF lab
// Written by Ben Ackerman '18 - Summer 2017

// This interface is used to build the callback that gets executed upon a change in the reader's status

public interface StatusChangedCallback {
	// This is the method that gets executed when a status change occurs
	// Takes the new status as its parameter
	void statusChanged(ReaderStatus newStatus);
}
