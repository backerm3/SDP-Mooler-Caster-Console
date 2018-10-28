/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * CachedAudio: Handles the caching of remote audio files
 */
package com.stereodustparticles.console.cache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.stereodustparticles.console.ables.Loadable;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.ui.DownloadMonitor;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;

public class CachedAudio {
	// Cache a file, then load it to the specified Loadable object
	public static void cacheAndLoad(URL src, Loadable dest) throws IOException {
		// If the input URL's protocol is HTTP...
		if ( src.getProtocol().equals("http") ) {
			// Extract the filename from the URL, then build a File object pointing to a file
			// of that name in the specified SDP spot cache directory
			// http://stackoverflow.com/questions/26508424/need-to-extract-filename-from-url
			String urlStr = src.toString();
			File cacheCandidate = new File(Prefs.loadString(Prefs.CACHE_DIR), urlStr.substring(urlStr.lastIndexOf('/') + 1));
			
			// If the file doesn't exist...
			if ( ! cacheCandidate.exists() ) {
				// Create the spot download directory (and its parents), if needed
				cacheCandidate.getParentFile().mkdirs();
				
				// Create a SpotDownloadMonitor and show it
				DownloadMonitor monitor = new DownloadMonitor(cacheCandidate.getName());
				
				// Connect to the SDP server
				HttpURLConnection connection = (HttpURLConnection)src.openConnection();
				
				// If we didn't get a 200 OK response, complain and return
				if ( connection.getResponseCode() != 200 ) {
					monitor.done();
					int responseCode = connection.getResponseCode();
					String responseMsg = connection.getResponseMessage();
					Platform.runLater(() -> Microwave.showError("File Download Error", "Got a " + responseCode + " " + responseMsg + " error from the remote server when attempting to download:\n\n" + src.toString() + "\n\nRemind the server owner of their One Job, then try again."));
					connection.disconnect();
					return;
				}
				
				// We're still here?  Good - let's grab the I/O streams
				InputStream download = connection.getInputStream();
				FileOutputStream save = new FileOutputStream(cacheCandidate);
				
				// Get total size and set it on the monitor window
				monitor.setFileSize(connection.getContentLength());
				
				// Loop through the input stream and write it to the file
				int bytesRead = -1;
				int totalBytesRead = 0;
	            byte[] buffer = new byte[4096]; // Set buffer size here!
	            while ((bytesRead = download.read(buffer)) != -1) {
	                save.write(buffer, 0, bytesRead);
	                if ( bytesRead > 0 ) {
		                totalBytesRead += bytesRead;
		                monitor.setProgress(totalBytesRead);
	                }
	            }
	            
	            // We're done, close things up
	            save.close();
	            download.close();
	            connection.disconnect();
	            monitor.done();
			}
			
			// Finally, load the file from the cache
			dest.load(cacheCandidate.toURI().toURL());
		}
		// Otherwise, just load directly from the input URL
		else {
			dest.load(src);
		}
	}
}
