package com.mycompany.forensics_finall;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Upload_sketchControllerAWS {

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
    private javafx.scene.control.Label matchSimilarity;

    private static final Region CLIENT_REGION = Region.US_EAST_2;
    private static final String BUCKET_NAME = "forensics-bucket-project";

    // Your AWS credentials
    private static final String AWS_ACCESS_KEY_ID = "xyz";
    private static final String AWS_SECRET_ACCESS_KEY = "zyx";

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
        String fileName = sketchPath.getText();
        if (fileName == null || fileName.isEmpty()) {
            showError("File Selection Error", "Please select an image first.");
            return;
        }

        // Upload to S3
        String uploadedFileName = new File(fileName).getName(); // Get the file name to use as the key in S3
        try (S3Client s3Client = S3Client.builder()
                .region(CLIENT_REGION)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)))
                .build()) {

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(uploadedFileName) // Use the uploaded file name
                    .contentType("image/jpeg")
                    .build();

            s3Client.putObject(request, RequestBody.fromFile(new File(fileName)));
            showInfo("Upload Successful", "Sketch uploaded successfully.");

        } catch (Exception e) {
            showError("Upload Error", "The sketch could not be uploaded: " + e.getMessage());
            return;
        }

        // Proceed with face matching
        String collectionId = "Records";

        try (RekognitionClient rekognitionClient = RekognitionClient.builder()
                .region(CLIENT_REGION)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)))
                .build()) {

            SearchFacesByImageRequest searchFacesByImageRequest = SearchFacesByImageRequest.builder()
                    .collectionId(collectionId)
                    .image(software.amazon.awssdk.services.rekognition.model.Image.builder()
                            .s3Object(S3Object.builder()
                                    .bucket(BUCKET_NAME)
                                    .name(uploadedFileName) // Use the uploaded file name
                                    .build())
                            .build())
                    .faceMatchThreshold(70F)
                    .maxFaces(3)
                    .build();

            SearchFacesByImageResponse searchFacesByImageResult = rekognitionClient.searchFacesByImage(searchFacesByImageRequest);
            List<FaceMatch> faceImageMatches = searchFacesByImageResult.faceMatches();

            if (faceImageMatches.isEmpty()) {
                showInfo("No Match Found", "No match found in the database.");
                match.setImage(null);
                return;
            }

            for (FaceMatch face : faceImageMatches) {
                matchPath.setText(face.face().externalImageId());
                matchSimilarity.setText("SIMILARITY: " + face.similarity());
                matchProperties.setText("******" +
                        "FACE MATCHED" +
                        "******\n" +
                        "\n" +
                        "Name in database: " + face.face().externalImageId() +
                        "\n" +
                        "Similarity: " + face.similarity() +
                        "\n" +
                        "Confidence: " + face.face().confidence() + "\n");

                String externalImageId = face.face().externalImageId();
                System.out.println("External Image ID: " + externalImageId);

                String path = constructS3ImageUrl(externalImageId);
                System.out.println("Image URL: " + path);

                try {
                    URL url = new URL(path);
                    BufferedImage img = ImageIO.read(url);
                    if (img != null) {
                        Image fxImage = SwingFXUtils.toFXImage(img, null);
                        match.setImage(fxImage);
                    } else {
                        showError("Image Error", "The retrieved image is null.");
                    }
                } catch (IOException e) {
                    showError("Image Error", "An error occurred while retrieving the image: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            showError("Match Error", "An error occurred during the face match process: " + e.getMessage());
        }
    }

    private String constructS3ImageUrl(String externalImageId) {
        String cleanedExternalImageId = externalImageId.replace("Photos_", "");
        return "https://" + BUCKET_NAME + ".s3." + CLIENT_REGION.id() + ".amazonaws.com/Photos/" + cleanedExternalImageId;
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
