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
import java.util.concurrent.Callable;

public class JavaHttpClientTask {

    private static final Logger LOGGER = Logger.getLogger(JavaHttpClientTask.class);

    public static Callable<Task> task1(Duration connectTimeout) {

        class LocalTask extends Task {

            private HttpClient client;
            private HttpRequest request;
            private HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("http-request-hello");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .build();
                this.request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/hello"))
                        .GET()
                        .build();
                this.handler = HttpResponse.BodyHandlers.ofString();
            }

            @Override
            public TaskStatus run() {
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
            }
        }

        return LocalTask::new;
    }

    public static Callable<Task> task2(Duration connectTimeout) {

        class LocalTask extends Task {

            private HttpClient client;
            private HttpRequest request;
            private HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("http-request-my-name");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(connectTimeout)
                        .build();
                this.request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/hello/greeting/my-name"))
                        .GET()
                        .build();
                this.handler = HttpResponse.BodyHandlers.ofString();
            }

            @Override
            public TaskStatus run() {
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
            }
        }

        return LocalTask::new;
    }
}
