package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    final static int serverPort = 9999;
    final static int fixedThreadPoolSize = 64;

    public static void main(String[] args) {
        var server = new Server(serverPort, fixedThreadPoolSize);
        server.start();
    }
}