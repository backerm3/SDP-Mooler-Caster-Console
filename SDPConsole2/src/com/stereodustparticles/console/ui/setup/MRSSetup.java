/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017-18
 * 
 * MRSSetup: MRS configuration dialog
 */
package com.stereodustparticles.console.ui.setup;

import com.stereodustparticles.console.pref.Prefs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MRSSetup {
	private static Stage stage = null;
	
	private static void init() {
		stage = new Stage();
		stage.setTitle("MRS Configuration");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		
		// Root
		VBox root = new VBox(40);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(20, 25, 20, 25));
		
		// Text fields in grid
		GridPane mtxPane = new GridPane();
		mtxPane.setHgap(18);
		mtxPane.setVgap(6);
		mtxPane.setPadding(new Insets(15, 15, 15, 15));
		
		mtxPane.add(new Label("Base URL:"), 0, 0);
		mtxPane.add(new Label("API Key:"), 0, 1);
		
		TextField url = new TextField(Prefs.loadString(Prefs.MRS_URL));
		PasswordField key = new PasswordField();
		key.setText(Prefs.loadString(Prefs.MRS_KEY));
		
		mtxPane.add(url, 1, 0);
		mtxPane.add(key, 1, 1);
		
		root.getChildren().add(mtxPane);
		
		// "Butts"
		HBox buttBar = new HBox(8);
		buttBar.setAlignment(Pos.CENTER);
		Button ok = new Button("OK");
		Button cancel = new Button("Cancel");
		ok.setPrefWidth(80);
		cancel.setPrefWidth(80);
		buttBar.getChildren().addAll(ok, cancel);
		
		ok.setOnAction((e) -> {
			Prefs.saveString(Prefs.MRS_URL, url.getText());
			Prefs.saveString(Prefs.MRS_KEY, key.getText());
			
			stage.close();
		});
		
		cancel.setOnAction((e) -> stage.close());
		
		root.getChildren().add(buttBar);
		
		stage.setScene(new Scene(root));
	}
	
	public static void show() {
		if ( stage == null ) {
			init();
		}
		
		stage.show();
	}
}
