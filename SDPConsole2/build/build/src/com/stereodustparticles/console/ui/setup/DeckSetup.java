/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * DeckSetup: Defines the Deck Preferences dialog
 */
package com.stereodustparticles.console.ui.setup;

import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.ui.Microwave;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;

public class DeckSetup {
	private static Stage stage = null;
	private static Spinner<Integer> flashPt;
	private static CheckBox autoTent;
	
	private static void init() {
		stage = new Stage();
		stage.setTitle("Deck Preferences");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		
		VBox root = new VBox(40);
		root.setAlignment(Pos.CENTER_LEFT);
		root.setPadding(new Insets(20, 25, 20, 25));
		
		HBox flashSet = new HBox(8);
		flashSet.setAlignment(Pos.CENTER_LEFT);
		flashPt = new Spinner<Integer>(0, 120, Prefs.loadInt(Prefs.FLASH_POINT));
		flashPt.setPrefWidth(65);
		flashPt.setEditable(true);
		flashSet.getChildren().addAll(new Label("Flash the deck timer when"), flashPt, new Label("seconds remain in playback."));
		root.getChildren().add(flashSet);
		
		autoTent = new CheckBox("Automatically add tentative tracks to the playlist when loading the decks");
		autoTent.setSelected(Prefs.loadBoolean(Prefs.AUTO_ADD_TENTATIVE));
		root.getChildren().add(autoTent);
	
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
				String text = flashPt.getEditor().getText();
				SpinnerValueFactory<Integer> valueFactory = flashPt.getValueFactory();
				StringConverter<Integer> converter = valueFactory.getConverter();
				Integer value = converter.fromString(text);
	            valueFactory.setValue(value);
			}
			// While we're at it, check to make sure the user isn't trying to slip a non-number past us!
			catch (NumberFormatException e1) {
				Microwave.showError("Nice try!", "I see what you're trying to do, and yes, I thought of that!\n\nEnter an actual number for the timer flash point, then try again!");
				return;
			}
			
			Prefs.saveInt(Prefs.FLASH_POINT, flashPt.getValue());
			Prefs.saveBoolean(Prefs.AUTO_ADD_TENTATIVE, autoTent.isSelected());
			
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
		else {
			flashPt.getValueFactory().setValue(Prefs.loadInt(Prefs.FLASH_POINT));
			autoTent.setSelected(Prefs.loadBoolean(Prefs.AUTO_ADD_TENTATIVE));
		}
		
		stage.show();
	}
}
