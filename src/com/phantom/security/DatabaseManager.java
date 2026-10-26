package com.phantom.security;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.MessageDigest;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public class DatabaseManager {
    
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    private static final String USER = "system";
    private static final String PASS = "Phantom123";

    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    
    public static String hashPassword(String plainTextPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainTextPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash password", e);
        }
    }

    
    public static boolean registerUser(String username, String rawPassword) {
        String sql = "INSERT INTO phantom_users (username, password_hash) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(rawPassword));
            pstmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Registration failed (User might already exist): " + e.getMessage());
            return false;
        }
    }

    
    public static boolean authenticateUser(String username, String rawPassword) {
        String sql = "SELECT password_hash FROM phantom_users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                String loginHash = hashPassword(rawPassword);
                
                return storedHash.equals(loginHash);
            }
            return false; 
            
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Authentication failed: " + e.getMessage());
            return false;
        }
    }
}