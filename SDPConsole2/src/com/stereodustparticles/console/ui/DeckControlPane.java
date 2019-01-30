package com.stereodustparticles.console.ui;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.deck.DeckLoadRequest;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventListener;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.multi.MultiConsole;
import com.stereodustparticles.console.pref.Prefs;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * DeckControlPane: Extends GridPane to form the deck controls
 */

public class DeckControlPane extends GridPane {
	
	private int remain = 0;
	private int duration = 0;
	
	// Constructor - build the controls
	public DeckControlPane(int deckNum) {
		super();
		
		// Set alignment/padding/etc.
		setAlignment(Pos.CENTER);
		setHgap(6);
		setVgap(6);
		setPadding(new Insets(8, 0, 8, 0));
		setPrefWidth(384); // THIS stupid thing, believe it or not, is what finally knocked out those damn PHANTOM MARGINS!
		
		// Add play button
		Button playButton = new Button("PLAY");
		playButton.setTextAlignment(TextAlignment.CENTER);
		playButton.setStyle("-fx-base: #0F0; -fx-font-size: 18; -fx-font-weight: bold");
		playButton.setMaxSize(USE_PREF_SIZE, Double.MAX_VALUE);
		playButton.setMinWidth(USE_PREF_SIZE);
		playButton.setPrefWidth(89);
		add(playButton, 0, 0, 1, 3);
		
		// Add artist box
		Label artist = new Label("No track loaded.");
		artist.setPrefWidth(229);
		artist.setMaxWidth(229);
		artist.setTextOverrun(OverrunStyle.ELLIPSIS);
		artist.setStyle("-fx-border-color: #BBB;");
		add(artist, 1, 0, 2, 1);
		
		// Add title box
		Label title = new Label("No track loaded.");
		title.setMaxWidth(Double.MAX_VALUE);
		title.setTextOverrun(OverrunStyle.ELLIPSIS);
		title.setWrapText(false);
		title.setStyle("-fx-border-color: #BBB;");
		add(title, 1, 1, 3, 1);
		
		// Add time box
		Label timer = new Label("0:00.0");
		timer.setAlignment(Pos.CENTER_RIGHT);
		timer.setStyle("-fx-border-color: #BBB;");
		timer.setPrefWidth(50);
		timer.setMaxWidth(50);
		timer.setMinWidth(50);
		add(timer, 3, 0);
		
		// Add cue button
		Button cueButton = new Button("CUE");
		cueButton.setStyle("-fx-base: #FF0; -fx-font-weight: bold;");
		cueButton.setMaxWidth(USE_PREF_SIZE);
		cueButton.setMinWidth(USE_PREF_SIZE);
		cueButton.setPrefWidth(74);
		add(cueButton, 1, 2);
		
		// Add progress bar
		ProgressBar progress = new ProgressBar();
		progress.setProgress(0);
		progress.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(progress, true);
		add(progress, 2, 2, 2, 1);
		
		// Add volume fader
		Slider volume = new Slider();
		volume.setMin(-30);
		volume.setMax(6);
		volume.setValue(0);
		volume.setShowTickLabels(true);
		volume.setMajorTickUnit(6);
		add(volume, 0, 3, 4, 1);
		
		// Add event handlers
		playButton.setOnAction(new EventHandler<ActionEvent>() {
		    
			@Override
		    public void handle(ActionEvent e) {
				EventBus.fireEvent(new Event(EventType.DECK_PLAY_PRESSED, deckNum));
		    }
			
		});
		
		cueButton.setOnAction(new EventHandler<ActionEvent>() {
		    
			@Override
		    public void handle(ActionEvent e) {
				EventBus.fireEvent(new Event(EventType.DECK_CUE_PRESSED, deckNum));
		    }
			
		});
		
		volume.valueProperty().addListener(new ChangeListener<Number>() {
			
			@Override
	        public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) {
	        	EventBus.fireEvent(new Event(EventType.DECK_VOLUME_ADJUST, deckNum, newValue.floatValue()));
	        }
            
        });
		
		// If a remote DECK_VOLUME_ADJUST comes in (i.e. one for a value other than the current one), move the slider
		EventBus.registerListener(EventType.DECK_VOLUME_ADJUST, (e) -> {
			int deck = (Integer)e.getParams()[0];
			
			if ( deck == deckNum ) {
				float newVolume = (Float)e.getParams()[1];
				
				if ( newVolume - volume.getValue() > 0.01 ) {
					volume.setValue(newVolume);
				}
			}
		});
		
