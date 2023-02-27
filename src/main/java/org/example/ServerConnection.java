package org.example;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;

public class ServerConnection implements Callable<Boolean> {
    private final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
    private final Socket socket;

    public ServerConnection(Socket socket) {
        this.socket = socket;
    }

    @Override
    public Boolean call() throws Exception {
        try (
                final var socket = this.socket;
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            Request request = Request.build(in);
            if (request == null) {
                badRequest(out);
                return false;
            }

            System.out.println("Тест получения всех параметров: " + request.getQueryParams());
            System.out.println("Тест получения параметра value: " + request.getQueryParam("value"));
            System.out.println("Тест получения параметра title: " + request.getQueryParam("title"));

            handleRequest(request, out);
        }
        return false;
    }

    private void handleRequest(Request request, BufferedOutputStream outStream) throws IOException {
        if (!validPaths.contains(request.getPath())) {
            notFoundRequest(outStream);
            return;
        }

        final var filePath = Path.of(".", "public", request.getPath());
        final var mimeType = Files.probeContentType(filePath);

        // special case for classic
        if (request.getPath().equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            okRequest(outStream, mimeType, content.length);
            outStream.write(content);
            outStream.flush();
            return;
        }

        final var length = Files.size(filePath);
        okRequest(outStream, mimeType, length);
        Files.copy(filePath, outStream);
        outStream.flush();
    }

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static void notFoundRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    private static void okRequest(BufferedOutputStream out, String mimeType, long contentLength) throws IOException {
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + contentLength + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
    }

}
