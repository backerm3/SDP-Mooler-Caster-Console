/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * PlaylistEntryEditor: Defines the playlist entry editing dialog
 */
package com.stereodustparticles.console.ui;

import java.util.Map;

import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.playlist.PlaylistEntry;
import com.stereodustparticles.console.playlist.PlaylistFlags;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class PlaylistEntryEditor {
	private Stage stage;
	private Map<String, Integer> flagSet;
	private PlaylistEntry orig;
	private int index;
	
	// Pass null and anything for Add, pass existing entry and its index for Edit
	public PlaylistEntryEditor(PlaylistEntry entry, int i) {
		orig = entry;
		index = i;
		
		stage = new Stage();
		stage.setTitle("Edit Playlist Entry");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		stage.setAlwaysOnTop(true);
		
		VBox root = new VBox(25);
		root.setAlignment(Pos.CENTER_LEFT);
		root.setPadding(new Insets(10, 15, 10, 15));
		
		// Title/Artist
		TextField title = new TextField();
		TextField artist = new TextField();
		
		if ( orig != null ) {
			title.setText(orig.getTitle());
			artist.setText(orig.getArtist());
		}
		
		GridPane songPane = new GridPane();
		songPane.setHgap(9);
		songPane.setVgap(4);
		songPane.add(new Label("Title:"), 0, 0);
		songPane.add(new Label("Artist:"), 0, 1);
		songPane.add(title, 1, 0);
		songPane.add(artist, 1, 1);
		root.getChildren().add(songPane);
		
		// Tentative
		CheckBox tentative = new CheckBox("Track is tentative (not yet played)");
		if ( orig != null ) {
			tentative.setSelected(orig.isTentative());
		}
		root.getChildren().add(tentative);
		
		// Flags
		TilePane flagPane = new TilePane();
		flagPane.setPrefColumns(3);
		flagPane.setHgap(6);
		flagPane.setVgap(10);
		
		flagSet = PlaylistFlags.getFlagSet();
		for ( Map.Entry<String, Integer> flag : flagSet.entrySet() ) {
			CheckBox fBox = new CheckBox(flag.getKey());
			if ( orig != null ) {
				fBox.setSelected(PlaylistFlags.flagIsSet(orig.getFlags(), flag.getValue()));
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
		cancel.setOnAction((e) -> stage.close());
		ok.setOnAction((e) -> {
			// Validate title/artist
			// I can't believe how much extra code I'm using just for a cheap laugh...
			if ( title.getText().isEmpty() && artist.getText().isEmpty() ) {
				Microwave.showError("You only had ONE JOB!", "You didn't really want to make a blank playlist entry, did you?", stage);
				return;
			}
			else if ( title.getText().isEmpty() ) {
				Microwave.showError("You only had ONE JOB!", "Um, does this song have a name?", stage);
				return;
			}
			else if ( artist.getText().isEmpty() ) {
				Microwave.showError("You only had ONE JOB!", "\"Blank\"... I've never heard of that group before!", stage);
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
			
			// Execute the operation
			if ( orig != null ) { // Edit
				EventBus.fireEvent(new Event(EventType.PLAYLIST_EDIT, index, new PlaylistEntry(artist.getText(), title.getText(), orig.getLocation(), tentative.isSelected(), flags)));
			}
			else { // Add
				EventBus.fireEvent(new Event(EventType.PLAYLIST_ADD, new PlaylistEntry(artist.getText(), title.getText(), tentative.isSelected(), flags)));
			}
			
			// Close the window
			stage.close();
		});
		
		// Build the scene
		stage.setScene(new Scene(root));
	}
	
	// Show the window
	public void show() {
		stage.show();
	}
}
