package org.konex.server.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerApp {
    private static final Logger LOGGER = Logger.getLogger(ServerApp.class.getName());
    private static final int DEFAULT_PORT = 12345;
    private final int port;
    private final ExecutorService clientPool = Executors.newCachedThreadPool();

    public ServerApp(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info(() -> "KoneX Server running on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info(() -> "New client connected: " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket);
                clientPool.submit(handler);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server encountered an I/O error", e);
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args != null && args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                LOGGER.warning("Invalid port argument provided, falling back to default");
            }
        }
        new ServerApp(port).start();
    }
}
