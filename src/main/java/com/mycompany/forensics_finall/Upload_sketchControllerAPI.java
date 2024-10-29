package com.mycompany.forensics_finall;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import org.json.JSONObject;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Upload_sketchControllerAPI {

    @FXML
    private ImageView sketch;
    @FXML
    private ImageView match;
    @FXML
    private TextField sketchPath;
    @FXML
    private TextField matchPath;
    @FXML
    private TextArea matchProperties;
    @FXML
    private Label matchSimilarity;

    @FXML
    private void handleOpenSketch() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.jpeg", "*.gif"));
        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            String path = file.getAbsolutePath();
            sketchPath.setText(path);
            try {
                BufferedImage bufferedImage = ImageIO.read(file);
                Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                sketch.setImage(image);
            } catch (IOException e) {
                showError("Error opening image", "An error occurred while opening the image.");
            }
        }
    }

    @FXML
    private void handleFindMatch() {
        String apiUrl = "http://localhost:5000/upload_sketch"; // Ensure this is correct
        String fileName = sketchPath.getText();

        try {
            File imageFile = new File(fileName);
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=Boundary");

            try ( OutputStream os = connection.getOutputStream();  PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"))) {

                // Write the file part
                writer.append("--Boundary\r\n");
                writer.append("Content-Disposition: form-data; name=\"sketch\"; filename=\"" + imageFile.getName() + "\"\r\n");
                writer.append("Content-Type: image/jpeg\r\n\r\n");
                writer.flush();

                // Write the file data
                try ( FileInputStream inputStream = new FileInputStream(imageFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                }

                writer.append("\r\n--Boundary--\r\n");
                writer.flush();
            }

            // Read the response from your API
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Process the response
                JSONObject jsonResponse = new JSONObject(response.toString());
                String matchedImageId = jsonResponse.getString("matched_image_id");
                double similarity = jsonResponse.getDouble("similarity");

                // Remove 'Photos/' prefix from the matched image ID
                matchedImageId = matchedImageId.replace("Photos/", "");

                matchPath.setText(matchedImageId);
                matchSimilarity.setText("SIMILARITY : " + similarity);
                matchProperties.setText("****************\nFACE MATCHED\n****************\n\n"
                        + "Name in database: " + matchedImageId
                        + "\nSimilarity: " + similarity + "\n");

                // Construct the corrected URL
                String matchedImageUrl = "http://localhost:5000/" + matchedImageId;
                try {
                    URL url = new URL(matchedImageUrl);
                    BufferedImage matchedImage = ImageIO.read(url);
                    if (matchedImage != null) {
                        Image fxImage = SwingFXUtils.toFXImage(matchedImage, null);
                        match.setImage(fxImage);
                    } else {
                        showError("Image Error", "The retrieved image is null.");
                    }
                } catch (IOException e) {
                    showError("Image Error", "An error occurred while retrieving the image.");
                }
            } else {
                showError("API Error", "Error occurred: " + responseCode);
            }
        } catch (IOException e) {
            showError("Image Error", "An error occurred during image processing: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(message);
        textArea.setWrapText(true);
        textArea.setEditable(false);
        textArea.setPrefSize(600, 400);

        VBox vbox = new VBox(textArea);
        vbox.setSpacing(10);
        alert.getDialogPane().setContent(vbox);

        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.showAndWait();
    }

    @FXML
    private void handleBack(MouseEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader();
            fxmlLoader.setLocation(getClass().getResource("menu.fxml"));
            javafx.scene.Parent root = fxmlLoader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setTitle("Menu");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            Logger logger = Logger.getLogger(getClass().getName());
            logger.log(Level.SEVERE, "Failed to load menu.", e);
        }
    }
}
