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
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;

public class SoundboardSetup {
	private static Stage stage = null;
	
	private static void init() {
		stage = new Stage();
		stage.setTitle("Soundboard Preferences");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		
		VBox root = new VBox(40);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(20, 25, 20, 25));
		
		VBox autoLoadSet = new VBox(8);
		autoLoadSet.setAlignment(Pos.CENTER_LEFT);
		
		HBox autoLoadPathSet = new HBox(4);
		TextField autoLoadPath = new TextField(Prefs.loadString(Prefs.STARTUP_LAYOUT));
		autoLoadPath.setPrefWidth(250);
		Button browse = new Button("Browse...");
		browse.setOnAction((e) -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Open Soundboard Layout");
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Soundboard Layouts", "*.bl2", "*.bla"));
			File last = new File(Prefs.loadString(Prefs.LAST_LAYOUT_DIR));
			if ( last.exists() ) {
				chooser.setInitialDirectory(last);
			}
			File from = chooser.showOpenDialog(null);
			if ( from != null ) {
				autoLoadPath.setText(from.toString());
				Prefs.saveString(Prefs.LAST_LAYOUT_DIR, from.getParentFile().toString());
			}
		});
		autoLoadPathSet.getChildren().addAll(autoLoadPath, browse);
		autoLoadSet.getChildren().addAll(new Label("Load the following layout file at startup (leave blank for none):"), autoLoadPathSet);
		root.getChildren().add(autoLoadSet);
		
		// Soundboard size adjustments
		GridPane mtxPane = new GridPane();
		mtxPane.setHgap(18);
		mtxPane.setVgap(6);
		mtxPane.setPadding(new Insets(25, 25, 25, 25));
		
		// Labels
		Label sbsTitle = new Label("Soundboard Size:");
		sbsTitle.setStyle("-fx-font-weight: bold");
		mtxPane.add(sbsTitle, 0, 0);
		mtxPane.add(new Label("Rows:"), 0, 1);
		mtxPane.add(new Label("Columns:"), 0, 2);
		
		// Spinners
		Spinner<Integer> rows = new Spinner<Integer>(1, 10, Prefs.loadInt(Prefs.SOUNDBOARD_ROWS));
		Spinner<Integer> cols = new Spinner<Integer>(1, 10, Prefs.loadInt(Prefs.SOUNDBOARD_COLS));
		rows.setPrefWidth(65);
		cols.setPrefWidth(65);
		rows.setEditable(true);
		cols.setEditable(true);
		mtxPane.add(rows, 1, 1);
		mtxPane.add(cols, 1, 2);
		
		root.getChildren().add(mtxPane);
		
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
			Prefs.saveString(Prefs.STARTUP_LAYOUT, autoLoadPath.getText());
			
			// Commit spinner changes before saving settings
			try {
				commitSpinnerChange(rows);
				commitSpinnerChange(cols);
			}
			// Catch sneaky users trying to slip things past us
			catch (NumberFormatException e1) {
				Microwave.showError("You only had ONE JOB!", "That doesn't look like a number to me!");
				return;
			}
			
			Prefs.saveInt(Prefs.SOUNDBOARD_ROWS, rows.getValue());
			Prefs.saveInt(Prefs.SOUNDBOARD_COLS, cols.getValue());
			
			// Remind the user to restart the Console
			Microwave.showInfo("Settings Changed", "Restart the Console for your new settings to take effect.", stage);
			
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
	
	// Commit changes to the spinner
	// Loosely derived from https://stackoverflow.com/questions/32340476/manually-typing-in-text-in-javafx-spinner-is-not-updating-the-value-unless-user
	private static <T> void commitSpinnerChange(Spinner<T> spinner) {
		String text = spinner.getEditor().getText();
		SpinnerValueFactory<T> valueFactory = spinner.getValueFactory();
		StringConverter<T> converter = valueFactory.getConverter();
		T value = converter.fromString(text);
        valueFactory.setValue(value);
	}
}
