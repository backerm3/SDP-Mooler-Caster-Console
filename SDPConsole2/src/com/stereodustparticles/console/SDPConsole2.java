package com.stereodustparticles.console;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import com.stereodustparticles.console.deck.Decks;
import com.stereodustparticles.console.library.LibraryManager;
import com.stereodustparticles.console.mrs.MRSIntegration;
import com.stereodustparticles.console.multi.MultiConsole;
import com.stereodustparticles.console.playlist.Playlist;
import com.stereodustparticles.console.pref.Prefs;
import com.stereodustparticles.console.soundboard.Soundboard;
import com.stereodustparticles.console.ui.Microwave;
import com.stereodustparticles.console.ui.MoolerCasterView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/*
 * SDP Mooler Caster Console - version 2
 * Simple DJ software for "Mooler Casting" operations
 * 
 * Written by Ben Ackerman (IfYouLikeGoodIdeas) for Stereo Dust Particles, 2017
 * 
 * SDPConsole2: This is the main class that initializes the application and holds
 * any application-wide code that may be necessary.
 */

public class SDPConsole2 extends Application {
	// Global Constants
	public static final String PROG_VERSION = "2.02"; // Program version constant
	
	// Global random number generator
	public static Random random = new Random();
	
	// Close request handler
	private EventHandler<WindowEvent> checkBoardAndCleanUp = event -> {
        if ( ! Soundboard.layoutIsValid() ) {
        	if ( ! confirmBoardChange() ) {
        		event.consume();
        		return;
        	}
        }
        
        MultiConsole.cleanUp();
        Decks.cleanUp();
    };
    
    // Global uncaught exception handler
    // http://www.nomachetejuggling.com/2006/06/13/java-5-global-exception-handling/
    class GremlinHandler implements Thread.UncaughtExceptionHandler {
    	
		  public void uncaughtException(Thread t, Throwable e) {
			  	// Print to stderr first, in case JavaFX is hosed
			    System.err.println("IfYouLikeGoodIdeas must be immediately clubbed for not catching the following exception:");
			    e.printStackTrace();
			  
			  	Platform.runLater(() -> {
			    	Microwave.showException("Code Gremlins Detected!", "A miscellaneous error has occurred.  This is most likely because IfYouLikeGoodIdeas has done something dumb, or didn't foresee anyone doing what you just did.\n\nCopy the details below, and pass them on to IfYouLikeGoodIdeas when you send him to the white courtesy club.", e);
			    });
		  }
	}
    
    // TODO Fix this so we don't have to have this code in 2 places!
    
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
 		chooser.setInitialDirectory(new File(Prefs.loadString(Prefs.LAST_LAYOUT_DIR)));
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
	
	// "Main" method - startup method (wrapper) for non-JavaFX-aware environments
    public static void main(String[] args) {
        launch(args);
    }
    
    // The *real* startup method
    @Override
    public void start(Stage primaryStage) {
    	// Set global overrides
    	Thread.setDefaultUncaughtExceptionHandler(new GremlinHandler());
    	primaryStage.setOnCloseRequest(checkBoardAndCleanUp);
    	
    	// First, let's take care of some backend initialization
    	// NOTE: Prefs must be initialized first!
    	Prefs.init();
    	LibraryManager.init();
    	Microwave.init();
    	Decks.init();
    	Playlist.init();
    	Soundboard.init();
    	MultiConsole.init();
    	MRSIntegration.init();
    	
    	// Set window title
    	primaryStage.setTitle("SDP Mooler Caster Console v" + PROG_VERSION);
        
    	// Set up the Mooler Caster View!
    	MoolerCasterView mcv = new MoolerCasterView();
    	primaryStage.setScene(mcv.getScene());
        
        // Show the window
        primaryStage.show();
        
        // Make sure the stage is not taller than the screen (it will be by default on most monitors)
    	double screenHeight = Screen.getPrimary().getVisualBounds().getHeight();
    	if ( primaryStage.getHeight() > screenHeight ) {
    		primaryStage.setHeight(screenHeight);
    		primaryStage.setY(0); // Also move it down from off the top of the screen
    	}
    	
    	// If a startup layout has been set, load it
    	String startBla = Prefs.loadString(Prefs.STARTUP_LAYOUT);
    	if ( ! startBla.isEmpty() ) {
    		File bla = new File(startBla);
    		
    		// Check that the file actually exists
    		if ( ! bla.exists() ) {
    			Microwave.showError("Error Loading Startup Layout", "The specified startup layout file doesn't exist!  Are you sure you entered a sane value for that setting?");
    		}
    		
    		String blaExt = Utils.getFileExtension(bla);
    		try {
    			if ( blaExt.toLowerCase().equals("bla") ) {
        			Soundboard.loadLegacyLayout(bla);
        		}
        		else {
        			Soundboard.loadLayout(bla);
        		}
    		}
    		catch (IOException e) {
    			Microwave.showException("Error Loading Startup Layout", "An error occurred while loading the specified startup layout.  Are you REALLY sure the file is valid?", e);
    		}
    	}
    }
}
