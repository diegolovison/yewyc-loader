package com.github.yewyc.javahttpclient;

import com.github.yewyc.MeasureLatency;
import com.github.yewyc.MeasureLatencyType;
import com.github.yewyc.Task;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.github.yewyc.TheBlackhole.consume;

public class JavaHttpClientSimplePerfTest {

    private static final Logger LOGGER = Logger.getLogger(JavaHttpClientSimplePerfTest.class);

    public static void main(String[] args) {
        JavaHttpClient client = new JavaHttpClient();
        try (MeasureLatency measure = new MeasureLatency(10, 100, 1, 5, MeasureLatencyType.GLOBAL)
                .addTask(createTask1(client)).start()) {
            measure.generateReport().plot();
        }
    }

    protected static Task createTask1(JavaHttpClient client) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("http-request-hello", () -> {
            try {
                HttpResponse<String> response = client.send(request, handler);
                consume(response.statusCode());
                consume(response.body());
            } catch (IOException | InterruptedException e) {
                LOGGER.error(e);
            }
        }, true);
    }

    protected static Task createTask2(JavaHttpClient client) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello/greeting/my-name"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("http-request-my-name", () -> {
            try {
                HttpResponse<String> response = client.send(request, handler);
                consume(response.statusCode());
                consume(response.body());
            } catch (IOException | InterruptedException e) {
                LOGGER.error(e);
            }
        }, true);
    }

    protected static class JavaHttpClient {
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
