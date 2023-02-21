package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int serverPort;
    private final ExecutorService service;

    public Server(int serverPort, int fixedThreadPoolSize) {
        this.serverPort = serverPort;
        this.service = Executors.newFixedThreadPool(fixedThreadPoolSize);
    }

    public void start() {
        try (final var server = new ServerSocket(serverPort))
        {
            while (true) {
                Socket socket = server.accept();
                service.submit(new ServerConnection(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось установить подключение! " + e);
        }
    }
}
