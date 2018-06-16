/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * MCSetup: Defines the Multi-Console Setup dialog
 */
package com.stereodustparticles.console.ui.setup;

import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.multi.SerialUtils;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.ui.Microwave;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class MCSetup {
	private static Stage stage = null;
	private static String sbMode;
	private static String deckMode;
	private static String plMode;
		
	private static void init() {
		stage = new Stage();
		stage.setTitle("Multi-Console Setup");
		stage.setResizable(false);
		stage.initStyle(StageStyle.UTILITY);
		
		sbMode = Prefs.loadString(Prefs.MC_SOUNDBOARD_MODE);
		deckMode = Prefs.loadString(Prefs.MC_DECK_MODE);
		plMode = Prefs.loadString(Prefs.MC_PLAYLIST_MODE);
		
		VBox root = new VBox(8);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(10, 15, 20, 15));
		
		TabPane tabs = new TabPane();
		tabs.setMaxWidth(450);
		
		// Identity tab
		Tab identityTab = new Tab("Identity");
		identityTab.setClosable(false);
		
		VBox idSet = new VBox(30);
		idSet.setAlignment(Pos.CENTER_LEFT);
		idSet.setPadding(new Insets(20, 35, 20, 35));
		TextField consoleID = new TextField(Prefs.loadString(Prefs.MC_IDENTITY));
		consoleID.setMaxWidth(170);
		Label explanation = new Label("This must be set uniquely for each console in the network.  Having two consoles with the same identity may summon nasal demons.");
		explanation.setWrapText(true);
		idSet.getChildren().addAll(new Label("This console should identify itself as:"), consoleID, explanation);
		
		identityTab.setContent(idSet);
		tabs.getTabs().add(identityTab);
		
		// Functions tab
		Tab functionsTab = new Tab("Functions");
		functionsTab.setClosable(false);
		
		VBox funcRoot = new VBox(40);
		funcRoot.setAlignment(Pos.CENTER_LEFT);
		funcRoot.setPadding(new Insets(20, 15, 20, 15));
		
		HBox funcSet = new HBox(8);
		funcSet.setAlignment(Pos.CENTER_LEFT);
		ChoiceBox<String> funcMenu = new ChoiceBox<String>(FXCollections.observableArrayList("Soundboard", "Decks", "Playlist"));
		funcSet.getChildren().addAll(new Label("Function:"), funcMenu);
		funcRoot.getChildren().add(funcSet);
		
		VBox modeSel = new VBox(8);
		final ToggleGroup modeOpts = new ToggleGroup();
		RadioButton standAlone = new RadioButton("Stand-Alone");
		RadioButton master = new RadioButton("Master");
		RadioButton slave = new RadioButton("Slave");
		standAlone.setToggleGroup(modeOpts);
		master.setToggleGroup(modeOpts);
		slave.setToggleGroup(modeOpts);
		standAlone.setUserData('A');
		master.setUserData('M');
		slave.setUserData('S');
		modeSel.getChildren().addAll(new Label("Mode:"), standAlone, master, slave);
		funcRoot.getChildren().add(modeSel);
		
		HBox slaveToSet = new HBox(8);
		slaveToSet.disableProperty().bind(slave.selectedProperty().not());
		slaveToSet.setAlignment(Pos.CENTER_LEFT);
		TextField masterID = new TextField();
		slaveToSet.getChildren().addAll(new Label("Slave to the following console:"), masterID);
		funcRoot.getChildren().add(slaveToSet);
		
		// Listeners for these controls
		funcMenu.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
			String newSetStr;
			if ( newVal.equals("Soundboard") ) {
				newSetStr = sbMode;
			}
			else if ( newVal.equals("Decks") ) {
				newSetStr = deckMode;
			}
			else if ( newVal.equals("Playlist") ) {
				newSetStr = plMode;
			}
			else {
				// This should never happen
				System.err.println("IfYouLikeGoodIdeas only had ONE JOB!  MCSetup, funcMenu change listener, else #1");
				return;
			}
			
			char mode = newSetStr.charAt(0);
			String slaveTo = newSetStr.substring(1);
			
			if ( mode == 'A' ) {
				standAlone.setSelected(true);
			}
			else if ( mode == 'M' ) {
				master.setSelected(true);
			}
			else if ( mode == 'S' ) {
				slave.setSelected(true);
			}
			else {
				// This should never happen
				System.err.println("IfYouLikeGoodIdeas only had ONE JOB!  MCSetup, funcMenu change listener, else #2");
				return;
			}
			
			masterID.setText(slaveTo);
		});
		
		modeOpts.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
			if ( modeOpts.getSelectedToggle() != null ) {
				String mode = modeOpts.getSelectedToggle().getUserData().toString();
				String func = funcMenu.getSelectionModel().getSelectedItem();
				if ( func.equals("Soundboard") ) {
					sbMode = mode + masterID.getText();
				}
				else if ( func.equals("Decks") ) {
					deckMode = mode + masterID.getText();
				}
				else if ( func.equals("Playlist") ) {
					plMode = mode + masterID.getText();
				}
			}
		});
		
		masterID.textProperty().addListener((obs, oldVal, newVal) -> {
			if ( modeOpts.getSelectedToggle() != null ) {
				String mode = modeOpts.getSelectedToggle().getUserData().toString();
				String func = funcMenu.getSelectionModel().getSelectedItem();
				if ( func.equals("Soundboard") ) {
					sbMode = mode + masterID.getText();
				}
				else if ( func.equals("Decks") ) {
					deckMode = mode + masterID.getText();
				}
				else if ( func.equals("Playlist") ) {
					plMode = mode + masterID.getText();
				}
			}
		});
		
		funcMenu.getSelectionModel().selectFirst();
		
		functionsTab.setContent(funcRoot);
		tabs.getTabs().add(functionsTab);
		
		// Links tab
		Tab linksTab = new Tab("Links");
		linksTab.setClosable(false);
		
		VBox linksRoot = new VBox(40);
		linksRoot.setAlignment(Pos.CENTER_LEFT);
		linksRoot.setPadding(new Insets(20, 35, 20, 35));
		
		VBox outSet = new VBox(12);
		outSet.setAlignment(Pos.CENTER_LEFT);
		CheckBox outEnable = new CheckBox("Make an outbound TCP connection to another console");
		outEnable.setSelected(Prefs.loadBoolean(Prefs.MC_OUTBOUND_ENABLE));
		outSet.getChildren().add(outEnable);
		
		HBox outIPSet = new HBox(8);
		outIPSet.disableProperty().bind(outEnable.selectedProperty().not());
		outIPSet.setAlignment(Pos.CENTER_LEFT);
		TextField outboundIP = new TextField(Prefs.loadString(Prefs.MC_OUTBOUND_IP));
		outboundIP.setMaxWidth(100);
		TextField outboundPort = new TextField(Integer.toString(Prefs.loadInt(Prefs.MC_OUTBOUND_PORT)));
		outboundPort.setMaxWidth(60);
		outIPSet.getChildren().addAll(new Label("IP:"), outboundIP, new Label(" Port:"), outboundPort);
		outSet.getChildren().add(outIPSet);
		
		linksRoot.getChildren().add(outSet);
		
		VBox inSet = new VBox(12);
		inSet.setAlignment(Pos.CENTER_LEFT);
		CheckBox inEnable = new CheckBox("Listen for inbound TCP connections from other consoles");
		inEnable.setSelected(Prefs.loadBoolean(Prefs.MC_INBOUND_ENABLE));
		inSet.getChildren().add(inEnable);
		
		HBox inIPSet = new HBox(8);
		inIPSet.disableProperty().bind(inEnable.selectedProperty().not());
		inIPSet.setAlignment(Pos.CENTER_LEFT);
		TextField inboundPort = new TextField(Integer.toString(Prefs.loadInt(Prefs.MC_INBOUND_PORT)));
		inboundPort.setMaxWidth(60);
		inIPSet.getChildren().addAll(new Label("Port:"), inboundPort);
		inSet.getChildren().add(inIPSet);
		
		linksRoot.getChildren().add(inSet);
		
		VBox serialSet = new VBox(12);
		serialSet.setAlignment(Pos.CENTER_LEFT);
		CheckBox serialEnable = new CheckBox("Connect to another console over a serial port");
		serialEnable.setSelected(Prefs.loadBoolean(Prefs.MC_SERIAL_ENABLE));
		serialSet.getChildren().add(serialEnable);
		
		HBox serialPortSet = new HBox(8);
		serialPortSet.disableProperty().bind(serialEnable.selectedProperty().not());
		serialPortSet.setAlignment(Pos.CENTER_LEFT);
		ChoiceBox<String> serialPort = new ChoiceBox<String>();
		String[] ports = SerialUtils.getPortNames();
		if ( ports != null ) {
			serialPort.setItems(FXCollections.observableArrayList(ports));
			serialPort.getSelectionModel().select(Prefs.loadString(Prefs.MC_SERIAL_PORT));
		}
		else {
			serialEnable.setSelected(false);
			serialEnable.setDisable(true);
		}
		serialPortSet.getChildren().addAll(new Label("Port:"), serialPort);
		serialSet.getChildren().add(serialPortSet);
		
		linksRoot.getChildren().add(serialSet);
		
		linksTab.setContent(linksRoot);
		tabs.getTabs().add(linksTab);
		
		root.getChildren().add(tabs);
		
		// "Butts"
		HBox buttBar = new HBox(8);
		buttBar.setAlignment(Pos.CENTER);
		Button ok = new Button("OK");
		Button cancel = new Button("Cancel");
		ok.setPrefWidth(80);
		cancel.setPrefWidth(80);
		buttBar.getChildren().addAll(ok, cancel);
		
		// Butt event handlers
		ok.setOnAction((e) -> {
			// Validate port numbers
			int inPort, outPort;
			try {
				inPort = Integer.parseInt(inboundPort.getText());
				outPort = Integer.parseInt(outboundPort.getText());
			}
			catch ( NumberFormatException ex ) {
				Microwave.showError("You only had ONE JOB!", "The port numbers have to be, well, numbers, you know!", stage);
				return;
			}
			
			if ( inPort <= 0 || inPort > 65535 || outPort <= 0 || outPort > 65535 ) {
				Microwave.showError("You only had ONE JOB!", "Port numbers have to be between 1 and 65535, silly!", stage);
				return;
			}
			
			// Validate IP address
			if ( ! Utils.validIP(outboundIP.getText()) ) {
				Microwave.showError("You only had ONE JOB!", "That doesn't look like an IP address to me!", stage);
				return;
			}
			
			// Save preferences
			Prefs.saveInt(Prefs.MC_INBOUND_PORT, inPort);
			Prefs.saveInt(Prefs.MC_OUTBOUND_PORT, outPort);
			Prefs.saveString(Prefs.MC_OUTBOUND_IP, outboundIP.getText());
			Prefs.saveString(Prefs.MC_SERIAL_PORT, serialPort.getSelectionModel().getSelectedItem());
			Prefs.saveBoolean(Prefs.MC_INBOUND_ENABLE, inEnable.isSelected());
			Prefs.saveBoolean(Prefs.MC_OUTBOUND_ENABLE, outEnable.isSelected());
			Prefs.saveBoolean(Prefs.MC_SERIAL_ENABLE, serialEnable.isSelected());
			
			Prefs.saveString(Prefs.MC_SOUNDBOARD_MODE, sbMode);
			Prefs.saveString(Prefs.MC_DECK_MODE, deckMode);
			Prefs.saveString(Prefs.MC_PLAYLIST_MODE, plMode);
			
			Prefs.saveString(Prefs.MC_IDENTITY, consoleID.getText());
			
			Microwave.showInfo("Settings Changed", "Restart the Console for these changes to take effect.\n\nWeirdness may ensue if you don't - you have been warned!");
			
			stage.close();
			stage = null;
		});
		
		cancel.setOnAction((e) -> {
			// I'm lazy, so we'll just ditch this stage on canceling so it will be regenerated with the right options next time
			stage.close();
			stage = null;
		});
		
		root.getChildren().add(buttBar);
		
		stage.setScene(new Scene(root));
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
