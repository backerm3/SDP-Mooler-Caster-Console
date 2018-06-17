/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017-18
 * 
 * RequestListCell: Custom list cell for MRS request view
 */
package com.stereodustparticles.console.ui;

import com.stereodustparticles.console.mrs.MRSIntegration;
import com.stereodustparticles.musicrequestsystem.mri.Request;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class RequestListCell extends ListCell<Request> {
	
	// Roughly per https://www.turais.de/how-to-custom-listview-cell-in-javafx/
	@Override
	protected void updateItem(Request req, boolean empty) {
		super.updateItem(req, empty);
		
		if ( empty || req == null ) {
			setText(null);
			setGraphic(null);
			return;
		}
		
		// Side-by-side labels and button
		HBox root = new HBox();
		root.setAlignment(Pos.CENTER_LEFT);
		
		// Stacked labels
		VBox stacker = new VBox(2);
		stacker.setAlignment(Pos.CENTER_LEFT);
		
		Label reqText = new Label(req.getRequest());
		reqText.setStyle("-fx-font-weight: bold;");
		
		stacker.getChildren().addAll(reqText, new Label("Requested by " + req.getUser() + " (" + MRSIntegration.getTextualStatus(req) + ")"));
		
		// Spacer
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		
		// Info button
		Button info = new Button("Info...");
		info.setOnAction((e) -> Microwave.showInfo("Request Info", req.toString(), MRSRequestView.getStage()));
		
		// Finally...
		root.getChildren().addAll(stacker, spacer, info);
		setText(null);
		setGraphic(root);
	}
}
