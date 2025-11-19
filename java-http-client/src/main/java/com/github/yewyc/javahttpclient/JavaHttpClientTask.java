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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class JavaHttpClientTask {

    private static final Logger LOGGER = Logger.getLogger(JavaHttpClientTask.class);

    public static Callable<Task> task1(Duration connectTimeout, int maxConnections) {

        class LocalTask extends HttpTask {

            private HttpRequest request;
            private HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("http-request-hello", connectTimeout, maxConnections);

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

    public static Callable<Task> task2(Duration connectTimeout, int maxConnections) {

        class LocalTask extends HttpTask {

            private HttpRequest request;
            private HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("http-request-my-name", connectTimeout, maxConnections);
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

    private static abstract class HttpTask extends Task {

        HttpClient client;

        HttpTask(String name, Duration connectTimeout, int maxConnections) {
            super(name);
            ThreadFactory threadFactory = r -> {
                Thread t = new Thread(r);
                t.setName("HttpClient-Worker-" + t.getId());
                return t;
            };
            ExecutorService executor = Executors.newFixedThreadPool(maxConnections, threadFactory);

            this.client = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .executor(executor)
                    .build();
        }
    }
}
