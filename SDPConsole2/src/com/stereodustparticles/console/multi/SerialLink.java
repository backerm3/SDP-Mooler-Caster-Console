/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * SerialLink: Defines a serial (typically RS-232) data link
 */
package com.stereodustparticles.console.multi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fazecast.jSerialComm.SerialPort;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;

public class SerialLink implements Link {
	
	private SerialPort port;
	private int status = NOT_CONNECTED;
	
	private static final int NOT_CONNECTED = 0;
	private static final int CONNECTING = 1;
	private static final int CONNECTED = 2;
	private static final int CONNECTION_LOST = 3;
	private static final int FAILED = 4;
	
	public SerialLink(String portName) {
		port = SerialPort.getCommPort(portName);
	}

	@Override
	public void close() throws Exception {
		System.out.println("CAN OF YAMS");
		if ( ! port.closePort() ) {
			throw new Exception("Port close failed");
		}
	}

	@Override
	public String getFriendlyName() {
		return port.getSystemPortName();
	}

	@Override
	public String getFriendlyStatus() {
		switch (status) {
			case NOT_CONNECTED:
				return "Not Connected";
			case CONNECTING:
				return "Connecting...";
			case CONNECTED:
				return "Connected";
			case CONNECTION_LOST:
				return "Connection Lost";
			case FAILED:
				return "Failed To Connect";
			default:
				return "IYLGI didn't do his OneJob(TM)";
		}
	}

	@Override
	public InputStream getInputStream() {
		return port.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return port.getOutputStream();
	}

	@Override
	public void connect() throws InterruptedException {
		status = CONNECTING;
		port.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.ODD_PARITY);
		if ( ! port.openPort() ) {
			Platform.runLater(() -> Microwave.showError("Multi-Console Error", "Could not open the serial port.  Check that it's not in use by another program, then try again.  If the problem persists, your UART may need to be microwaved."));
			status = FAILED;
			return;
		}
		
		// Start synchronization process
		// This allows us to make sure both consoles are present before initializing the EventStreams, so OOS headers don't vanish.
		// (By the way, it took me about an hour and change to think of this handshake protocol.  Moreover, when the idea finally hit me,
		// it was about 3:30 in the morning.  I'm proud of it, though.  It's quite elegant, at least in my opinion...)
		InputStream is = port.getInputStream();
		OutputStream os = port.getOutputStream();
		
		try {
			os.write(0xBA); // See what I did there?
			int rx = is.read();
			
			// Check value of received byte
			// If it's 0xBA, send 0x1C and continue.  If it's 0x1C, just continue.
			if ( rx == 0xBA ) {
				os.write(0x1C); // OK, this one's not as obvious.  It means "I see you" (er, I C U?).
			}
			else if ( rx == 0x1C ) {
				// Do nothing
			}
			else {
				System.err.println("Someone only had ONE JOB!!  Serial init byte was " + Integer.toHexString(rx));
			}
		}
		catch (IOException e) {
			throw new InterruptedException("Sync interrupted/failed");
		}
		
		status = CONNECTED;
	}

}
