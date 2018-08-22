package edu.rochester.cif.grimreaper.readers.elcom;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import com.fazecast.jSerialComm.SerialPort;

import edu.rochester.cif.grimreaper.readers.CardReader;
import edu.rochester.cif.grimreaper.readers.ReaderStatus;
import edu.rochester.cif.grimreaper.readers.StatusChangedCallback;

// GrimReaper - Access controller for the CIF lab
// Written by Ben Ackerman '18 - Summer 2017

// This class (and the rest of its package) defines the Elcom MAG-742/MAG-7042 (polled RS-485) reader type

public class ElcomCardReader implements CardReader {

	private ElcomDataLink link = null;
	private Timer pollTimer = null;
	private ReaderStatus status;
	private StatusChangedCallback callback = null;
	
	@Override
	public void grantAccess() {
		String res = link.sendCommand("OA");
		updateStatus(res);
	}

	@Override
	public void denyAccess() {
		String res = link.sendCommand("OD");
		updateStatus(res);
	}

	@Override
	public void registerStatusChangedCallback(StatusChangedCallback callback) {
		this.callback = callback;
	}

	@Override
	public String getID() {
		return link.sendCommand("R");
	}

	@Override
	public void parseReaderParams(Properties config) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		pollTimer.cancel();
		link.close();
	}

	@Override
	public void open() {
		// Open the data link (TODO configurable params)
		link = new ElcomDataLink();
		link.open("COM3", 9600, 7, SerialPort.ONE_STOP_BIT, SerialPort.ODD_PARITY, SerialPort.FLOW_CONTROL_DISABLED);
		status = ReaderStatus.IDLE;
		
		// Set up polling
		pollTimer = new Timer();
		TimerTask pollFunc = new TimerTask() {
			@Override
			public void run() {
				// Query reader status (if no card is waiting)
				if ( status != ReaderStatus.CARD_WAITING ) {
					String statusChars = link.sendCommand("?");
					updateStatus(statusChars);
				}
			}
		};
		// TODO make poll freq configurable
		pollTimer.schedule(pollFunc, 0, 500);
	}
	
	// Convert the reader's response to a reader state, and run the status-changed callback if necessary
	private void updateStatus(String statusChars) {
		// DEBUG
		//System.out.println(statusChars);
		
		// Default to "idle" state
		ReaderStatus newStatus = ReaderStatus.IDLE;
		
		// If no status characters were received, our link has been lost
		if ( statusChars.equals("") ) {
			newStatus = ReaderStatus.LINK_LOST;
		}
		// Is there a card in memory?
		else if ( hasChar("D", statusChars) ) {
			newStatus = ReaderStatus.CARD_WAITING;
		}
		// Has the tamper switch been tripped?
		else if ( hasChar("T", statusChars) ) {
			newStatus = ReaderStatus.TAMPER;
		}
		// Is the unit reporting that there was a power failure?
		else if ( hasChar("P", statusChars) ) {
			newStatus = ReaderStatus.RECOVERED_FROM_POWER_FAILURE;
		}
		/*switch (statusChars) {
			case "S":
				newStatus = ReaderStatus.IDLE;
				break;
			case "SP":
				newStatus = ReaderStatus.RECOVERED_FROM_POWER_FAILURE;
				break;
			case "SD":
			case "SFD":
			case "SOD":
				newStatus = ReaderStatus.CARD_WAITING;
				break;
			case "SF":
				newStatus = ReaderStatus.FORCED_OPEN;
				break;
			case "ST":
				newStatus = ReaderStatus.TAMPER;
				break;
			case "":
				newStatus = ReaderStatus.LINK_LOST;
				break;
			default:
				System.err.println("(!) [ElcomCardReader] Unknown response code \"" + statusChars + "\"");
				newStatus = ReaderStatus.IDLE;
				break;
		}*/
		
		if ( newStatus != status ) {
			status = newStatus;
			if ( callback != null ) {
				callback.statusChanged(newStatus);
			}
		}
	}
	
	// Utility function to check if given character exists in given string
	private boolean hasChar(String needle, String haystack) {
		return (haystack.indexOf(needle) >= 0);
	}

}
