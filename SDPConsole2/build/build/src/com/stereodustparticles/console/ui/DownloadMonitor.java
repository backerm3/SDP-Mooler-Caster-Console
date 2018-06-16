/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * DownloadMonitor: Displays a progress dialog to monitor file downloads
 * Since this class is typically used from within a worker thread, it runs all its operations "later"
 */
package com.stereodustparticles.console.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DownloadMonitor {
	private Stage stage;
	private ProgressBar progress;
	private Label bytesLabel;
	private int totalBytes;
	
	public DownloadMonitor(String fileName) {
		Platform.runLater(() -> {
			stage = new Stage();
			stage.setTitle("File Download");
			stage.setAlwaysOnTop(true);
			stage.setResizable(false);
			stage.initStyle(StageStyle.UTILITY);
			
			// Prevent window from being closed
			stage.setOnCloseRequest((e) -> e.consume());
			
			VBox main = new VBox(25);
			main.setPadding(new Insets(25, 25, 25, 25));
			
			progress = new ProgressBar();
			progress.setPrefWidth(350);
			
			bytesLabel = new Label("Connecting...");
			
			main.getChildren().addAll(new Label("Downloading file \"" + fileName + "\"..."), progress, bytesLabel);
			
			stage.setScene(new Scene(main));
			stage.show();
		});
	}
	
	public void setFileSize(int size) {
		Platform.runLater(() -> {
			totalBytes = size;
			progress.setProgress(0);
			bytesLabel.setText("0 of " + totalBytes + " bytes completed.");
		});
	}
	
	public void setProgress(int bytes) {
		Platform.runLater(() -> {
			progress.setProgress(((double)bytes / (double)totalBytes));
			bytesLabel.setText(bytes + " of " + totalBytes + " completed.");
		});
	}
	
	public void done() {
		Platform.runLater(() -> stage.close());
	}
}
