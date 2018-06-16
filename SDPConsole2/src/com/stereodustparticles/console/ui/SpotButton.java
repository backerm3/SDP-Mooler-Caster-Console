/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * SpotButton: Extends StackPane to form the compound control (button + timer) used for each button on the soundboard
 */
package com.stereodustparticles.console.ui;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventListener;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.multi.MultiConsole;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.soundboard.Soundboard;
import com.stereodustparticles.console.soundboard.SpotLoadRequest;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.util.Duration;

public class SpotButton extends StackPane {
	private boolean loading = false;
	private int duration = 0;
	private int remain = 0;
	
	private Button button;
	private Label counter;
	private Timeline timer;
	
	private String color = ""; // in web format
	
	public SpotButton(int row, int col) {
		super();
		
		button = new Button("-");
		counter = new Label("");
		
		// Set control parameters:
		button.setPrefSize(89, 89); // Button size = 89x89 (same as MCC 1.x)
		button.setWrapText(true); // Wrap text (isn't this so much easier than it was in Swing?)
		counter.setPadding(new Insets(2, 2, 2, 2)); // Timer gets 2px of padding on all sides
		counter.setMouseTransparent(true); // Mouse events on the timer should pass through to the button underneath it
		StackPane.setAlignment(counter, Pos.BOTTOM_RIGHT); // Timer goes in the bottom right of the button
		
		// Set counter color to follow button text color
		button.textFillProperty().addListener(new ChangeListener<Paint>() {

			@Override
			public void changed(ObservableValue<? extends Paint> observable, Paint oldValue, Paint newValue) {
				counter.setTextFill(newValue);
			}
			
		});
		
		// Build the context menu
		ContextMenu menu = new ContextMenu();
		
		// - Color picker
		ColorPicker colorPicker = new ColorPicker();
		if ( ! color.isEmpty() ) {
			colorPicker.setValue(Color.web(color));
		}
		colorPicker.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				EventBus.fireEvent(new Event(EventType.SPOT_COLOR_CHANGE, Utils.getWebColor(colorPicker.getValue()), row, col));
			}
			
		});
		MenuItem colorOption = new MenuItem(null, colorPicker);
		
		// - Reset color
		MenuItem resetColor = new MenuItem(null, new Label("Reset Color"));
		resetColor.setOnAction(new EventHandler<ActionEvent>() {
		    
			@Override
		    public void handle(ActionEvent event) {
				EventBus.fireEvent(new Event(EventType.SPOT_COLOR_CHANGE, "", row, col));
		    }
			
		});
		
		// - Clear Spot
		MenuItem clearSpot = new MenuItem(null, new Label("Clear Spot"));
		clearSpot.setOnAction(new EventHandler<ActionEvent>() {
		    
			@Override
		    public void handle(ActionEvent event) {
				EventBus.fireEvent(new Event(EventType.SPOT_CLEAR, row, col));
		    }
			
		});
		
		menu.getItems().addAll(colorOption, resetColor, clearSpot);
		button.setContextMenu(menu);
		
		// Set up listeners on the event bus
		EventBus.registerListener(EventType.SPOT_REQUEST_LOAD, new EventListener() {

			@Override
			public void onEvent(Event e) {
				loading = true;
				counter.setText("Here?");
			}
			
		});
		
		EventBus.registerListener(EventType.SPOT_POS_CHOSEN, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( MultiConsole.getSoundboardMode() == 'A' && ! e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) ) {
					return;
				}
				
				int cRow = ((Integer)e.getParams()[1]).intValue();
				int cCol = ((Integer)e.getParams()[2]).intValue();
				
				if ( row != cRow || col != cCol ) {
					loading = false;
					updateCounter();
				}
				else {
					SpotLoadRequest req = ((SpotLoadRequest)e.getParams()[0]);
					
					loading = false;
					button.setDisable(true);
					counter.setText("Loading...");
					button.setText(req.getTitle());
				}
			}
			
		});
		
		EventBus.registerListener(EventType.SPOT_READY, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( ! e.getOriginator().equals(MultiConsole.getSoundboardMaster()) ) {
					return;
				}
				
				int cRow = ((Integer)e.getParams()[0]).intValue();
				int cCol = ((Integer)e.getParams()[1]).intValue();
				
				if ( row == cRow && col == cCol ) {
					button.setDisable(false);
					duration = ((Integer)e.getParams()[2]).intValue();
					remain = duration;
					updateCounter();
				}
			}
			
		});
		
		EventBus.registerListener(EventType.SPOT_PLAYBACK_STARTED, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( ! e.getOriginator().equals(MultiConsole.getSoundboardMaster()) ) {
					return;
				}
				
				int cRow = ((Integer)e.getParams()[0]).intValue();
				int cCol = ((Integer)e.getParams()[1]).intValue();
				
				if ( row == cRow && col == cCol ) {
					updateButtonStyle(true);
					timer.play();
				}
			}
			
		});
		
		EventBus.registerListener(EventType.SPOT_PLAYBACK_STOPPED, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( ! e.getOriginator().equals(MultiConsole.getSoundboardMaster()) ) {
					return;
				}
				
				int cRow = ((Integer)e.getParams()[0]).intValue();
				int cCol = ((Integer)e.getParams()[1]).intValue();
				
				if ( row == cRow && col == cCol ) {
					updateButtonStyle(false);
					timer.stop();
					remain = duration;
					updateCounter();
				}
			}
			
		});
		
		EventBus.registerListener(EventType.SPOT_LOAD_ERROR, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( ! e.getOriginator().equals(MultiConsole.getSoundboardMaster()) ) {
					return;
				}
				
				int cRow = ((Integer)e.getParams()[1]).intValue();
				int cCol = ((Integer)e.getParams()[2]).intValue();
				
				if ( row == cRow && col == cCol ) {
					button.setStyle("");
					button.setText("-");
					button.setDisable(false);
					timer.stop();
					duration = 0;
					remain = 0;
					updateCounter();
				}
			}
			
		});
		
		EventBus.registerListener(EventType.SPOT_CLEAR, new EventListener() {
			
			// Same actions as for load error
			@Override
			public void onEvent(Event e) {
				if ( MultiConsole.getSoundboardMode() == 'A' && ! e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) ) {
					return;
				}
				
				int cRow = ((Integer)e.getParams()[0]).intValue();
				int cCol = ((Integer)e.getParams()[1]).intValue();
				
				if ( row == cRow && col == cCol ) {
					button.setText("-");
					button.setDisable(false);
					timer.stop();
					duration = 0;
					remain = 0;
					color = "";
					updateCounter();
					updateButtonStyle(false);
				}
			}
			
		});
		
		EventBus.registerListener(EventType.SPOT_COLOR_CHANGE, new EventListener() {
			
			@Override
			public void onEvent(Event e) {
				if ( MultiConsole.getSoundboardMode() == 'A' && ! e.getOriginator().equals(Prefs.loadString(Prefs.MC_IDENTITY)) ) {
					return;
				}
				
				int cRow = ((Integer)e.getParams()[1]).intValue();
				int cCol = ((Integer)e.getParams()[2]).intValue();
				
				if ( row == cRow && col == cCol ) {
					color = (String)e.getParams()[0];
					updateButtonStyle();
				}
			}
			
		});
		
		// Event listener for the button
		button.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent e) {
				// If loading is in progress, fire a position chosen event
				if ( loading ) {
					EventBus.fireEvent(new Event(EventType.SPOT_POS_CHOSEN, Soundboard.getQueuedRequest(), row, col));
				}
				// Otherwise, fire a button pressed event
				else {
					EventBus.fireEvent(new Event(EventType.SPOT_BUTTON_PRESSED, row, col));
				}
			}
			
		});
		
		// Timer for the counter
		timer = new Timeline(new KeyFrame(Duration.millis(100), new EventHandler<ActionEvent>() {

		    @Override
		    public void handle(ActionEvent event) {
		        remain--;
		        updateCounter();
		    }
		    
		}));
		timer.setCycleCount(Timeline.INDEFINITE);
		
		// Add the controls to the StackPane
		getChildren().addAll(button, counter);
	}
	
	// Update the counter text
	private void updateCounter() {
		if ( loading ) return;
		
		if ( duration == 0 ) {
			counter.setText("");
		}
		else {
			counter.setText(Utils.tenthsToString(remain));
		}
	}
	
	// Update the button styles - parameter sets whether border is on or not
	// There is probably a much more elegant way of doing this*, but this will do for now
	private void updateButtonStyle(boolean borderOn) {
		if ( color.isEmpty() ) {
			if ( borderOn ) {
				button.setStyle("-fx-border-color: #0F0;");
			}
			else {
				button.setStyle("");
			}
		}
		else {
			if ( borderOn ) {
				button.setStyle("-fx-base: " + color + "; -fx-border-color: #0F0;");
			}
			else {
				button.setStyle("-fx-base: " + color + ";");
			}
		}
	}
	// * And it probably involves external CSS files...
	
	// Executing the above method with no parameter will try to determine the border state automatically...
	// ** STUPID HACK ALERT! **
	private void updateButtonStyle() {
		updateButtonStyle(remain < duration); // See what I did there?
	}
}
