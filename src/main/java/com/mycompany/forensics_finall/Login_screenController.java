package com.mycompany.forensics_finall;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Login_screenController implements Initializable {

    @FXML
    private Text ip_add;
    @FXML
    private Text mac_add;
    @FXML
    private TextField email;
    @FXML
    private TextField password;
    @FXML
    private Text error;
    @FXML
    private Button send;
    @FXML
    private Text loginerror;
    @FXML
    private AnchorPane login_page;

    private Connection conn = null;
    private PreparedStatement preparedStatement = null;
    private ResultSet resultSet = null;

    public Login_screenController() {
        conn = connectdb.ConnectDB();
    }

    // Code for MAC and IP
    class IpMac extends Thread {
        @Override 
        public void run() {
            try {
                String currentIpAddress = getCurrentIpAddress();
                String currentMacAddress = getCurrentMacAddress();

                Platform.runLater(() -> {
                    ip_add.setText("IP Address: " + currentIpAddress);
                    mac_add.setText("MAC Address: " + currentMacAddress);
                });
                
            } catch (Exception e) {
                Logger.getLogger(Login_screenController.class.getName()).log(Level.SEVERE, null, e);
                Platform.runLater(() -> error.setText("Error retrieving network details."));
            }
        }
    }

    // Login Function
    private String Login() {
        String status = "Success";
        String e_mail = email.getText();
        String pass = password.getText();
        
        if (e_mail.isEmpty() || pass.isEmpty()) {
            Platform.runLater(() -> error.setText("Empty credentials"));
            status = "Error";
        } else {
            String sql = "SELECT * FROM login_data WHERE email = ? AND password = ?";
            try {
                conn = DriverManager.getConnection("jdbc:sqlite:login.sqlite");
                preparedStatement = conn.prepareStatement(sql);
                preparedStatement.setString(1, e_mail);
                preparedStatement.setString(2, pass);
                resultSet = preparedStatement.executeQuery();

                if (!resultSet.next()) {
                    Platform.runLater(() -> error.setText("Enter Correct Email/Password"));
                    status = "Error";
                } else {
                    // Fetch expected IP and MAC from the database
                    String dbIpAddress = resultSet.getString("ip");
                    String dbMacAddress = resultSet.getString("mac");

                    // Get current IP and MAC addresses
                    String currentIpAddress = getCurrentIpAddress();
                    String currentMacAddress = getCurrentMacAddress();

                    // Check if current IP and MAC match the database
                    if ((dbIpAddress != null && !dbIpAddress.equals(currentIpAddress)) ||
                        (dbMacAddress != null && !dbMacAddress.equals(currentMacAddress))) {
                        Platform.runLater(() -> error.setText("IP or MAC address does not match."));
                        status = "Error";
                    } else {
                        Platform.runLater(() -> loginerror.setText("Login Successful"));
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Login_screenController.class.getName()).log(Level.SEVERE, null, ex);
                status = "Exception";
            }
        }
        return status;
    }

    private String getCurrentIpAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Logger.getLogger(Login_screenController.class.getName()).log(Level.SEVERE, null, e);
        }
        return ""; // Default or error value
    }

    private String getCurrentMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    StringBuilder macAddress = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        macAddress.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    return macAddress.toString();
                }
            }
        } catch (SocketException e) {
            Logger.getLogger(Login_screenController.class.getName()).log(Level.SEVERE, null, e);
        }
        return ""; // Default or error value
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        new IpMac().start(); // Run for MAC and IP showing

        // Add event filters to handle Enter key press
        email.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleButtonAction(new ActionEvent(send, send));
            }
        });

        password.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleButtonAction(new ActionEvent(send, send));
            }
        });
    }

    @FXML
    private void handleButtonAction(ActionEvent event) {
        if (event.getSource() == send) {
            if (Login().equals("Success")) {  // Login Code
                try {
                    FXMLLoader fxmlLoader = new FXMLLoader();
                    fxmlLoader.setLocation(getClass().getResource("menu.fxml"));
                    Scene scene = new Scene(fxmlLoader.load());
                    Stage stage = new Stage();
                    stage.setTitle("New Window");
                    stage.setScene(scene);
                    stage.setResizable(false);
                    stage.show();
                    ((Node) (event.getSource())).getScene().getWindow().hide();
                } catch (IOException e) {
                    Logger logger = Logger.getLogger(getClass().getName());
                    logger.log(Level.SEVERE, "Failed to create new Window.", e);
                }
            } else {
                error.setText("Enter Correct Email/Password");
            }
        }
    }
}
