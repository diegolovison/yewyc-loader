package com.github.yewyc.httpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class LocalHttpServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/hello", new HelloHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            t.sendResponseHeaders(200, 0);
            t.getResponseBody().close();
        }
    }
}
