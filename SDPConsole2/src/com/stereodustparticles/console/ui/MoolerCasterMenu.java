package com.stereodustparticles.console.ui;

import java.io.File;
import java.io.IOException;

import com.stereodustparticles.console.SDPConsole2;
import com.stereodustparticles.console.Utils;
import com.stereodustparticles.console.deck.Decks;
import com.stereodustparticles.console.error.MRSException;
import com.stereodustparticles.console.event.Event;
import com.stereodustparticles.console.event.EventBus;
import com.stereodustparticles.console.event.EventType;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.mrs.MRSIntegration;
import com.stereodustparticles.console.multi.MultiConsole;
import com.stereodustparticles.console.playlist.Playlist;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.soundboard.Soundboard;
import com.stereodustparticles.console.ui.setup.DeckSetup;
import com.stereodustparticles.console.ui.setup.LibraryManagerUI;
import com.stereodustparticles.console.ui.setup.MCSetup;
import com.stereodustparticles.console.ui.setup.MRSListGenerator;
import com.stereodustparticles.console.ui.setup.MRSSetup;
import com.stereodustparticles.console.ui.setup.MiscSetup;
import com.stereodustparticles.console.ui.setup.PlaylistSetup;
import com.stereodustparticles.console.ui.setup.SoundboardSetup;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * MoolerCasterMenu: Defines the menu bar at the top of the screen
 */

public class MoolerCasterMenu extends MenuBar {
	public MoolerCasterMenu() {
		super();
		setUseSystemMenuBar(true);
		
		// File menu
		Menu file = new Menu("_File");
		file.setMnemonicParsing(true);
		
		// - New Soundboard Layout
		MenuItem newBla = new MenuItem("New Soundboard Layout");
		newBla.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
		newBla.setOnAction((e) -> {
			if ( confirmBoardChange() ) {
				Soundboard.clearAll();
			}
		});
		
		// - Open Soundboard Layout
		MenuItem openBla = new MenuItem("Open Soundboard Layout");
		openBla.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
		openBla.setOnAction((e) -> {
			if ( confirmBoardChange() ) {
				FileChooser chooser = new FileChooser();
				chooser.setTitle("Open Soundboard Layout");
				chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Soundboard Layouts", "*.bl2", "*.bla"));
				File last = new File(Prefs.loadString(Prefs.LAST_LAYOUT_DIR));
				if ( last.exists() ) {
					chooser.setInitialDirectory(last);
				}
				File from = chooser.showOpenDialog(null);
				if ( from != null ) {
					try {
						String ext = Utils.getFileExtension(from);
						if ( ext.toLowerCase().equals("bla") ) {
							Soundboard.loadLegacyLayout(from);
						}
						else {
							if ( ! Soundboard.loadLayout(from) ) {
								Microwave.showWarning("It's too small! (That's what she said!)", "This layout was made for a larger soundboard.  Spots that could not fit were not loaded.\n\nTo load the layout in its entirety, increase the size of your soundboard in Options -> Soundboard Preferences.");
							}
						}
						Prefs.saveString(Prefs.LAST_LAYOUT_DIR, from.getParentFile().toString());
					}
					catch (IOException e1) {
						Microwave.showException("Error Restoring Playlist", "An error occurred while restoring the playlist.  Check that the selected location is valid and readable, then try again.\n\nIf the problem persists, well, that would be your luck, now wouldn't it?", e1);
					}
				}
			}
		});
		
		// - Save Soundboard Layout
		MenuItem saveBla = new MenuItem("Save Soundboard Layout");
		saveBla.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
		saveBla.setOnAction((e) -> saveBoard());
		
		// - Save Soundboard Layout As
		MenuItem saveBlaAs = new MenuItem("Save Soundboard Layout As...");
		saveBlaAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
		saveBlaAs.setOnAction((e) -> saveBoardAs());
		
		// - Refresh Libraries
		MenuItem refreshLibs = new MenuItem("Refresh Libraries");
		refreshLibs.setOnAction((e) -> {
			for ( String libName : LibraryManager.getAvailableLibraries() ) {
				LibraryManager.getLibraryForName(libName).resetCache();
				EventBus.fireEvent(new Event(EventType.LIBRARY_LIST_UPDATED));
			}
		});
		
		// - Export Playlist
		MenuItem exportPlaylist = new MenuItem("Export Current Playlist...");
		exportPlaylist.setOnAction((e) -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Export Playlist");
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
			File last = new File(Prefs.loadString(Prefs.LAST_PLAYLIST_EXPORT_DIR));
			if ( last.exists() ) {
				chooser.setInitialDirectory(last);
			}
			File dest = chooser.showSaveDialog(null);
			if ( dest != null ) {
				try {
					Playlist.export(dest);
					Prefs.saveString(Prefs.LAST_PLAYLIST_EXPORT_DIR, dest.getParentFile().toString());
				}
				catch (IOException e1) {
					Microwave.showException("Error Exporting Playlist", "An error occurred while exporting the playlist.  Check that the selected location is valid and writable, then try again.\n\nIf the problem persists, throw your computer in the nearest swimming pool.", e1);
				}
			}
		});
		
