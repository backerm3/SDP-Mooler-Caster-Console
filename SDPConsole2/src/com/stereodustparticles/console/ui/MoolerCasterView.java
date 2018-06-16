package com.stereodustparticles.console.ui;

import java.io.FileNotFoundException;
import java.util.List;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.deck.DeckLoadRequest;
import com.stereodustparticles.console.deck.Decks;
import com.stereodustparticles.console.error.HTTPException;
import com.stereodustparticles.console.error.MRSPasswordException;
import com.stereodustparticles.console.error.ModemDefenestrationException;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.library.Library;
import com.stereodustparticles.console.library.LibraryEntry;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.playlist.Playlist;
import com.stereodustparticles.console.playlist.PlaylistEntry;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.soundboard.SpotLoadRequest;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * MoolerCasterView: This defines the main UI of the program
 */

public class MoolerCasterView {
	
	private Scene scene;
	private ListView<LibraryEntry> libView;
	private Button goUp;
	private Button libRoot;
	private Label selTrk; // Normally "Selected Track:", but changes to various loading messages
	private TextField search;
	
	private List<LibraryEntry> cLibData = null;
	private ObservableList<LibraryEntry> cLibDisplay = FXCollections.observableArrayList();
	
	// Constructor - sets up the scene
	public MoolerCasterView() {
		
		// Start with a root node: a VBox that positions the Mooler Caster Menu at the top, and everything else below
		VBox root = new VBox();
		
		// Add the Mooler Caster Menu
		MoolerCasterMenu mcm = new MoolerCasterMenu();
		root.getChildren().add(mcm);
		
		// Create the main view: an HBox that splits the two halves of the UI apart
		HBox main = new HBox(8);
		main.setAlignment(Pos.CENTER);
		root.getChildren().add(main);
	    
		// Now, we'll build the left pane. Start with a VBox...
		VBox leftPane = new VBox(5);
		leftPane.setAlignment(Pos.CENTER);
		
		// Add a grid for the soundboard
		GridPane spotGrid = new GridPane();
		spotGrid.setAlignment(Pos.CENTER);
		spotGrid.setHgap(6);
		spotGrid.setVgap(6);
		spotGrid.setPadding(new Insets(25, 25, 25, 25));
		
		// Populate the grid with buttons
		int rows = Prefs.loadInt(Prefs.SOUNDBOARD_ROWS);
		int cols = Prefs.loadInt(Prefs.SOUNDBOARD_COLS);
		for ( int i = 0; i < rows; i++ ) {
			for ( int j = 0; j < cols; j++ ) {
				SpotButton button = new SpotButton(i, j);
				spotGrid.add(button, j, i);
			}
		}
		
		// Add the spot grid to the left pane
		leftPane.getChildren().add(spotGrid);
		
		// Create the deck controls and add them to the left pane
		leftPane.getChildren().add(new DeckControlPane(1));
		leftPane.getChildren().add(new DeckControlPane(2));
		
		// Add the left pane to the root
		main.getChildren().add(leftPane);
		
		// Start building the right pane
		VBox rightPane = new VBox();
		rightPane.setAlignment(Pos.CENTER_LEFT);
		rightPane.setPadding(new Insets(15, 25, 15, 25));
		rightPane.setSpacing(8);
		
		// Add the library browser label
		Label libBrsr = new Label("Library Browser:");
		libBrsr.setStyle("-fx-font-weight: bold;");
		rightPane.getChildren().add(libBrsr);
		
		// Add the library selector
		HBox libSelBar = new HBox();
		libSelBar.setAlignment(Pos.CENTER_LEFT);
		libSelBar.setSpacing(7);
		libSelBar.getChildren().add(new Label("Library:"));
		ChoiceBox<String> libMenu = new ChoiceBox<String>(FXCollections.observableArrayList(LibraryManager.getAvailableLibraries())); // Populate the menu
		libSelBar.getChildren().add(libMenu);
		rightPane.getChildren().add(libSelBar);
		
		// Configure the library selector to respond to the "library list updated" event
		EventBus.registerListener(EventType.LIBRARY_LIST_UPDATED, (e) -> {
			libMenu.setItems(FXCollections.observableArrayList(LibraryManager.getAvailableLibraries()));
			libMenu.getSelectionModel().selectFirst();
		});
		
		// Add library browser top button bar
		// (And yes, I really did have to use "butt" here...)
		HBox topButtBar = new HBox();
		topButtBar.setSpacing(7);
		goUp = new Button("Go Up");
		libRoot = new Button("Library Root");
		search = new TextField();
		search.setPromptText("Search...");
		HBox.setHgrow(search, Priority.ALWAYS);
		topButtBar.getChildren().addAll(goUp, libRoot, search);
		rightPane.getChildren().add(topButtBar);
		
		// Add the library browser pane
		// TODO Set up swipe-scrolling as per https://stackoverflow.com/questions/26537548/javafx-listview-with-touch-events-for-scrolling-up-and-down (roughly)
		libView = new ListView<LibraryEntry>();
		libView.setItems(cLibDisplay);
		rightPane.getChildren().add(libView);
		
		// Add the "Selected Track" pane
		selTrk = new Label("Selected Track:");
		selTrk.setStyle("-fx-font-weight: bold");
		rightPane.getChildren().add(selTrk);
		
		GridPane selTrkBox = new GridPane();
		selTrkBox.setPadding(new Insets(0, 10, 0, 10));
		selTrkBox.setHgap(25);
		selTrkBox.setVgap(3);
		selTrkBox.add(new Label("Title:"), 0, 0);
		selTrkBox.add(new Label("Artist:"), 0, 1);
		selTrkBox.add(new Label("Duration:"), 0, 2);
		
		Label title = new Label("-");
		Label artist = new Label("-");
		Label duration = new Label("-");
		selTrkBox.addColumn(1, title, artist, duration);
		
		rightPane.getChildren().add(selTrkBox);
		
		// Add the library browser's bottom "butt" bar
		HBox btmButtBar = new HBox();
		btmButtBar.setAlignment(Pos.CENTER_LEFT);
		btmButtBar.setSpacing(7);
		btmButtBar.setPadding(new Insets(0, 0, 25, 0)); // To push next set of controls down later
		Label loadTo = new Label("Load To:");
		Button loadDeck1 = new Button("Deck 1");
		Button loadDeck2 = new Button("Deck 2");
		Button loadSB = new Button("Soundboard");
		Button addTent = new Button("Add Tentative");
		loadTo.setDisable(true);
		loadDeck1.setDisable(true);
		loadDeck2.setDisable(true);
		loadSB.setDisable(true);
		addTent.setDisable(true);
		btmButtBar.getChildren().addAll(loadTo, loadDeck1, loadDeck2, loadSB, addTent);
		rightPane.getChildren().add(btmButtBar);
		
		// Add the playlist manager label
		Label plMgr = new Label("Playlist Manager:");
		plMgr.setStyle("-fx-font-weight: bold;");
		rightPane.getChildren().add(plMgr);
		
		// Add the playlist manager pane
		ListView<PlaylistEntry> playlist = new ListView<PlaylistEntry>();
		playlist.setItems(Playlist.getObservableList());
		rightPane.getChildren().add(playlist);
		
		// Add the playlist manager controls
		HBox plButtBar = new HBox();
		plButtBar.setAlignment(Pos.CENTER_LEFT);
		plButtBar.setSpacing(7);
		Button manAdd = new Button("Add");
		Button edit = new Button("Edit");
		Button delete = new Button("Delete");
		Button moveUp = new Button("Move Up");
		Button moveDn = new Button("Move Down");
		edit.setDisable(true);
		delete.setDisable(true);
		moveUp.setDisable(true);
		moveDn.setDisable(true);
		plButtBar.getChildren().addAll(manAdd, edit, delete, moveUp, moveDn);
		rightPane.getChildren().add(plButtBar);
		
		// Add the tentative track controls
		HBox ttButtBar = new HBox();
		ttButtBar.setAlignment(Pos.CENTER_LEFT);
		ttButtBar.setSpacing(7);
		Label ttLoad = new Label("Load Tentative Track To:");
		Button ttDeck1 = new Button("Deck 1");
		Button ttDeck2 = new Button("Deck 2");
		Button ttPlayed = new Button("Mark Played");
		ttLoad.setDisable(true);
		ttDeck1.setDisable(true);
		ttDeck2.setDisable(true);
		ttPlayed.setDisable(true);
		ttButtBar.getChildren().addAll(ttLoad, ttDeck1, ttDeck2, ttPlayed);
		rightPane.getChildren().add(ttButtBar);
		
		// Add the right pane to the root
		main.getChildren().add(rightPane);
		
		// Set up the callbacks for the library browser
		libMenu.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				selTrk.setText("Loading library, please wait...");
				Library newLib = LibraryManager.getLibraryForName(newValue);
				Utils.runInBackground(() -> updateLibView(newLib)); // I never thought I'd appreciate lambdas...
			}
			
		});
		
		libView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<LibraryEntry>() {

			@Override
			public void changed(ObservableValue<? extends LibraryEntry> observable, LibraryEntry oldValue, LibraryEntry newValue) {
				// Enable/disable load buttons as necessary
				if ( newValue == null || newValue.isDir() ) {
					loadTo.setDisable(true);
					loadDeck1.setDisable(true);
					loadDeck2.setDisable(true);
					loadSB.setDisable(true);
					addTent.setDisable(true);
				}
				else if ( ! newValue.isLoadable() ) {
					loadTo.setDisable(false);
					loadDeck1.setDisable(true);
					loadDeck2.setDisable(true);
					loadSB.setDisable(true);
					addTent.setDisable(false);
				}
				else {
					loadTo.setDisable(false);
					loadDeck1.setDisable(false);
					loadDeck2.setDisable(false);
					loadSB.setDisable(false);
					addTent.setDisable(false);
				}
				
				// Update metadata labels - if new selection is null, blank them
				if ( newValue == null ) {
					title.setText("-");
					artist.setText("-");
					duration.setText("-");
				}
				else {
					selTrk.setText("Loading...");
					
					// Get metadata in the background, then come back to the foreground to adjust the labels
					Utils.runInBackground(new Runnable() {

						@Override
						public void run() {
							String titleText = newValue.getTitle();
							String artistText = newValue.getArtist();
							String durationText = newValue.getDurationPreview();
							
							Platform.runLater(new Runnable() {

								@Override
								public void run() {
									title.setText(titleText);
									artist.setText(artistText);
									duration.setText(durationText);
									selTrk.setText("Selected Track:");
								}
								
							});
						}
						
					});
				}
			}
			
		});
		
		// Double-click event, for changing dirs
		libView.setOnMouseClicked(new EventHandler<MouseEvent>() {
		    @Override
		    public void handle(MouseEvent mouseEvent) {
		        if ( mouseEvent.getButton().equals(MouseButton.PRIMARY) ) {
		            if ( mouseEvent.getClickCount() == 2 ) {
		                selTrk.setText("Loading library, please wait...");
		                Utils.runInBackground(() -> {
		                	LibraryEntry cEntry = libView.getSelectionModel().selectedItemProperty().get();
		                	if ( cEntry.isDir() ) {
		                		Library cLibrary = LibraryManager.getLibraryForName(libMenu.getSelectionModel().selectedItemProperty().get());
			    				cLibrary.changeDir(cEntry.getTitle());
			                	updateLibView(cLibrary);
		                	}
		                	else {
		                		Platform.runLater(() -> selTrk.setText("Selected Track:"));
		                	}
		                });
		            }
		        }
		    }
		});
		
		// Search
		search.textProperty().addListener((obs, ov, nv) -> {
			if ( cLibData == null ) return;
			
			if ( nv.isEmpty() ) {
				cLibDisplay.setAll(cLibData);
				return;
			}
			
			cLibDisplay.clear();
			for ( LibraryEntry entry : cLibData ) {
				if ( entry.toString().toLowerCase().contains(nv.toLowerCase()) ) {
					cLibDisplay.add(entry);
				}
			}
		});
		
		// Go up event handler
		goUp.setOnAction(new EventHandler<ActionEvent>() {
		    
			@Override
		    public void handle(ActionEvent e) {
				selTrk.setText("Loading library, please wait...");
				Library cLibrary = LibraryManager.getLibraryForName(libMenu.getSelectionModel().selectedItemProperty().get());
				Utils.runInBackground(() -> {
					LibraryEntry from = cLibrary.getCurrentDirectory();
					cLibrary.upOneLevel();
					updateLibView(cLibrary);
					Platform.runLater(() -> {
						libView.getSelectionModel().select(from);
						libView.scrollTo(from);
					});
				});
		    }
			
		});
		
		// Library root event handler
		libRoot.setOnAction(new EventHandler<ActionEvent>() {
		    
			@Override
		    public void handle(ActionEvent e) {
				selTrk.setText("Loading library, please wait...");
				Library cLibrary = LibraryManager.getLibraryForName(libMenu.getSelectionModel().selectedItemProperty().get());
				Utils.runInBackground(() -> {
					cLibrary.backToRoot();
					updateLibView(cLibrary);
				});
		    }
			
		});
		
		// Deck load
		loadDeck1.setOnAction(new EventHandler<ActionEvent>() {
		    
			@Override
		    public void handle(ActionEvent e) {
				if ( Decks.deck1IsPlaying() ) {
					Microwave.showWarning("You only had ONE JOB!", "You didn't really want to load over a playing track, did you?");
					return;
				}
				
				String cLibrary = libMenu.getSelectionModel().selectedItemProperty().get();
				LibraryEntry cEntry = libView.getSelectionModel().selectedItemProperty().get();
				
				DeckLoadRequest req = new DeckLoadRequest(cEntry.getTitle(), cEntry.getArtist(), cEntry.getDuration(), cLibrary, LibraryManager.getLibraryForName(cLibrary).getPathInLibrary(cEntry), 1);
				EventBus.fireEvent(new Event(EventType.DECK_REQUEST_LOAD, req));
				
				if ( Prefs.loadBoolean(Prefs.AUTO_ADD_TENTATIVE) && ! Decks.snpIsEnabled() ) {
					EventBus.fireEvent(new Event(EventType.PLAYLIST_ADD, new PlaylistEntry(cEntry, true, LibraryManager.getLibraryForName(cLibrary).getDefaultFlags())));
				}
		    }
			
		});
		
		loadDeck2.setOnAction(new EventHandler<ActionEvent>() {
		    
			@Override
		    public void handle(ActionEvent e) {
				if ( Decks.deck2IsPlaying() ) {
					Microwave.showWarning("You only had ONE JOB!", "You didn't really want to load over a playing track, did you?");
					return;
				}
				
				String cLibrary = libMenu.getSelectionModel().selectedItemProperty().get();
				LibraryEntry cEntry = libView.getSelectionModel().selectedItemProperty().get();
				
				DeckLoadRequest req = new DeckLoadRequest(cEntry.getTitle(), cEntry.getArtist(), cEntry.getDuration(), cLibrary, LibraryManager.getLibraryForName(cLibrary).getPathInLibrary(cEntry), 2);
				EventBus.fireEvent(new Event(EventType.DECK_REQUEST_LOAD, req));
				
				if ( Prefs.loadBoolean(Prefs.AUTO_ADD_TENTATIVE) && ! Decks.snpIsEnabled() ) {
					EventBus.fireEvent(new Event(EventType.PLAYLIST_ADD, new PlaylistEntry(cEntry, true, LibraryManager.getLibraryForName(cLibrary).getDefaultFlags())));
				}
		    }
			
		});
		
		loadSB.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				String cLibrary = libMenu.getSelectionModel().selectedItemProperty().get();
				LibraryEntry cEntry = libView.getSelectionModel().selectedItemProperty().get();
				
				EventBus.fireEvent(new Event(EventType.SPOT_REQUEST_LOAD, new SpotLoadRequest(cEntry.getTitle(), cLibrary, LibraryManager.getLibraryForName(cLibrary).getPathInLibrary(cEntry))));
			}
			
		});
		
		addTent.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				Library cLibrary = LibraryManager.getLibraryForName(libMenu.getSelectionModel().selectedItemProperty().get());
				LibraryEntry cEntry = libView.getSelectionModel().selectedItemProperty().get();
				
				EventBus.fireEvent(new Event(EventType.PLAYLIST_ADD, new PlaylistEntry(cEntry, true, cLibrary.getDefaultFlags())));
			}
			
		});
		
		// Playlist controls
		manAdd.setOnAction((e) -> {
			new PlaylistEntryEditor(null, 0).show();
		});
		
		edit.setOnAction((e) -> {
			new PlaylistEntryEditor(playlist.getSelectionModel().getSelectedItem(), playlist.getSelectionModel().getSelectedIndex()).show();
		});
		
		delete.setOnAction((e) -> {
			int index = playlist.getSelectionModel().getSelectedIndex();
			if ( index > -1 ) {
				EventBus.fireEvent(new Event(EventType.PLAYLIST_DELETE, index));
			}
		});
		
		moveUp.setOnAction((e) -> {
			int index = playlist.getSelectionModel().getSelectedIndex();
			if ( index > -1 ) {
				EventBus.fireEvent(new Event(EventType.PLAYLIST_MOVE_UP, index));
			}
		});
		
		moveDn.setOnAction((e) -> {
			int index = playlist.getSelectionModel().getSelectedIndex();
			if ( index > -1 ) {
				EventBus.fireEvent(new Event(EventType.PLAYLIST_MOVE_DOWN, index));
			}
		});
		
		ttDeck1.setOnAction((e) -> {
			if ( Decks.deck1IsPlaying() ) {
				Microwave.showWarning("You only had ONE JOB!", "You didn't really want to load over a playing track, did you?");
				return;
			}
			
			LibraryEntry cEntry = playlist.getSelectionModel().selectedItemProperty().get().getLocation();
			
			DeckLoadRequest req = new DeckLoadRequest(cEntry.getTitle(), cEntry.getArtist(), cEntry.getDuration(), cEntry.getLibraryName(), LibraryManager.getLibraryForName(cEntry.getLibraryName()).getPathInLibrary(cEntry), 1);
			EventBus.fireEvent(new Event(EventType.DECK_REQUEST_LOAD, req));
		});
		
		ttDeck2.setOnAction((e) -> {
			if ( Decks.deck2IsPlaying() ) {
				Microwave.showWarning("You only had ONE JOB!", "You didn't really want to load over a playing track, did you?");
				return;
			}
			
			LibraryEntry cEntry = playlist.getSelectionModel().selectedItemProperty().get().getLocation();
			
			DeckLoadRequest req = new DeckLoadRequest(cEntry.getTitle(), cEntry.getArtist(), cEntry.getDuration(), cEntry.getLibraryName(), LibraryManager.getLibraryForName(cEntry.getLibraryName()).getPathInLibrary(cEntry), 2);
			EventBus.fireEvent(new Event(EventType.DECK_REQUEST_LOAD, req));
		});
		
		ttPlayed.setOnAction((e) -> {
			EventBus.fireEvent(new Event(EventType.PLAYLIST_MARK_PLAYED, playlist.getSelectionModel().getSelectedItem()));
		});
		
		// Create a listener to track when a selected track becomes no longer tentative
		// Used to auto-disable Load Tentative Track To controls upon playback start
		ChangeListener<Boolean> tentListener = (obs, oldValue, newValue) -> {
			if ( newValue == false ) {
				ttLoad.setDisable(true);
				ttDeck1.setDisable(true);
				ttDeck2.setDisable(true);
				ttPlayed.setDisable(true);
			}
			else {
				ttLoad.setDisable(false);
				ttPlayed.setDisable(false);
				
				LibraryEntry loc = playlist.getSelectionModel().getSelectedItem().getLocation();
				if ( loc != null && loc.isLoadable() ) {
					ttDeck1.setDisable(false);
					ttDeck2.setDisable(false);
				}
				else {
					ttDeck1.setDisable(true);
					ttDeck2.setDisable(true);
				}
			}
		};
		
		// Auto enable/disable playlist controls
		playlist.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
			if ( newValue == null ) {
				edit.setDisable(true);
				delete.setDisable(true);
				moveUp.setDisable(true);
				moveDn.setDisable(true);
			}
			else {
				edit.setDisable(false);
				delete.setDisable(false);
				moveUp.setDisable(false);
				moveDn.setDisable(false);
			}
			
			if ( newValue == null || ! newValue.isTentative() ) {
				ttLoad.setDisable(true);
				ttDeck1.setDisable(true);
				ttDeck2.setDisable(true);
				ttPlayed.setDisable(true);
			}
			else {
				ttLoad.setDisable(false);
				ttPlayed.setDisable(false);
				
				LibraryEntry loc = newValue.getLocation();
				if ( loc != null && loc.isLoadable() ) {
					ttDeck1.setDisable(false);
					ttDeck2.setDisable(false);
				}
				else {
					ttDeck1.setDisable(true);
					ttDeck2.setDisable(true);
				}
			}
			
			// Hook a listener into the new selection to check for change in tentative status
			// Remove said listener from the old one first
			if ( oldValue != null ) {
				oldValue.tentativeProperty().removeListener(tentListener);
			}
			if ( newValue != null ) {
				newValue.tentativeProperty().addListener(tentListener);
			}
		});
		
		// Now that our callbacks are ready, select the first library on the list
		libMenu.getSelectionModel().selectFirst();
		
		// Finally, build the scene, from the top-level pane
	    scene = new Scene(root);
	}
	
	// Update the library list view
	// This should be run in a separate thread!
	// It will queue UI changes on the application thread automatically
	private void updateLibView(Library library) {
		
		// If no library is selected, return without doing jack diddly squat
		if ( library == null ) {
			return;
		}
		
		try {
			cLibData = library.getList();
			
			// Go back to the UI thread to update the display
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					goUp.setDisable(! library.canGoUp());
					libRoot.setDisable(library.isAtRoot());
					if ( search.getText().isEmpty() ) {
						cLibDisplay.setAll(cLibData);
					}
					else {
						search.clear();
					}
				}
				
			});
		}
		// Error messages!
		catch (HTTPException e) {
			Platform.runLater(() -> Microwave.showError("Error Loading Library", "Got a " + e.getMessage() + " error from the SDP server when attempting to download:\n\n" + e.getOffendingFile() + "\n\nTell Weasel he only had ONE JOB!"));
		}
		catch (ModemDefenestrationException e) {
			Platform.runLater(() -> Microwave.showException("Error Loading Library", "Connecting to the remote server failed with the following error:\n" + e.getMessage() + "\n\nCheck your Internet connection, throw your modem or router out the window if necessary, and try again.", e));
		}
		catch (FileNotFoundException e) {
			Platform.runLater(() -> Microwave.showError("Error Loading Library", "The requested library is pointing at an invalid directory.  Are you sure it (still) exists?"));
		}
		catch (MRSPasswordException e) {
			Platform.runLater(() -> Microwave.showError("Error Loading Library", "The MRS has rejected you!  Are you sure that was the right password?"));
		}
		catch (Exception e) {
			// If we hit this point, some unknown error has occurred
			// We'll show an exception box for that
			Platform.runLater(() -> Microwave.showException("Error Loading Library", "An unknown error occurred while loading the library.  Something probably needs to be microwaved.\n\nPlease see the details below for more information.", e));
		}
		finally {
			Platform.runLater(() -> selTrk.setText("Selected Track:"));
		}
	}

	// Return the scene
	public Scene getScene() {
		return scene;
	}
	
}
