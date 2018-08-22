package edu.rochester.cif.grimreaper.readers.elcom;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.fazecast.jSerialComm.SerialPort;

// GrimReaper - Access controller for the CIF lab
// Written by Ben Ackerman '18 - Summer 2017

// This class handles communication with an Elcom card reader

public class ElcomDataLink implements Closeable {
	
	private final String unitAddress = "22"; // TODO make configurable
	private SerialPort comPort = null;
	private BufferedReader comPortReader = null;
	private Charset readerCharset = Charset.forName("US-ASCII");
	
	// Open the serial port
	public void open(String port, int baud, int dataBits, int stopBits, int parity, int flowCtrl) {
		comPort = SerialPort.getCommPort(port);
		comPort.setComPortParameters(baud, dataBits, stopBits, parity);
		comPort.setFlowControl(flowCtrl);
		comPort.openPort();
		comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0); // TODO make timeout configurable
		
		// Get reader
		comPortReader = new BufferedReader(new InputStreamReader(comPort.getInputStream()));
	}
	
	// Close the serial port
	public void close() {
		try {
			comPortReader.close();
		} catch (IOException e) {
			System.err.println("(!) [ElcomDataLink] Failed to close the serial port:");
			e.printStackTrace();
		}
		comPort.closePort();
	}
	
	// Send a command to the reader, return its response
	// Throws an exception when a timeout occurs
	public String sendCommand(String cmdChars) {
		byte[] cmd = buildCommand(cmdChars);
		comPort.writeBytes(cmd, cmd.length);
		String response = "";
		try {
			response = comPortReader.readLine();
		}
		catch ( IOException e ) {
			System.err.println("(!) [ElcomDataLink] Failed to read from serial port");
			return "";
		}
		// Shave off first 2 characters (the address) before returning
		// TODO maybe verify correct address?
		if ( response.length() > 2 ) {
			return response.substring(2);
		}
		else {
			return "";
		}
	}
	
	// Internal method for building a command string and converting it to a byte array
	private byte[] buildCommand(String cmdChars) {
		String cmd = "#" + unitAddress + cmdChars + "\r";
		return cmd.getBytes(readerCharset);
	}

}
