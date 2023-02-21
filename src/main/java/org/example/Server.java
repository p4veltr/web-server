package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    ExecutorService service;

    public void start() {
        service = Executors.newFixedThreadPool(64);
        try (final var server = new ServerSocket(9999))
        {
            while (true) {
                Socket socket = server.accept();
                service.submit(new ServerConnection(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
