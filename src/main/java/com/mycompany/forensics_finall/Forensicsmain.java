package com.mycompany.forensics_finall;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.net.URL;
/**
 * FXML Controller class
 *
 * @author ishan,shereen,devansh
 */
public class Forensicsmain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        URL fxmlLocation = getClass().getResource("splash_screen.fxml");
        // Load the FXML file
        Parent root = FXMLLoader.load(fxmlLocation);

        // Set up the scene and stage
        Scene scene = new Scene(root);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
