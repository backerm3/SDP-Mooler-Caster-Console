package com.stereodustparticles.console;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import com.stereodustparticles.console.ables.CSVable;

import javafx.scene.paint.Color;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Utils: contains application-wide utility methods
 */

public class Utils {
	// Convert tenths of seconds to a human-readable string (xx:xx.x)
	public static String tenthsToString(int time) {
		// If we have negative time, just return zero
		if ( time < 0 ) {
			return "0:00.0";
		}
		
		int tenths = time % 10;
		int sec = (time / 10) % 60;
		int min = (time / 10) / 60;
		return String.format("%d:%02d.%d", min, sec, tenths);
	}
	
	// Convert tenths of seconds to a less-precise human-readable string (xx:xx)
	public static String tenthsToStringShort(int time) {
		// If we have negative time, just return zero
		if ( time < 0 ) {
			return "0:00";
		}
		
		int sec = (time / 10) % 60;
		int min = (time / 10) / 60;
		return String.format("%d:%02d", min, sec);
	}
	
	// Execute something (anything!) in the background
	public static void runInBackground(Runnable r) {
		new Thread(r).start();
	}
	
	// Get the web color string for the given color
	// From https://stackoverflow.com/questions/17925318/how-to-get-hex-web-string-from-javafx-colorpicker-color
	public static String getWebColor(Color color) {
        return String.format( "#%02X%02X%02X",
            (int)( color.getRed() * 255 ),
            (int)( color.getGreen() * 255 ),
            (int)( color.getBlue() * 255 ) );
    }
	
	// Generate the contents of the Memory Info box
	public static String getMemoryInfo() {
		Runtime rt = Runtime.getRuntime();
		int maxMemory = (int)(rt.maxMemory() / (1024 * 1024)); // Max memory in MB
		int currentTotalMemory = (int)(rt.totalMemory() / (1024 * 1024)); // Total memory in MB
		double freeMemory = (double)(((int)rt.freeMemory()) / (1024.0 * 1024.0)); // Free memory in MB with decimal places
		return "Current Memory (Heap) Size: " + currentTotalMemory + " MB" + 
				"\nMaximum Memory Size: " + maxMemory + " MB" +
				"\nMemory Free: " + String.format("%.1f", freeMemory) + " MB" +
				"\n\n(Reserved areas not included)";
	}
	
	// Write out a CSV file from a collection of CSVable objects
	public static void writeCSV(Collection<? extends CSVable> src, File to) throws IOException {
		final String SYSTEM_LINE_END = System.getProperty("line.separator");
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		
		try {
			// Create the file if it doesn't exist yet
			if ( ! to.exists() ) {
				to.createNewFile();
			}

			fw = new FileWriter(to.getAbsoluteFile(), false);
			bw = new BufferedWriter(fw);

			synchronized (src) {
				for ( CSVable obj : src ) {
					bw.write(obj.toCSVRow() + SYSTEM_LINE_END);
				}
			}
		}
		finally {
			try {
				if (bw != null) {
					bw.close();
				}
				if (fw != null) {
					fw.close();
				}
			}
			catch (IOException e) {
				System.err.println("I'm having really bad luck today - I couldn't even close the CSV file!");
				e.printStackTrace();
			}
		}
	}
	
	// Write out a CSV file from a 2D array of CSVable objects
	public static void writeCSV2D(CSVable[][] src, File to) throws IOException {
		final String SYSTEM_LINE_END = System.getProperty("line.separator");
		
		FileWriter fw = null;
		BufferedWriter bw = null;
		
		try {
			// Create the file if it doesn't exist yet
			if ( ! to.exists() ) {
				to.createNewFile();
			}

			fw = new FileWriter(to.getAbsoluteFile(), false);
			bw = new BufferedWriter(fw);

			synchronized (src) {
				for ( CSVable[] row : src ) {
					for ( CSVable obj : row ) {
						bw.write(obj.toCSVRow() + SYSTEM_LINE_END);
					}
				}
			}
		}
		finally {
			try {
				if (bw != null) {
					bw.close();
				}
				if (fw != null) {
					fw.close();
				}
			}
			catch (IOException e) {
				System.err.println("I'm having really bad luck today - I couldn't even close the CSV file!");
				e.printStackTrace();
			}
		}
	}
	
	// Get the extension from a File object
	// https://www.journaldev.com/842/how-to-get-file-extension-in-java
	public static String getFileExtension(File file) {
        String fileName = file.getName();
        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
        return fileName.substring(fileName.lastIndexOf(".")+1).toLowerCase();
        else return "";
    }
	
	// Check if the input string is a valid IP address
	// https://stackoverflow.com/questions/4581877/validating-ipv4-string-in-java
	public static boolean validIP(String ip) {
	    try {
	        if ( ip == null || ip.isEmpty() ) {
	            return false;
	        }

	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) {
	            return false;
	        }

	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) {
	                return false;
	            }
	        }
	        if ( ip.endsWith(".") ) {
	            return false;
	        }

	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
	
	// Sanitize a string for use in the MRS
	public static String sanitizeForMRS(String in) {
		return in.replaceAll("[\"\u2018\u2019\u201c\u201d]", "'").replaceAll("[\u2026\u22ef]", "...").replaceAll("[\u2010\u2011\u2012\u2013\u2014\u2015]", "-").replaceAll("[&+]", "and");
	}
}
