/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * LibraryEditor: Defines the library editor UI
 */
package com.stereodustparticles.console.ui.setup;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.library.CSVLibrary;
import com.stereodustparticles.console.library.FSLibrary;
import com.stereodustparticles.console.library.Library;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.playlist.PlaylistFlags;
import com.stereodustparticles.console.ui.Microwave;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LibraryEditor {
	
	private Stage stage;
	private Map<String, Integer> flagSet;
	
	// Set parameter to null for new library
	public LibraryEditor(String oldName) {
		stage = new Stage();
		stage.setTitle("Edit Library");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		stage.setAlwaysOnTop(true);
		
		VBox root = new VBox(25);
		root.setAlignment(Pos.CENTER_LEFT);
		root.setPadding(new Insets(10, 15, 10, 15));
		
		// Name field
		HBox nameBar = new HBox(5);
		nameBar.setAlignment(Pos.CENTER_LEFT);
		TextField name = new TextField();
		nameBar.getChildren().addAll(new Label("Library Name:"), name);
		root.getChildren().add(nameBar);
		
		// Library type selector
		VBox libTypeSel = new VBox(8);
		final ToggleGroup typeOpts = new ToggleGroup();
		RadioButton fileSystem = new RadioButton("Local (File System)");
		RadioButton csv = new RadioButton("CSV");
		fileSystem.setToggleGroup(typeOpts);
		csv.setToggleGroup(typeOpts);
		libTypeSel.getChildren().addAll(new Label("Library Type:"), fileSystem, csv);
		root.getChildren().add(libTypeSel);
		
		// Path field
		HBox locBar = new HBox(5);
		locBar.setAlignment(Pos.CENTER_LEFT);
		TextField location = new TextField();
		location.setPrefWidth(300);
		Button browse = new Button("Browse...");
		locBar.getChildren().addAll(new Label("Library Path:"), location, browse);
		root.getChildren().add(locBar);
		
		// Populate fields from current library (if specified)
		Library lib = null;
		if ( oldName != null ) {
			name.setText(oldName);
			
			lib = LibraryManager.getLibraryForName(oldName);
			location.setText(lib.getLocationAsString());
			
			if ( lib instanceof FSLibrary ) {
				fileSystem.setSelected(true);
			}
			else if ( lib instanceof CSVLibrary ) {
				csv.setSelected(true);
			}
		}
		
		// Flags
		TilePane flagPane = new TilePane();
		flagPane.setPrefColumns(3);
		flagPane.setHgap(6);
		flagPane.setVgap(10);
		
		flagSet = PlaylistFlags.getFlagSet();
		for ( Map.Entry<String, Integer> flag : flagSet.entrySet() ) {
			CheckBox fBox = new CheckBox(flag.getKey());
			if ( lib != null ) {
				fBox.setSelected(PlaylistFlags.flagIsSet(lib.getDefaultFlags(), flag.getValue()));
			}
			flagPane.getChildren().add(fBox);
			TilePane.setAlignment(fBox, Pos.CENTER_LEFT);
		}
		
		VBox flagWrap = new VBox(10);
		flagWrap.getChildren().addAll(new Label("Flags:"), flagPane);
		root.getChildren().add(flagWrap);
		
		// OK/Cancel
		HBox okCancel = new HBox(8);
		okCancel.setAlignment(Pos.CENTER);
		Button ok = new Button("OK");
		Button cancel = new Button("Cancel");
		ok.setPrefWidth(80);
		cancel.setPrefWidth(80);
		okCancel.getChildren().addAll(ok, cancel);
		root.getChildren().add(okCancel);
		
		// Event handlers
		browse.setOnAction((e) -> {
			// Browsing is only available for file system libraries
			if ( ! fileSystem.isSelected() ) {
				Microwave.showError("Um, it doesn't work that way...", "Browsing is only available for local libraries.  Select that library type before browsing.", stage);
				return;
			}
			
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle("Choose Base Directory for Library");
			
			if ( ! location.getText().isEmpty() ) {
				File startDir = new File(location.getText());
				if ( startDir.exists() ) {
					chooser.setInitialDirectory(startDir);
				}
			}
			
			File selDir = chooser.showDialog(stage);
			
			if ( selDir != null ) {
				location.setText(selDir.toString());
			}
		});
		
		cancel.setOnAction((e) -> stage.close());
		
		ok.setOnAction((evt) -> {
			// Did the user actually enter a name?
			if ( name.getText().isEmpty() ) {
				Microwave.showError("You only had ONE JOB!", "You have to give the library a name, silly!", stage);
				return;
			}
			
			// Assemble flags
			int flags = 0;
			for ( Node n : flagPane.getChildren() ) { // This is probably a really hacky way of doing this...
				CheckBox cb = (CheckBox)n;
				if ( cb.isSelected() ) {
					int flagVal = flagSet.get(cb.getText());
					flags |= flagVal;
				}
			}
			
			// Check what type of library is selected and act accordingly
			if ( fileSystem.isSelected() ) {
				File loc = new File(location.getText());
				
				// If the location specified doesn't exist, remind the user of their One Job
				if ( ! (loc.exists() && loc.isDirectory())) {
					Microwave.showError("Error Saving Library Configuration", "The location you entered doesn't exist or is not a directory.  Are you sure you did your ONE JOB?", stage);
					return;
				}
				
				stage.close();
				LibraryManager.putLibrary(name.getText(), oldName, new FSLibrary(name.getText(), loc, flags));
				LibraryManagerUI.refreshList();
				EventBus.fireEvent(new Event(EventType.LIBRARY_LIST_UPDATED));
			}
			else if ( csv.isSelected() ) {
				URL loc;
				try {
					loc = new URL(location.getText());
				}
				catch (MalformedURLException e) {
					Microwave.showError("You only had ONE JOB!", "That doesn't look like a URL to me!", stage);
					return;
				}
				
				stage.close();
				LibraryManager.putLibrary(name.getText(), oldName, new CSVLibrary(name.getText(), loc, flags));
				LibraryManagerUI.refreshList();
			}
			else {
				Microwave.showError("You only had ONE JOB!", "You have to select a library type, silly!", stage);
				return;
			}
		});
		
		// Build the scene
		stage.setScene(new Scene(root));
	}
	
	// Show the window
	public void show() {
		stage.show();
	}
	
}
