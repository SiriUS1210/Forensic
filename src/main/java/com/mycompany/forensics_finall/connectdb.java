package com.mycompany.forensics_finall;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class connectdb {
    Connection conn = null;
    public static Connection ConnectDB()
    {
        try 
        {
            Class.forName("org.sqlite.JDBC");
            Connection conn;
            conn = DriverManager.getConnection("jdbc:sqlite:login.sqlite");
            return conn;
        }catch (SQLException | ClassNotFoundException e) {
            System.err.println("JavaConnect : "+e.getMessage());            
            return null;
          }
    }
}
