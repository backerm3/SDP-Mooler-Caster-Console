/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * LibraryManagerUI: Houses the main library management dialog
 */
package com.stereodustparticles.console.ui.setup;

import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.ui.Microwave;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LibraryManagerUI {
	private static Stage stage = null;
	private static ListView<String> libList = null;
	
	// Build the window
	// Called automatically when needed (see below)
	private static void init() {
		stage = new Stage();
		stage.setTitle("Library Manager");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		stage.setAlwaysOnTop(true);
		
		VBox root = new VBox(8);
		root.setAlignment(Pos.CENTER_LEFT);
		root.setPadding(new Insets(10, 15, 10, 15));
		
		root.getChildren().add(new Label("Configured Libraries:"));
		
		libList = new ListView<String>();
		refreshList();
		root.getChildren().add(libList);
		
		HBox libOpsBar = new HBox(8);
		Button add = new Button("Add");
		Button edit = new Button("Edit");
		Button del = new Button("Delete");
		edit.setDisable(true);
		del.setDisable(true);
		libOpsBar.getChildren().addAll(add, edit, del);
		root.getChildren().add(libOpsBar);
		
		libList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if ( newValue == null ) {
					edit.setDisable(true);
					del.setDisable(true);
				}
				else {
					edit.setDisable(false);
					del.setDisable(false);
				}
			}
			
		});
		
		add.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				new LibraryEditor(null).show();
			}
			
		});
		
		edit.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				new LibraryEditor(libList.getSelectionModel().getSelectedItem()).show();
			}
			
		});
		
		del.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				if ( Microwave.confirmAction("Confirm Library Deletion", "Are you sure you want to delete this library?", stage) ) {
					LibraryManager.removeLibrary(libList.getSelectionModel().getSelectedItem());
					refreshList();
					EventBus.fireEvent(new Event(EventType.LIBRARY_LIST_UPDATED));
				}
			}
			
		});
		
		stage.setScene(new Scene(root));
	}
	
	// Refresh the list of libraries
	public static void refreshList() {
		libList.setItems(FXCollections.observableArrayList(LibraryManager.getAvailableLibraries()));
	}
	
	// Show the dialog
	// This is what gets called from outside
	public static void show() {
		if ( stage == null ) {
			init();
		}
		
		stage.show();
	}
}