		// By UXWBill's suggestion, make the Play button change to "I'm Feeling Lucky" on a load error :P
		// Also re-enable the buttons so that the DJ can, in fact, test their luck!
		EventBus.registerListener(EventType.DECK_LOAD_ERROR, (e) -> {
			if ( MultiConsole.getDeckMaster().equals(e.getOriginator()) && ((Integer)e.getParams()[1]).intValue() == deckNum ) {
				playButton.setStyle("-fx-base: #F70; -fx-font-size: 12; -fx-font-weight: bold");
				playButton.setText("I'M\nFEELING\nLUCKY");
				playButton.setDisable(false);
				cueButton.setDisable(false);
			}
		});
		
		// Disable the play and cue buttons when a deck load request is placed for this deck
		EventBus.registerListener(EventType.DECK_REQUEST_LOAD, new EventListener() {

			@Override
			public void onEvent(Event e) {
				// TODO prevent inadvertent firing of this event in slave mode
				if ( e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) || e.getOriginator().equals(MultiConsole.getDeckMaster()) || MultiConsole.getDeckMode() == 'M' ) {
					DeckLoadRequest request = (DeckLoadRequest)e.getParams()[0];
					if ( request.getDeckNum() == deckNum ) {
						playButton.setStyle("-fx-base: #0F0; -fx-font-size: 18; -fx-font-weight: bold");
						playButton.setText("PLAY");
						playButton.setDisable(true);
						cueButton.setDisable(true);
						
						duration = request.getDuration();
						remain = duration;
						timer.setText(Utils.tenthsToString(remain));
						artist.setText(request.getArtist());
						title.setText(request.getTitle());
						progress.setProgress(0);
						
						// Reset the progress bar and timer to normal style
						// Fixes bug where deck stopped on red cycle and reloaded would play in solid red
						progress.setStyle("");
						timer.setStyle("-fx-border-color: #BBB;");
					}
				}
			}
			
		});
		
		// Re-enable them when the associated deck sends a Deck Ready event
		EventBus.registerListener(EventType.DECK_READY, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( MultiConsole.getDeckMaster().equals(e.getOriginator()) && ((Integer)e.getParams()[0]).intValue() == deckNum ) {
					playButton.setDisable(false);
					cueButton.setDisable(false);
					
					// If a confirmed duration was passed, store it
					int confirmedDuration = ((Integer)e.getParams()[1]).intValue();
					if ( confirmedDuration != -1 ) {
						duration = confirmedDuration;
						remain = duration;
						timer.setText(Utils.tenthsToString(remain));
					}
				}
			}
			
		});
		
		// When deck playback starts, change the play button to a stop button
		EventBus.registerListener(EventType.DECK_PLAYBACK_STARTED, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( MultiConsole.getDeckMaster().equals(e.getOriginator()) && ((Integer)e.getParams()[0]).intValue() == deckNum ) {
					playButton.setStyle("-fx-base: #F00; -fx-font-size: 18; -fx-font-weight: bold");
					playButton.setText("STOP");
				}
			}
			
		});
		
		// and back again when playback stops
		EventBus.registerListener(EventType.DECK_PLAYBACK_STOPPED, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( MultiConsole.getDeckMaster().equals(e.getOriginator()) && ((Integer)e.getParams()[0]).intValue() == deckNum ) {
					playButton.setStyle("-fx-base: #0F0; -fx-font-size: 18; -fx-font-weight: bold");
					playButton.setText("PLAY");
					
					// Reset progress/timer styles (to avoid "stop on red" issue)
					progress.setStyle("");
					timer.setStyle("-fx-border-color: #BBB;");
				}
			}
			
		});
		
		// Reset counter on cue
		EventBus.registerListener(EventType.DECK_CUE_PRESSED, (e) -> {
			if ( (e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) || e.getOriginator().equals(MultiConsole.getDeckMaster()) || MultiConsole.getDeckMode() == 'M') && ((Integer)e.getParams()[0]).intValue() == deckNum ) {
				progress.setProgress(0);
				remain = duration;
				timer.setText(Utils.tenthsToString(remain));
			}
		});
		
		// Update the timer upon a tick event
		EventBus.registerListener(EventType.DECK_COUNTER_TICK, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( MultiConsole.getDeckMaster().equals(e.getOriginator()) && ((Integer)e.getParams()[0]).intValue() == deckNum ) {
					remain--;
					timer.setText(Utils.tenthsToString(remain));
					int elapsed = duration - remain;
					progress.setProgress((double)elapsed / (double)duration);
					
					// Flash the counter if necessary
					if ( remain < Prefs.loadInt(Prefs.FLASH_POINT) * 10 ) {
						if ( remain % 10 >= 5 ) {
							progress.setStyle("-fx-accent: #F00;");
							timer.setStyle("-fx-text-fill: #F00; -fx-border-color: #BBB;");
						}
						else {
							progress.setStyle("");
							timer.setStyle("-fx-border-color: #BBB;");
						}
					}
				}
			}
			
		});
	}
	
}
