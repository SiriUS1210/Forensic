module com.mycompany.forensics_finall {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.sql;
    requires java.logging;
    requires javafx.swing;
    requires org.json;
    
    // Required AWS SDK modules
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.services.rekognition; 
    requires software.amazon.awssdk.services.s3; 
    requires software.amazon.awssdk.regions; // Added for regions package
    requires software.amazon.awssdk.auth;    // Added for auth credentials package

    opens com.mycompany.forensics_finall to javafx.fxml;
    exports com.mycompany.forensics_finall;
}
