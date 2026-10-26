package com.phantom.network;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;

public class PhantomServer {
    private static final int PORT = 8080;
    private static CopyOnWriteArrayList<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    
    
    private static final String LOG_FILE = "server_audit.log";

    public static void main(String[] args) {
        logEvent("[SERVER] Phantom Server initialized. Listening on port " + PORT + "...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                logEvent("[SERVER] New connection established from IP: " + clientSocket.getInetAddress());

                
                ClientHandler clientThread = new ClientHandler(clientSocket);
                activeClients.add(clientThread);
                new Thread(clientThread).start();
            }
        } catch (IOException e) {
            logEvent("[SERVER ERROR] Exception: " + e.getMessage());
        }
    }

    
    public static synchronized void logEvent(String message) {
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timeStampedMessage = "[" + LocalDateTime.now().format(dtf) + "] " + message;
        
        
        System.out.println(timeStampedMessage);
        
    
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(LOG_FILE, true)))) {
            out.println(timeStampedMessage);
        } catch (IOException e) {
            System.err.println("CRITICAL: Failed to write to audit log!");
        }
    }

    
    public static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : activeClients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    
    public static void removeClient(ClientHandler client) {
        activeClients.remove(client);
        logEvent("[SERVER] A client has disconnected. Active users remaining: " + activeClients.size());
    }
}


class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            PhantomServer.logEvent("[HANDLER ERROR] Stream failure: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            String incomingMessage;
            while ((incomingMessage = reader.readLine()) != null) {
                // Log the encrypted ciphertext to the file
                PhantomServer.logEvent("[TRAFFIC] Intercepted: " + incomingMessage);
                PhantomServer.broadcastMessage(incomingMessage, this);
            }
        } catch (IOException e) {
            
        } finally {
            closeConnections();
        }
    }

    public void sendMessage(String message) {
        writer.println(message);
    }

    private void closeConnections() {
        PhantomServer.removeClient(this);
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}