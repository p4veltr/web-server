package org.example;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Request {
    private final String method;
    private final String path;
    private final List<String> headers;
    private final List<NameValuePair> queryParams;

    private Request(String method, String path, List<String> headers, List<NameValuePair> queryParams) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.queryParams = queryParams;
    }

    public String getPath() {
        return path;
    }

    public static Request build(BufferedInputStream in) throws IOException {
        final String GET = "GET";
        final String POST = "POST";
        final var allowedMethods = List.of(GET, POST);
        // лимит на request line + заголовки
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return null;
        }

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return null;
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            return null;
        }
        System.out.println(method);

        var requestPath = requestLine[1];
        if (!requestPath.startsWith("/")) {
            return null;
        }
        System.out.println(requestPath);

        // начнём сбор параметров - из пути, исправляем сам путь для будущего объекта Request
        ArrayList<NameValuePair> requestQueryParams = new ArrayList<>();
        if (requestPath.contains("?")) {
            List<NameValuePair> pathParams = URLEncodedUtils.parse(requestPath.substring(requestPath.indexOf("?") + 1), StandardCharsets.UTF_8);
            requestQueryParams.addAll(pathParams);
            requestPath = requestPath.substring(0, requestPath.indexOf("?"));
        }
        final var path = requestPath;

        // ищем заголовки
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return null;
        }

        // отматываем на начало буфера
        in.reset();
        // пропускаем requestLine
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println(headers);

        // для GET тела нет
        var body = "";
        if (!method.equals(GET)) {
            in.skip(headersDelimiter.length);
            // вычитываем Content-Length, чтобы прочитать body
            final var contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final var length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);

                body = new String(bodyBytes);
                System.out.println(body);
                // добавим параметры из тела к общему списку параметров
                List<NameValuePair> bodyParams = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
                requestQueryParams.addAll(bodyParams);
            }
        }
        return new Request(method, path, headers, requestQueryParams);
    }

    public List<NameValuePair> getQueryParams() {
        return this.queryParams;
    }

    public List<NameValuePair> getQueryParam(String name) {
        ArrayList<NameValuePair> queryParam = new ArrayList<>();
        for (NameValuePair pair: this.queryParams) {
            if (pair.getName().equals(name)) {
                queryParam.add(pair);
            }
        }
        return queryParam;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    // from google guava with modifications
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
