package com.github.yewyc.javahttpclient;

import com.github.yewyc.Task;
import com.github.yewyc.TaskStatus;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class JavaHttpClientTask {

    private static final Logger LOGGER = Logger.getLogger(JavaHttpClientTask.class);

    protected static Task createTask1() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("http-request-hello", () -> {
            TaskStatus localStatus;
            try {
                HttpResponse<String> response = client.send(request, handler);
                if (response.statusCode() == 200) {
                    localStatus = TaskStatus.SUCCESS;
                } else {
                    localStatus = TaskStatus.FAILED;
                }
            } catch (IOException | InterruptedException e) {
                localStatus = TaskStatus.FAILED;
            }
            return localStatus;
        });
    }

    protected static Task createTask2() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello/greeting/my-name"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("http-request-my-name", () -> {
            TaskStatus localStatus;
            try {
                HttpResponse<String> response = client.send(request, handler);
                if (response.statusCode() == 200) {
                    localStatus = TaskStatus.SUCCESS;
                } else {
                    localStatus = TaskStatus.FAILED;
                }
            } catch (IOException | InterruptedException e) {
                localStatus = TaskStatus.FAILED;
            }
            return localStatus;
        });
    }
}
