/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017-18
 * 
 * MRSRequestView: Displays the list of requests from the MRS in a nice fancy window
 */
package com.stereodustparticles.console.ui;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.error.MRSException;
import com.stereodustparticles.console.mrs.MRSIntegration;
import com.stereodustparticles.musicrequestsystem.mri.Request;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MRSRequestView {
	private static Stage stage = null;
	private static boolean showPrev = false;
	
	private static void init() {
		stage = new Stage();
		stage.setTitle("Requests");
		
		// Root
		VBox root = new VBox(27);
		root.setAlignment(Pos.CENTER_LEFT);
		root.setPadding(new Insets(10, 15, 10, 15));
		
		// Request list
		ListView<Request> reqList = new ListView<Request>();
		reqList.setCellFactory(rv -> new RequestListCell());
		reqList.setItems(MRSIntegration.getRequestList());
		VBox.setVgrow(reqList, Priority.ALWAYS);
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
		
		// Go button event handler
		go.setOnAction((e) -> {
			Request req = reqList.getSelectionModel().getSelectedItem();
			if ( req == null ) {
				Microwave.showError("You only had ONE JOB!", "You have to select a request first, silly!");
				return;
			}
			
			int option = action.getSelectionModel().getSelectedIndex();
			
			Utils.runInBackground(() -> {
				try {
					switch (option) {
						case 0: // Queue
							MRSIntegration.queue(req, comment.getText());
							break;
						case 4: // Mark played
							MRSIntegration.markPlayed(req);
							break;
						case 5: // Decline
							MRSIntegration.decline(req, comment.getText());
							break;
					}
					
					Platform.runLater(() -> comment.clear());
					MRSIntegration.refresh();
				}
				catch ( MRSException e1 ) {
					Platform.runLater(() -> {
						Microwave.showError("The MRS Doesn't Like You", "The requested operation failed with the following error:\n\n" + e1.getMessage() + "\n\nThrow a GPX clock radio at the offending components, then try again.");
					});
				}
			});
		});
		
		// More Buttons
		HBox lowerButts = new HBox(10);
		Button refresh = new Button("Refresh List");
		refresh.setOnAction((e) -> Utils.runInBackground(() -> MRSIntegration.refresh(showPrev)));
		Button toggleShowPrev = new Button("Show Previous Requests");
		toggleShowPrev.setOnAction((e) -> {
			showPrev = ! showPrev;
			
			if ( showPrev ) {
				toggleShowPrev.setText("Hide Previous Requests");
			}
			else {
				toggleShowPrev.setText("Show Previous Requests");
			}
			
			Utils.runInBackground(() -> MRSIntegration.refresh(showPrev));
		});
		lowerButts.getChildren().addAll(refresh, toggleShowPrev);
		root.getChildren().add(lowerButts);
		
		stage.setScene(new Scene(root));
	}
	
	// Return the stage, so the request cell's info message appears right
	static Stage getStage() {
		return stage;
	}
	
	public static void show() {
		if ( stage == null ) {
			init();
		}
		
		Utils.runInBackground(() -> MRSIntegration.refresh(showPrev));
		stage.show();
	}
}
