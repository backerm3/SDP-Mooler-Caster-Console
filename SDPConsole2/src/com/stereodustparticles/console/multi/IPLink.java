/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * IPLink: Defines an IP-based link, either outbound or already-established inbound
 */
package com.stereodustparticles.console.multi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class IPLink implements Link {
	
	// Status constants
	private static final int NOT_CONNECTED = 0;
	private static final int CONNECTING = 1;
	private static final int CONNECTED = 2;
	private static final int WAIT_RETRY = 3;
	private static final int CONNECTION_LOST = 4;
	private static final int DITCHED = 5;
	
	// Instance variables
	private Socket socket = null;
	private Socket connSocket = null;
	private int status = NOT_CONNECTED;
	private String ip = null;
	private int port = 0;
	
	// Constructor for outbound links
	public IPLink(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	// Constructor for pre-existing (presumably inbound) links
	public IPLink(Socket sock) {
		socket = sock;
		status = CONNECTED;
	}

	@Override
	public void close() throws Exception {
		if ( socket != null ) {
			socket.close();
		}

		if ( connSocket != null ) {
			connSocket.close();
		}
	}

	@Override
	public String getFriendlyName() {
		if ( socket == null || socket.getInetAddress() == null ) {
			return "(Unknown)";
		}
		else {
			return socket.getInetAddress().getHostAddress();
		}
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
			case WAIT_RETRY:
				return "Connection failed, waiting to retry";
			case CONNECTION_LOST:
				return "Connection Lost";
			case DITCHED:
				return "Disconnected Externally";
			default:
				return "IYLGI didn't do his OneJob(TM)";
		}
	}
	
	// Repeatedly try to connect a socket until we succeed
	private Socket connectSocket(String ip, int port) throws InterruptedException {
		while (true) {
			status = CONNECTING;
			connSocket = new Socket();
			try {
				connSocket.connect(new InetSocketAddress(ip, port));
			}
			catch ( Exception e ) {
				if ( connSocket.isClosed() ) {
					throw new InterruptedException("Link is closing");
				}
				
				status = WAIT_RETRY;
				Thread.sleep(2000); // TODO make this retry delay adjustable?
				continue;
			}
			
			status = CONNECTED;
			return connSocket;
		}
	}

	@Override
	public InputStream getInputStream() {
		try {
			return socket.getInputStream();
		}
		catch (IOException e) {
			// TODO Improve?
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public OutputStream getOutputStream() {
		try {
			return socket.getOutputStream();
		}
		catch (IOException e) {
			// TODO Improve?
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public void connect() throws InterruptedException {
		socket = connectSocket(ip, port);
	}
}