		// - Restore Playlist
		MenuItem restorePlaylist = new MenuItem("Restore Playlist From Backup...");
		restorePlaylist.setOnAction((e) -> {
			FileChooser chooser = new FileChooser();
			chooser.setTitle("Restore Playlist");
			chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Playlist Logs", "*.pll"));
			chooser.setInitialDirectory(new File(System.getProperty("java.io.tmpdir")));
			File from = chooser.showOpenDialog(null);
			if ( from != null ) {
				try {
					Playlist.restoreFromPLL(from);
				}
				catch (IOException e1) {
					Microwave.showException("Error Restoring Playlist", "An error occurred while restoring the playlist.  Check that the selected location is valid and readable, then try again.\n\nIf the problem persists, well, that would be your luck, now wouldn't it?", e1);
				}
			}
		});
		
		// - Exit the Console
		MenuItem exit = new MenuItem("Exit the Console");
		exit.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN));
		exit.setOnAction((e) -> {
			Stage stage = (Stage)(getScene().getWindow());
			stage.close();
		});
		
		file.getItems().addAll(newBla, openBla, new SeparatorMenuItem(), saveBla, saveBlaAs, new SeparatorMenuItem(), refreshLibs, new SeparatorMenuItem(), exportPlaylist, restorePlaylist, new SeparatorMenuItem(), exit);
		
		// Requests menu
		Menu reqs = new Menu("_Requests");
		reqs.setMnemonicParsing(true);
		
		// - View Requests
		MenuItem viewReqs = new MenuItem("View Requests");
		viewReqs.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN));
		viewReqs.setOnAction((e) -> {
			if ( ! MRSIntegration.isConfigured() ) {
				Microwave.showError("You only had ONE JOB!", "MRS integration is not configured.  You need to enter a valid URL and API key.");
				return;
			}
			
			MRSRequestView.show();
		});
		
		// - Open/Close MRS
		MenuItem openClose = new MenuItem("Open/Close MRS");
		openClose.setOnAction((e) -> {
			if ( ! MRSIntegration.isConfigured() ) {
				Microwave.showError("You only had ONE JOB!", "MRS integration is not configured.  You need to enter a valid URL and API key.");
				return;
			}
			
			Utils.runInBackground(() -> {
				try {
					boolean newState = MRSIntegration.toggleStatus();
					
					if ( newState ) {
						Platform.runLater(() -> Microwave.showInfo("WHOOOOOO!", "Request lines are now open."));
					}
					else {
						Platform.runLater(() -> Microwave.showInfo("Hold The Line!", "Request lines are now closed."));
					}
				}
				catch (MRSException e1) {
					Platform.runLater(() -> Microwave.showError("The MRS Doesn't Like You...", "Opening/closing the MRS failed with the following error:\n\n" + e1.getMessage() + "Microwave components as necessary, then try again."));
				}
			});
		});
		
		// - Run MRI Connection Test
		MenuItem test = new MenuItem("Run MRI Connection Test");
		test.setOnAction((e) -> {
			if ( ! MRSIntegration.isConfigured() ) {
				Microwave.showError("You only had ONE JOB!", "MRS integration is not configured.  You need to enter a valid URL and API key.");
				return;
			}
			
			Utils.runInBackground(() -> {
				boolean result = MRSIntegration.test();
				
				if ( result ) {
					Platform.runLater(() -> Microwave.showInfo("You Lucky So-And-So", "Test succeeded - MRS integration should be functioning"));
				}
				else {
					Platform.runLater(() -> Microwave.showError("That Would Be Too Easy!", "The MRI's test sequence failed.  Keep at it, bozo!"));
				}
			});
		});
		
		// - Generate MRS Song List...
		MenuItem songList = new MenuItem("Generate MRS Song List...");
		songList.setOnAction((e) -> {
			Microwave.showWarning("A Word of Warning...", "Depending on the size of your libraries, this process may take anywhere from a few minutes to a few months.  Plan accordingly.");
			
	 		FileChooser chooser = new FileChooser();
	 		chooser.setTitle("Save Song List");
	 		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
	 		chooser.setInitialDirectory(new File(Prefs.loadString(Prefs.LAST_LAYOUT_DIR)));
	 		File dest = chooser.showSaveDialog(null);
	 		if ( dest != null ) {
	 			new MRSListGenerator(dest).start();
	 		}
		});
		
		reqs.getItems().addAll(viewReqs, openClose, test, songList);
		
		// Options menu
		Menu options = new Menu("_Options");
		options.setMnemonicParsing(true);
		
		// - Manage Libraries...
		MenuItem libs = new MenuItem("Manage Libraries...");
		libs.setOnAction((e) -> LibraryManagerUI.show());
		
		// - Soundboard Preferences...
		MenuItem sbSize = new MenuItem("Soundboard Preferences...");
		sbSize.setOnAction((e) -> SoundboardSetup.show());
		
		// - Deck Preferences...
		MenuItem deckPrefs = new MenuItem("Deck Preferences...");
		deckPrefs.setOnAction((e) -> DeckSetup.show());
		
		// - Playlist Preferences...
		MenuItem plPrefs = new MenuItem("Playlist Options...");
		plPrefs.setOnAction((e) -> PlaylistSetup.show());
		
		// - Configure MRS...
		MenuItem configMRS = new MenuItem("Configure MRS...");
		configMRS.setOnAction((e) -> {
			MRSSetup.show();
		});
		
		// - Misc. Settings...
		MenuItem miscPrefs = new MenuItem("Misc. Settings...");
		miscPrefs.setOnAction((e) -> MiscSetup.show());
				
		// - Multi-Console Setup...
		MenuItem mcSetup = new MenuItem("Multi-Console Setup...");
		mcSetup.setOnAction((e) -> MCSetup.show());
		
		// - Enable Stream 'n' Poop(TM)
		CheckMenuItem snp = new CheckMenuItem("Enable Stream 'n' Poop™");
		snp.setOnAction((e) -> {
			Decks.setSNPEnabled(! Decks.snpIsEnabled());
			snp.setSelected(Decks.snpIsEnabled());
		});
		
		options.getItems().addAll(libs, sbSize, deckPrefs, plPrefs, configMRS, miscPrefs, mcSetup, new SeparatorMenuItem(), snp);
		
		// Help menu
		Menu help = new Menu("_Help");
		help.setMnemonicParsing(true);
		
		// - Memory Info
		MenuItem memInfo = new MenuItem("Memory Info");
		memInfo.setAccelerator(new KeyCodeCombination(KeyCode.F12, KeyCombination.CONTROL_DOWN));
		memInfo.setOnAction((e) -> {
			Microwave.showInfo("JVM Memory Info", Utils.getMemoryInfo());
		});
		
		// - Multi-Console Status
		MenuItem mcInfo = new MenuItem("Multi-Console Status");
		mcInfo.setAccelerator(new KeyCodeCombination(KeyCode.F11, KeyCombination.CONTROL_DOWN));
		mcInfo.setOnAction((e) -> {
			Microwave.showInfo("Multi-Console Status", MultiConsole.getStatus());
		});
		
		// - About
		MenuItem about = new MenuItem("About the Mooler Caster Console");
		about.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				Microwave.showInfo("About the Mooler Caster Console", "SDP Mooler Caster Console\nVersion " + SDPConsole2.PROG_VERSION + "\n\nWritten for the Stereo Dust Particles group of broadcasters by Ben Ackerman (IfYouLikeGoodIdeas), 2016-19\n\nIncludes DTC playout technology, © 2019 Ben Ackerman/Stereo Dust Particles.  Mooler Caster Console and Stream 'n' Poop are trademarks of Stereo Dust Particles.  Not that anyone else would even think about using such silly names anyway...");
			}
			
		});
		
		help.getItems().addAll(memInfo, mcInfo, about);
		
		// Add all the finished menus to the bar
		getMenus().addAll(file, reqs, options, help);
	}
	
	// Display the Save Changes dialog, if necessary
	private boolean confirmBoardChange() {
		// If the layout is up-to-date, we can just continue
		if ( Soundboard.layoutIsValid() ) {
			return true;
		}
		
		// Otherwise...
		else {
			if ( Soundboard.getCurrentLayout() == null ) {
				Boolean result = Microwave.confirmChange("Save Changes?", "Save changes to the current soundboard layout?");
				if ( result == null ) return false;
				else if ( result == false ) return true;
				else {
					return saveBoardAs();
				}
			}
			else {
				Boolean result = Microwave.confirmChange("Save Changes?", "Save changes to layout file \"" + Soundboard.getCurrentLayout().getName() + "\"?");
				if ( result == null ) return false;
				else if ( result == false ) return true;
				else {
					return saveBoard();
				}
			}
		}
	}
	
	// Save the current board layout as
	// Return false if cancelled, true otherwise
	private boolean saveBoardAs() {
		FileChooser chooser = new FileChooser();
		chooser.setTitle("Save Soundboard Layout");
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Soundboard Layouts", "*.bl2"));
		File last = new File(Prefs.loadString(Prefs.LAST_LAYOUT_DIR));
		if ( last.exists() ) {
			chooser.setInitialDirectory(last);
		}
		File dest = chooser.showSaveDialog(null);
		if ( dest != null ) {
			try {
				Soundboard.writeLayout(dest);
				Prefs.saveString(Prefs.LAST_LAYOUT_DIR, dest.getParentFile().toString());
				return true;
			}
			catch (IOException e1) {
				Microwave.showException("Error Saving Layout", "An error occurred while saving the soundboard layout.  Check that the selected location is valid and writable, then try again.\n\nIf the problem persists, you may need to kick your computer.", e1);
				return false;
			}
		}
		else {
			return false;
		}
	}
	
	// Save the current board layout
	// Return false if cancelled, true otherwise
	private boolean saveBoard() {
		if ( Soundboard.getCurrentLayout() == null ) {
			return saveBoardAs();
		}
		else {
			try {
				Soundboard.writeLayout();
				return true;
			}
			catch ( IOException e1 ) {
				Microwave.showException("Error Saving Layout", "An error occurred while saving the soundboard layout.  Check that the selected location is valid and writable, then try again.\n\nIf the problem persists, take your computer to your nearest Stereo Dust Particles store for a prompt clubbing.", e1);
				return false;
			}
		}
	}
}
