package com.phantom.network;

import com.phantom.security.AESCipher;
import com.phantom.security.DatabaseManager; // Importing our new DB bridge!

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class PhantomClient {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 8080;
    
    private static final byte[] TEMP_SHARED_KEY = "PhantomSecretKey1234567890123456".getBytes();

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    
    private PrintWriter serverOut;
    private AESCipher cipherEngine;
    private String username;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PhantomClient::new);
    }

    public PhantomClient() {
        // 1. Create a custom Authentication Panel
        JPanel authPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        authPanel.add(new JLabel("Username:"));
        JTextField userField = new JTextField();
        authPanel.add(userField);
        authPanel.add(new JLabel("Password:"));
        JPasswordField passField = new JPasswordField(); // Hides the password with dots
        authPanel.add(passField);

        // 2. Show the popup with Login and Register buttons
        Object[] options = {"Login", "Register", "Cancel"};
        int result = JOptionPane.showOptionDialog(null, authPanel, "Phantom Talks - Secure Login",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (result == 2 || result == JOptionPane.CLOSED_OPTION) {
            System.exit(0); // User hit Cancel
        }

        username = userField.getText().trim();
        String password = new String(passField.getPassword());

        // 3. Process the Database Authentication
        if (result == 1) { // Register Button Clicked
            if (DatabaseManager.registerUser(username, password)) {
                JOptionPane.showMessageDialog(null, "Registration successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0); // Force them to restart to actually log in
            } else {
                JOptionPane.showMessageDialog(null, "Registration failed. Username might be taken.", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        } else if (result == 0) { // Login Button Clicked
            if (!DatabaseManager.authenticateUser(username, password)) {
                JOptionPane.showMessageDialog(null, "Invalid username or password!", "Access Denied", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }

        // 4. If authentication passes, load the Chat GUI!
        buildChatGUI();
        connectToServer();
    }

    private void buildChatGUI() {
        frame = new JFrame("Phantom Talks - Logged in as: " + username);
        frame.setSize(450, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        chatArea.setBackground(new Color(30, 30, 30));
        chatArea.setForeground(new Color(0, 255, 0));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        inputField.setBackground(new Color(50, 50, 50));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(Color.WHITE);
        
        inputField.addActionListener(e -> sendMessage());
        frame.add(inputField, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            cipherEngine = new AESCipher(TEMP_SHARED_KEY);
            Socket socket = new Socket(SERVER_IP, PORT);
            serverOut = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader serverIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            chatArea.append("--- Securely Connected to PhantomStream ---\n");

            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverIn.readLine()) != null) {
                        String[] parts = serverMessage.split(": ", 2);
                        if (parts.length == 2) {
                            String sender = parts[0];
                            String encryptedPayload = parts[1];
                            String decryptedMessage = cipherEngine.decrypt(encryptedPayload);
                            SwingUtilities.invokeLater(() -> 
                                chatArea.append("[" + sender + "]: " + decryptedMessage + "\n")
                            );
                        }
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> chatArea.append("\n[Error] Connection lost.\n"));
                }
            });
            listenerThread.start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Failed to connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && serverOut != null) {
            try {
                String cipherText = cipherEngine.encrypt(message);
                serverOut.println(username + ": " + cipherText);
                chatArea.append("[You]: " + message + "\n");
                inputField.setText("");
            } catch (Exception e) {
                chatArea.append("[System Error]: Encryption failed.\n");
            }
        }
    }
}