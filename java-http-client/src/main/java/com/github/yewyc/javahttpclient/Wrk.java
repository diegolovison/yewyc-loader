package com.github.yewyc.javahttpclient;

import com.github.yewyc.Benchmark;
import com.github.yewyc.Task;
import com.github.yewyc.TaskStatus;
import com.github.yewyc.WeightTask;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Callable;

public class Wrk {

    static void main() {
        int timeout = 2;
        int threads = 2;
        int connections = 10;
        int duration = 30;

        Duration connectTimeout = Duration.ofSeconds(timeout);
        try (Benchmark benchmark = new Benchmark(duration, threads)) {
            benchmark
                    .addTask(
                            new WeightTask(task(connectTimeout, connections))
                    )
                    .start()
                    .generateReport()
                    .plot();
        }
    }

    private static Callable<Task> task(Duration connectTimeout, int maxConnections) {

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
}
