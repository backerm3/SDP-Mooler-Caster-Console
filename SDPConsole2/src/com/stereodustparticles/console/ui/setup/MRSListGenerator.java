/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * MRSListGenerator: Generate LER MRS song lists from libraries
 */
package com.stereodustparticles.console.ui.setup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.library.Library;
import com.stereodustparticles.console.library.LibraryEntry;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MRSListGenerator {
	private Stage stage;
	private File dest;
	private Label cFile;
	private Label processed;
	private boolean stop = false;
	private int filesProcessed = 0;
	
	public MRSListGenerator(File output) {
		stage = new Stage();
		stage.setTitle("MRS Song List Generator");
		stage.setResizable(false);
		stage.setWidth(450);
		stage.initStyle(StageStyle.UTILITY);
		stage.setAlwaysOnTop(true);
		
		VBox root = new VBox(40);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(20, 25, 20, 25));
		
		cFile = new Label("Currently Processing:");
		processed = new Label("0 files processed");
		Button cancel = new Button("Cancel");
		root.getChildren().addAll(new Label("Generating MRS song list..."), cFile, processed, cancel);
		
		cancel.setOnAction((e) -> stop = true);
		stage.setOnCloseRequest((e) -> {
			e.consume();
			stop = true;
		});
		
		stage.setScene(new Scene(root));
		
		dest = output;
	}
	
	public void start() {
		stage.show();
		
		Utils.runInBackground(() -> {
			FileOutputStream fos = null;
			OutputStreamWriter osw = null;
			BufferedWriter bw = null;
			
			try {
				// Create the file if it doesn't exist yet
				if ( ! dest.exists() ) {
					dest.createNewFile();
				}

				fos = new FileOutputStream(dest.getAbsoluteFile(), false);
				osw = new OutputStreamWriter(fos, "UTF-8");
				bw = new BufferedWriter(osw);

				for ( String libName : LibraryManager.getAvailableLibraries() ) {
					Library lib = LibraryManager.getLibraryForName(libName);
					
					// Reset library to root (fixes "song list of only one folder" bug)
					lib.backToRoot();
					
					// If library doesn't have inclusion in song lists enabled, skip it
					if ( ! lib.includeInSongLists() ) {
						continue;
					}
					
					try {
						traverseDirectory(lib, bw);
					}
					catch (Exception e) {
						Platform.runLater(() -> Microwave.showException("Library Error", "An error occurred while reading library \"" + libName + "\".  A clubbing is most definitely in order.", e, stage));
						stop = true;
					}
					
					if ( stop ) break;
				}
			}
			catch (IOException e) {
				Platform.runLater(() -> {
					Microwave.showException("Error Generating Song List", "An error occurred while writing the output file.  Check that your selected file is writable, then try again.\n\nIf the problem persists, a trip to the pool is definitely in order.", e, stage);
					stage.close();
				});
			}
			finally {
				try {
					if (bw != null) {
						bw.close();
					}
					if (osw != null) {
						osw.close();
					}
					if (fos != null) {
						fos.close();
					}
				}
				catch (IOException e) {
					System.err.println("I'm having really bad luck today - I couldn't even close the file!");
					e.printStackTrace();
				}
			}
			
			Platform.runLater(() -> {
				if ( ! stop ) {
					Microwave.showInfo("Success!", "MRS song list has been successfully generated.\n\n" + filesProcessed + " entries were included.", stage);
				}
				stage.close();
			});
		});
	}
	
	private void traverseDirectory(Library lib, BufferedWriter bw) throws Exception {
		List<LibraryEntry> dirContents = lib.getList();
		for ( LibraryEntry entry : dirContents ) {
			Platform.runLater(() -> cFile.setText("Currently Processing: " + entry.toString()));
			
			if ( entry.isDir() ) {
				lib.changeDir(entry.getTitle());
				traverseDirectory(lib, bw);
				lib.upOneLevel();
			}
			else {
				String line = entry.toMRSData();
				if ( line != null ) {
					bw.write(line + "\r\n");
					filesProcessed++;
					Platform.runLater(() -> processed.setText(filesProcessed + " files processed"));
				}
			}
			
			if ( stop ) break;
		}
	}
}
