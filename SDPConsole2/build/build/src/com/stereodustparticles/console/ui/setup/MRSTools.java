/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * MRSTools: Defines the MRS Tools dialog
 */
package com.stereodustparticles.console.ui.setup;

import java.io.File;
import java.net.MalformedURLException;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.error.HTTPException;
import com.stereodustparticles.console.error.ModemDefenestrationException;
import com.stereodustparticles.console.library.MRSLibrary;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.ui.Microwave;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MRSTools {
	private Stage stage;
	
	public MRSTools(MRSLibrary mrs) {
		stage = new Stage();
		stage.setTitle("MRS Tools");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		stage.setAlwaysOnTop(true);
		
		VBox root = new VBox(40);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(20, 25, 20, 25));
		
		Button open = new Button("Open Request Lines");
		Button close = new Button("Close Request Lines");
		Button list = new Button("Generate MRS Song List");
		root.getChildren().addAll(open, close, list);
		
		open.setOnAction((e) -> {
			Utils.runInBackground(() -> {
				try {
					mrs.open();
					Platform.runLater(() -> Microwave.showInfo("WHOOOOOO!", "Request lines are now open.", stage));
				}
				catch (MalformedURLException ex) {
					Platform.runLater(() -> Microwave.showException("MRS Communication Error", "A malformed URL was encountered while trying to contact the MRS.  Is your base URL correct?", ex));
				}
				catch (HTTPException ex) {
					Platform.runLater(() -> Microwave.showException("MRS Communication Error", "An HTTP error was returned by the server running the MRS.\n\nCheck that your base URL is correct, and your MRS version is supported.", ex));
				}
				catch (ModemDefenestrationException ex) {
					Platform.runLater(() -> Microwave.showException("MRS Communication Error", "A connection error was encountered while communicating with the MRS.  Your modem or router may require defenestration.", ex));
				}
			});
		});
		
		close.setOnAction((e) -> {
			Utils.runInBackground(() -> {
				try {
					mrs.close();
					Platform.runLater(() -> Microwave.showInfo("Hold The Line!", "Request lines are now closed.", stage));
				}
				catch (MalformedURLException ex) {
					Platform.runLater(() -> Microwave.showException("MRS Communication Error", "A malformed URL was encountered while trying to contact the MRS.  Is your base URL correct?", ex));
				}
				catch (HTTPException ex) {
					Platform.runLater(() -> Microwave.showException("MRS Communication Error", "An HTTP error was returned by the server running the MRS.\n\nCheck that your base URL is correct, and your MRS version is supported.", ex));
				}
				catch (ModemDefenestrationException ex) {
					Platform.runLater(() -> Microwave.showException("MRS Communication Error", "A connection error was encountered while communicating with the MRS.  Your modem or router may require defenestration.", ex));
				}
			});
		});
		
		list.setOnAction((e) -> {
			Microwave.showWarning("A Word of Warning...", "Depending on the size of your libraries, this process may take anywhere from a few minutes to a few months.  Plan accordingly.", stage);
			
	 		FileChooser chooser = new FileChooser();
	 		chooser.setTitle("Save Song List");
	 		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
	 		chooser.setInitialDirectory(new File(Prefs.loadString(Prefs.LAST_LAYOUT_DIR)));
	 		File dest = chooser.showSaveDialog(stage);
	 		if ( dest != null ) {
	 			new MRSListGenerator(dest).start();
	 		}
		});
		
		stage.setScene(new Scene(root));
	}
	
	public void show() {
		stage.show();
	}
}
