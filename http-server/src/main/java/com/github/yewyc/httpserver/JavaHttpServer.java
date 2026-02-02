package com.github.yewyc.httpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * You can have high throughput and low latency with `wrk2` because of the `TCP Delayed ACK timer`. `wrk2` hides the
 * flaw because the aggressive traffic forces the TCP buffers to flush continuously.
 * <p>
 * By default, the vm option `sun.net.httpserver.nodelay` is false. You can experiment by setting it to true
 */
public class JavaHttpServer {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/hello", new HelloHandler());
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor()); // The number of threads created by the Executor is unbounded
        server.start();
    }

    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String requestId = t.getRequestHeaders().getFirst("X-Request-Id");
            if (requestId == null) {
                requestId = "unknown";
            }
            String response = requestId;
            t.sendResponseHeaders(200, response.length());
            t.getResponseBody().write(response.getBytes());
            t.getResponseBody().close();
        }
    }
}
