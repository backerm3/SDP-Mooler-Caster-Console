/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * SoundboardSetup: Defines the Soundboard Preferences dialog
 */
package com.stereodustparticles.console.ui.setup;

import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.ui.Microwave;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;

public class PlaylistSetup {
	private static Stage stage = null;
	
	private static void init() {
		stage = new Stage();
		stage.setTitle("Playlist Options");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		
		VBox root = new VBox(40);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(20, 25, 20, 25));
		
		// Custom flag definitions
		GridPane mtxPane = new GridPane();
		mtxPane.setHgap(18);
		mtxPane.setVgap(6);
		mtxPane.setPadding(new Insets(25, 25, 25, 25));
		
		// Labels
		mtxPane.add(new Label("Custom Flag #1:"), 0, 0);
		mtxPane.add(new Label("Custom Flag #2:"), 0, 1);
		mtxPane.add(new Label("Custom Flag #3:"), 0, 2);
		
		// Text Fields
		TextField flag1 = new TextField(Prefs.loadString(Prefs.PLAYLIST_CUSTOM_FLAG_1));
		TextField flag2 = new TextField(Prefs.loadString(Prefs.PLAYLIST_CUSTOM_FLAG_2));
		TextField flag3 = new TextField(Prefs.loadString(Prefs.PLAYLIST_CUSTOM_FLAG_3));
		mtxPane.add(flag1, 1, 0);
		mtxPane.add(flag2, 1, 1);
		mtxPane.add(flag3, 1, 2);
		
		root.getChildren().add(mtxPane);
		
		// Playlist backup timeout
		HBox timeoutSet = new HBox(8);
		timeoutSet.setAlignment(Pos.CENTER_LEFT);
		Spinner<Integer> timeout = new Spinner<Integer>(1, 120, Prefs.loadInt(Prefs.PLAYLIST_SAVE_TIMEOUT));
		timeout.setPrefWidth(65);
		timeout.setEditable(true);
		timeoutSet.getChildren().addAll(new Label("Save the playlist backup file"), timeout, new Label("seconds after the last change."));
		root.getChildren().add(timeoutSet);
		
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
			// Commit the value in the spinner's text field, because apparently Java is too dumb to do that on its own!
			// Loosely derived from https://stackoverflow.com/questions/32340476/manually-typing-in-text-in-javafx-spinner-is-not-updating-the-value-unless-user
			try {
				String text = timeout.getEditor().getText();
				SpinnerValueFactory<Integer> valueFactory = timeout.getValueFactory();
				StringConverter<Integer> converter = valueFactory.getConverter();
				Integer value = converter.fromString(text);
	            valueFactory.setValue(value);
			}
			// While we're at it, check to make sure the user isn't trying to slip a non-number past us!
			catch (NumberFormatException e1) {
				Microwave.showError("I saw that!", "You didn't really think you could outsmart me that easily, did you?\n\nEnter an actual number for the playlist backup timeout, then try again.");
				return;
			}
			
			Prefs.saveInt(Prefs.PLAYLIST_SAVE_TIMEOUT, timeout.getValue());
			
			Prefs.saveString(Prefs.PLAYLIST_CUSTOM_FLAG_1, flag1.getText());
			Prefs.saveString(Prefs.PLAYLIST_CUSTOM_FLAG_2, flag2.getText());
			Prefs.saveString(Prefs.PLAYLIST_CUSTOM_FLAG_3, flag3.getText());
			
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
