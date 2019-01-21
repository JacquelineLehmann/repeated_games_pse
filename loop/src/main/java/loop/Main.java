package loop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * This class contains the main method of the program and initialises the main window.
 * 
 * @author Peter Koepernik
 *
 */
public class Main extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/view/windows/HomeWindow.fxml"));

        Scene scene = new Scene(root, 1024, 720);

        stage.setTitle("loop");
        stage.setScene(scene);
        stage.show();
    }
    
    /**
     * The entry point of the application. Loads the UI and opens the main window.
     * @param args the commandline arguments (unused)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
