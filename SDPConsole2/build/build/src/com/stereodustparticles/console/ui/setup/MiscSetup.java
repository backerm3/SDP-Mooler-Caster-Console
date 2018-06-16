/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * SoundboardSetup: Defines the Soundboard Preferences dialog
 */
package com.stereodustparticles.console.ui.setup;

import java.io.File;

import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.ui.Microwave;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MiscSetup {
	private static Stage stage = null;
	
	private static void init() {
		stage = new Stage();
		stage.setTitle("Misc. Settings");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		
		VBox root = new VBox(40);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(20, 25, 20, 25));
		
		VBox cacheSet = new VBox(8);
		cacheSet.setAlignment(Pos.CENTER_LEFT);
		
		HBox cacheDirSet = new HBox(4);
		TextField cacheDir = new TextField(Prefs.loadString(Prefs.CACHE_DIR));
		cacheDir.setPrefWidth(250);
		Button browse = new Button("Browse...");
		browse.setOnAction((e) -> {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle("Choose Cache Directory");
			File last = new File(Prefs.loadString(Prefs.CACHE_DIR));
			if ( last.exists() ) {
				chooser.setInitialDirectory(last);
			}
			File from = chooser.showDialog(null);
			if ( from != null ) {
				cacheDir.setText(from.toString());
				//Prefs.saveString(Prefs.LAST_LAYOUT_DIR, from.getParentFile().toString()); // Why was this here?
			}
		});
		cacheDirSet.getChildren().addAll(cacheDir, browse);
		cacheSet.getChildren().addAll(new Label("Use the following directory to cache files from remote libraries:"), cacheDirSet);
		root.getChildren().add(cacheSet);
		
		// "Butts"
		HBox buttBar = new HBox(8);
		buttBar.setAlignment(Pos.CENTER);
		Button ok = new Button("OK");
		Button cancel = new Button("Cancel");
		ok.setPrefWidth(80);
		cancel.setPrefWidth(80);
		buttBar.getChildren().addAll(ok, cancel);
		
		// Butt event handlers
		ok.setOnAction((e) -> {
			// Check to make sure a valid directory was specified
			if ( cacheDir.getText().isEmpty() ) {
				Microwave.showError("You only had ONE JOB!", "I can't just send files into thin air and then pull them out of my ass!\n\nChoose a valid cache directory, then try again.", stage);
				return;
			}
			
			File cacheDirFile = new File(cacheDir.getText());
			if ( ! (cacheDirFile.exists() && cacheDirFile.isDirectory()) ) {
				Microwave.showError("That Won't Work...", "That doesn't look like an actual directory to me!\n\nChoose a valid cache directory, then try again.", stage);
				return;
			}
			
			Prefs.saveString(Prefs.CACHE_DIR, cacheDir.getText());
			
			stage.close();
		});
		
		cancel.setOnAction((e) -> stage.close());
		
		root.getChildren().add(buttBar);
		
		stage.setScene(new Scene(root));
	}
	
	// Show the dialog
	// This is what gets called from outside
	public static void show() {
		if ( stage == null ) {
			init();
		}
		
		stage.show();
	}
}
