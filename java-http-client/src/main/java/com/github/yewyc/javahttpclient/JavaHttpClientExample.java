package com.github.yewyc.javahttpclient;

import com.github.yewyc.MeasureLatency;
import com.github.yewyc.Task;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.github.yewyc.TheBlackhole.consume;

public class JavaHttpClientExample {

    private static final Logger LOGGER = Logger.getLogger(JavaHttpClientExample.class);

    public static void main(String[] args) {

        JavaHttpClient client = new JavaHttpClient();

        Task task1 = new Task("hello", () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/hello"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                consume(response.statusCode());
                consume(response.body());
            } catch (IOException | InterruptedException e) {
                LOGGER.error(e);
            }
        });

        Task task2 = new Task("my-name", () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/hello/greeting/my-name"))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                consume(response.statusCode());
                consume(response.body());
            } catch (IOException | InterruptedException e) {
                LOGGER.error(e);
            }
        });

        new MeasureLatency(60, 1000, 1).addTask(task1, task2).start().generateReport();
    }

    public static class JavaHttpClient {
        private final HttpClient client;
        public JavaHttpClient() {
            this.client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
        }

        public HttpResponse<String> send(HttpRequest request, HttpResponse.BodyHandler<String> handler) throws IOException, InterruptedException {
            HttpResponse<String> response = client.send(request, handler);
            return response;
        }
    }
}
