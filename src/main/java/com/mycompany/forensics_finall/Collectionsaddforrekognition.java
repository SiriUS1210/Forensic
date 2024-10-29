package com.mycompany.forensics_finall;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object; // No alias needed
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import java.util.ArrayList;
import java.util.List;

public class Collectionsaddforrekognition {

    private static final Region CLIENT_REGION = Region.US_EAST_2;
    private static final String AWS_ACCESS_KEY_ID = "xyz"; // Replace with your AWS access key
    private static final String AWS_SECRET_ACCESS_KEY = "xyz"; // Replace with your AWS secret key
    private static final String BUCKET_NAME = "forensics-bucket-project"; // Your S3 bucket name
    private static final String COLLECTION_ID = "Records"; // Your Rekognition collection ID
    private static final String PHOTOS_FOLDER = "Photos/"; // The folder containing images

    public static void main(String[] args) {
        Collectionsaddforrekognition app = new Collectionsaddforrekognition();

        // Retrieve images from S3 bucket
        List<String> imageNames = app.getImagesFromS3();

        // Add images to the Rekognition collection
        app.addImagesToCollection(imageNames);
    }

    private List<String> getImagesFromS3() {
        List<String> imageNames = new ArrayList<>();

        try ( S3Client s3Client = S3Client.builder()
                .region(CLIENT_REGION)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)))
                .build()) {

            // Create a request to list objects in the specified S3 folder
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(BUCKET_NAME)
                    .prefix(PHOTOS_FOLDER)
                    .build();

            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            // Add each object key to the imageNames list
            for (S3Object s3Object : listResponse.contents()) { // Use the S3Object class directly
                String objectKey = s3Object.key();
                // Filter for image file types if necessary (e.g., jpg, png)
                if (objectKey.endsWith(".jpg") || objectKey.endsWith(".png")) {
                    imageNames.add(objectKey);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error retrieving images from S3: " + e.getMessage());
        }

        return imageNames;
    }

    private void addImagesToCollection(List<String> imageNames) {
    try (RekognitionClient rekognitionClient = RekognitionClient.builder()
            .region(CLIENT_REGION)
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)))
            .build()) {

        for (String imageName : imageNames) {
            // Get the filename without path
            String externalImageId = imageName.replaceAll("[^a-zA-Z0-9_.\\-:]", "_");

            software.amazon.awssdk.services.rekognition.model.S3Object rekognitionS3Object = software.amazon.awssdk.services.rekognition.model.S3Object.builder()
                    .bucket(BUCKET_NAME)
                    .name(imageName)
                    .build();

            IndexFacesRequest indexFacesRequest = IndexFacesRequest.builder()
                    .collectionId(COLLECTION_ID)
                    .image(software.amazon.awssdk.services.rekognition.model.Image.builder()
                            .s3Object(rekognitionS3Object)
                            .build())
                    .externalImageId(externalImageId) // Use the modified external image ID
                    .detectionAttributesWithStrings(List.of("ALL"))
                    .build();

            IndexFacesResponse indexFacesResponse = rekognitionClient.indexFaces(indexFacesRequest);

            if (!indexFacesResponse.faceRecords().isEmpty()) {
                System.out.println("Successfully added face to collection: " + externalImageId);
                indexFacesResponse.faceRecords().forEach(faceRecord -> {
                    System.out.println("Face ID: " + faceRecord.face().faceId());
                });
            } else {
                System.out.println("No faces detected in the image: " + imageName);
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        System.err.println("Error adding images to collection: " + e.getMessage());
    }
}


}
