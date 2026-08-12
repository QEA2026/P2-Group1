package com.rev.manager.repository;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.sql.Connection;
import java.sql.DriverManager;

public class AWSDatabaseConnection {
    // SSH Server Configuration
    private static final String SSH_HOST = "3.15.165.162";
    private static final int SSH_PORT = 22;
    private static final String SSH_USER = "ec2-user";
    private static final String PATH_TO_KEY = /*System.getenv("user.dir")+*/"..\\secret\\p2-database.pem";
    //private static final String SSH_PASSWORD = "ssh_password";

    // Database Configuration (Relative to the SSH Server)
    private static final String DB_HOST = "database-p2.cx2cyck8swfj.us-east-2.rds.amazonaws.com"; // 'localhost' if DB is on the same SSH server
    private static final int DB_PORT = 5432;
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "tW83Wfee7NQaWp47h0eZ";
    private static final String DB_NAME = "";

    private static final int LOCAL_PORT = 3307; // Arbitrary free local port
    private static int portOffset = 0;

    public static Connection get_aws_connection(){
        Session sshSession = null;
        Connection dbConnection = null;

        try{
            JSch jsch = new JSch();
            jsch.addIdentity(PATH_TO_KEY);
            sshSession = jsch.getSession(SSH_USER, SSH_HOST, SSH_PORT);
            sshSession.getConfig(PATH_TO_KEY);

            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            sshSession.setConfig(config);

            //System.out.println("Establishing SSH connection...");
            sshSession.connect();
            //System.out.println("SSH Connection established.");

            sshSession.setPortForwardingL((LOCAL_PORT+portOffset), DB_HOST, DB_PORT);
            //System.out.println("Port forwarding set up: localhost:" + LOCAL_PORT + " -> " + DB_HOST + ":" + DB_PORT);
            //System.out.println("The port is: "+sshSession.getPort());

            String jdbcUrl = "jdbc:postgresql://localhost:" + (LOCAL_PORT+portOffset) + "/" + DB_NAME;
            portOffset += 1;
            //System.out.println("Connecting to database via JDBC...");
            //System.out.println(jdbcUrl);
            dbConnection = DriverManager.getConnection(jdbcUrl, DB_USER, DB_PASSWORD);
            //System.out.println("Database connection successful!");
        } catch (Exception e){
            e.printStackTrace();
        }
        return dbConnection;
    }
}