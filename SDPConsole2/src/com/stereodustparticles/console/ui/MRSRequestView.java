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
import com.stereodustparticles.console.deck.DeckLoadRequest;
import com.stereodustparticles.console.deck.Decks;
import com.stereodustparticles.console.error.MRSException;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.library.LibraryEntry;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.mrs.MRSIntegration;
import com.stereodustparticles.console.playlist.PlaylistEntry;
import com.stereodustparticles.console.playlist.PlaylistFlags;
import com.stereodustparticles.console.pref.Prefs;
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
		action.getSelectionModel().select(0);
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
						case 1: // Queue and load to deck
						case 2: // LAME HACK ALERT!  Note that the option index just so happens to equal the target deck number...
							LibraryEntry reqTrack = MRSIntegration.getRequestedTrack(req);
							if ( reqTrack != null ) {
								if ( (option == 1 && Decks.deck1IsPlaying()) || (option == 2 && Decks.deck2IsPlaying()) ) {
									Platform.runLater(() -> Microwave.showWarning("You only had ONE JOB!", "You didn't really want to load over a playing track, did you?"));
									return;
								}
								
								DeckLoadRequest dlr = new DeckLoadRequest(reqTrack.getTitle(), reqTrack.getArtist(), reqTrack.getDuration(), reqTrack.getLibraryName(), LibraryManager.getLibraryForName(reqTrack.getLibraryName()).getPathInLibrary(reqTrack), option);
								EventBus.fireEvent(new Event(EventType.DECK_REQUEST_LOAD, dlr));
								
								// TODO allow users to disable automatic use of REQUEST flag?
								if ( Prefs.loadBoolean(Prefs.AUTO_ADD_TENTATIVE) && ! Decks.snpIsEnabled() ) {
									EventBus.fireEvent(new Event(EventType.PLAYLIST_ADD, new PlaylistEntry(reqTrack, true, PlaylistFlags.REQUEST | LibraryManager.getLibraryForName(reqTrack.getLibraryName()).getDefaultFlags())));
								}
							}
							else {
								Platform.runLater(() -> Microwave.showWarning("Auto-Load Not Available", "No file information was found in the request.  You'll need to load the track manually."));
							}
							
							// Don't queue the track again if it's already queued and no (new) comment is entered
							// This prevents the response comment from being overwritten on the MRS
							if ( req.getStatus() != 1 || ! comment.getText().isEmpty() ) {
								MRSIntegration.queue(req, comment.getText());
							}
							break;
						case 3: // Queue and add tentative
							LibraryEntry reqTrak = MRSIntegration.getRequestedTrack(req);
							if ( reqTrak != null ) {
								EventBus.fireEvent(new Event(EventType.PLAYLIST_ADD, new PlaylistEntry(reqTrak, true, PlaylistFlags.REQUEST | LibraryManager.getLibraryForName(reqTrak.getLibraryName()).getDefaultFlags())));
							}
							else {
								Platform.runLater(() -> Microwave.showWarning("Auto-Load Not Available", "No file information was found in the request.  You'll need to add the track to the playlist manually."));
							}
							
							// Don't queue the track again if it's already queued and no (new) comment is entered
							// This prevents the response comment from being overwritten on the MRS
							if ( req.getStatus() != 1 || ! comment.getText().isEmpty() ) {
								MRSIntegration.queue(req, comment.getText());
							}
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
