package com.mycompany.forensics_finall;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class MenuController implements Initializable {

    @FXML
    private Rectangle sketch;
    @FXML
    private Rectangle upload;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    @FXML // Open the sketch dashboard   
    private void sketch(MouseEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("dashboard.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = new Stage();
            stage.setTitle("Sketch Dashboard");
            stage.setScene(scene);
            stage.setMaximized(true); // Maximize the window
            stage.setResizable(true); // Allow resizing, hence minimize and maximize
            stage.show();
            ((Node) (event.getSource())).getScene().getWindow().hide();
        } catch (IOException e) {
            Logger logger = Logger.getLogger(getClass().getName());
            logger.severe("Failed to load the FXML file: " + e.getMessage());
        }
    }

    @FXML // Open upload sketch window
    private void upload(MouseEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("upload_sketch.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            Stage stage = new Stage();
            stage.setTitle("Upload Sketch");
            stage.setScene(scene);
            
            stage.setResizable(true); // Allow resizing, hence minimize and maximize
            stage.show();
            ((Node) (event.getSource())).getScene().getWindow().hide();
        } catch (IOException e) {
            Logger logger = Logger.getLogger(getClass().getName());
            logger.severe("Failed to load the FXML file: " + e.getMessage());
        }
    }
}
