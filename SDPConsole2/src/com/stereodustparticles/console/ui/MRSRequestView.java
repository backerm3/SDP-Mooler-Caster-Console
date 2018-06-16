/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017-18
 * 
 * MRSRequestView: Displays the list of requests from the MRS in a nice fancy window
 */
package com.stereodustparticles.console.ui;

import com.stereodustparticles.console.mrs.MRSIntegration;
import com.stereodustparticles.musicrequestsystem.mri.Request;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MRSRequestView {
	private static Stage stage = null;
	
	private static void init() {
		stage = new Stage();
		stage.setTitle("Requests");
		
		// Root
		VBox root = new VBox(40);
		root.setAlignment(Pos.CENTER_LEFT);
		root.setPadding(new Insets(10, 15, 10, 15));
		
		// Request list
		ListView<Request> reqList = new ListView<Request>();
		reqList.setItems(MRSIntegration.getRequestList());
		root.getChildren().add(reqList);
		
		// Action Selector
		HBox actionSelector = new HBox(8);
		actionSelector.setAlignment(Pos.CENTER_LEFT);
		ChoiceBox<String> action = new ChoiceBox<String>();
		action.getItems().addAll("Queue", "Queue & Load to Deck 1", "Queue & Load to Deck 2", "Queue & Add Tentative", "Mark Played", "Decline");
		TextField comment = new TextField();
		comment.setPromptText("Comment");
		Button go = new Button("Go");
		actionSelector.getChildren().addAll(new Label("With selected:"), action, comment, go);
		root.getChildren().add(actionSelector);
		
		// More Buttons
		HBox lowerButts = new HBox(10);
		Button refresh = new Button("Refresh List");
		refresh.setOnAction((e) -> MRSIntegration.refresh());
		lowerButts.getChildren().add(refresh);
		root.getChildren().add(lowerButts);
		
		stage.setScene(new Scene(root));
	}
	
	public static void show() {
		if ( stage == null ) {
			init();
		}
		
		MRSIntegration.refresh();
		stage.show();
	}
}
