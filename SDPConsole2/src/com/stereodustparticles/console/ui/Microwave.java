/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * Microwave: supplies methods for handling errors :P
 * 
 * Most of the alert box tricks are taken or adapted from http://code.makery.ch/blog/javafx-dialogs-official/
 */
package com.stereodustparticles.console.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Optional;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventListener;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.multi.MultiConsole;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

public class Microwave {
	
	// Show a message - used internally by the other methods
	private static void showMessage(AlertType type, String title, String msg, Window owner) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.initOwner(owner);

		alert.showAndWait();
	}
	
	// Show an error message
	public static void showError(String title, String msg, Window owner) {
		showMessage(AlertType.ERROR, title, msg, owner);
	}
	public static void showError(String title, String msg) {
		showError(title, msg, null);
	}
	
	// Show a warning message
	public static void showWarning(String title, String msg, Window owner) {
		showMessage(AlertType.WARNING, title, msg, owner);
	}
	public static void showWarning(String title, String msg) {
		showWarning(title, msg, null);
	}
	
	// Show an information message
	public static void showInfo(String title, String msg, Window owner) {
		showMessage(AlertType.INFORMATION, title, msg, owner);
	}
	public static void showInfo(String title, String msg) {
		showInfo(title, msg, null);
	}
	
	// Confirm an action
	// Returns true if OK was pressed, false if Cancel was pressed
	public static boolean confirmAction(String title, String msg, Window owner) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setContentText(msg);
		alert.initOwner(owner);

		Optional<ButtonType> result = alert.showAndWait();
		return (result.get() == ButtonType.OK);
	}
	public static boolean confirmAction(String title, String msg) {
		return confirmAction(title, msg, null);
	}
	
	// Display a Yes/No/Cancel dialog (typically Save Changes)
	// true = Yes, false = No, null = Cancel
	// (Yes, I really did just use the Boolean wrapper object as a ternary value...)
	public static Boolean confirmChange(String title, String msg) {
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(title);
		alert.setContentText(msg);

		alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);

		Optional<ButtonType> result = alert.showAndWait();
		if ( result.get() == ButtonType.YES ) {
			return true;
		}
		else if ( result.get() == ButtonType.NO ) {
			return false;
		}
		else {
			return null;
		}
	}
	
	// Show an exception
	public static void showException(String title, String msg, Throwable ex) {
		showException(title, msg, ex, null);
	}
	public static void showException(String title, String msg, Throwable ex, Window owner) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.initOwner(owner);

		// Create expandable Exception.
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ex.printStackTrace(pw);
		String exceptionText = sw.toString();

		Label label = new Label("Exception stack trace:");

		TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);

		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);

		GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);

		// Set expandable Exception into the dialog pane.
		alert.getDialogPane().setExpandableContent(expContent);

		alert.showAndWait();
	}
	
	// Show a password dialog
	public static String getPassword(String title, String msg) {
		Dialog<String> dialog = new Dialog<>();
		dialog.setTitle(title);
		
		dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
		
		HBox bar = new HBox(10);
		bar.setAlignment(Pos.CENTER_LEFT);
		bar.setPadding(new Insets(20, 150, 10, 10));
		
		PasswordField password = new PasswordField();
		bar.getChildren().addAll(new Label(msg), password);
		dialog.getDialogPane().setContent(bar);
		
		Platform.runLater(() -> password.requestFocus());
		
		dialog.setResultConverter(dialogButton -> {
		    if (dialogButton == ButtonType.OK) {
		        return password.getText();
		    }
		    return null;
		});
		
		Optional<String> result = dialog.showAndWait();
		if ( result.isPresent() ) {
			return result.get();
		}
		else {
			return "";
		}
	}
	
	// Initialize the microwave (register listeners for event-based errors)
	public static void init() {
		// Deck playback error
		EventBus.registerListener(EventType.DECK_PLAYBACK_ERROR, new EventListener() {

			@Override
			public void onEvent(Event e) {
				// TODO do we really want this (and the other errors) to show on all consoles in the group?
				if ( e.getOriginator().equals(MultiConsole.getDeckMaster()) ) {
					Exception ex = (Exception)e.getParams()[0];
					Microwave.showException("Deck Playback Error", "Deck playback failed with the following error: " + ex.getMessage() + "\n\nMicrowave your music playback computer, then try again.", ex);
				}
			}
			
		});
		
		// Deck load errors
		EventBus.registerListener(EventType.DECK_LOAD_ERROR, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( e.getOriginator().equals(MultiConsole.getDeckMaster()) ) {
					Exception ex = (Exception)e.getParams()[0];
					final String msgTitle = "Error Loading Deck";
					
					if ( ex instanceof UnsupportedAudioFileException ) {
						showException(msgTitle, "The file you selected is of an unsupported format.", ex);
					}
					else if ( ex instanceof IllegalArgumentException ) {
						showException(msgTitle, "The audio output could not be opened.\n\nCheck that your sound is working properly, and that the file is of a supported format.", ex);
					}
					else if ( ex instanceof IOException ) {
						showException(msgTitle, "Could not load the deck: " + ex.getMessage() + "\n\nThe file may be corrupt.  Microwave it, then try again.", ex);
					}
					else if ( ex instanceof LineUnavailableException ) {
						showException(msgTitle, "The audio line is unavailable for some reason.  Try clubbing your computer.", ex);
					}
					else {
						showException(msgTitle, "An unknown error occurred while loading the deck.  A clubbing is most definitely in order.", ex);
					}
				}
			}
			
		});
		
		// Spot load errors
		EventBus.registerListener(EventType.SPOT_LOAD_ERROR, new EventListener() {

			@Override
			public void onEvent(Event e) {
				if ( e.getOriginator().equals(MultiConsole.getSoundboardMaster()) ) {
					Exception ex = (Exception)e.getParams()[0];
					final String msgTitle = "Error Loading Spot";
					
					if ( ex instanceof UnsupportedAudioFileException ) {
						showException(msgTitle, "The file you selected is of an unsupported format.", ex);
					}
					else if ( ex instanceof IllegalArgumentException ) {
						showException(msgTitle, "The audio output could not be opened.\n\nCheck that your sound is working properly, and that the file is of a supported format.", ex);
					}
					else if ( ex instanceof IOException ) {
						showException(msgTitle, "Could not load the spot: " + ex.getMessage() + "\n\nThe file may be corrupt.  Microwave it, then try again.", ex);
					}
					else if ( ex instanceof LineUnavailableException ) {
						showException(msgTitle, "The audio line is unavailable for some reason.  Try clubbing your computer.", ex);
					}
					else if ( ex instanceof MalformedURLException ) {
						showException(msgTitle, "An invalid URL was passed when loading the spot.  There may be a problem with the library, or at the very least, someone only had ONE JOB!", ex);
					}
					else {
						showException(msgTitle, "An unknown error occurred while loading the spot.  A clubbing is most definitely in order.", ex);
					}
				}
			}
		});
	}
}
