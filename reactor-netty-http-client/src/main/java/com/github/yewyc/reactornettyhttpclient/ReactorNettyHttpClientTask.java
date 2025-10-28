package com.github.yewyc.reactornettyhttpclient;

import com.github.yewyc.Task;
import com.github.yewyc.TaskStatus;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

import java.util.concurrent.Callable;

public class ReactorNettyHttpClientTask {

    protected static Callable<Task> createTask1() {

        class LocalTask extends Task {

            private final HttpClient client;

            public LocalTask() {
                super("");
                this.client = HttpClient.create();
            }

            @Override
            public TaskStatus run() {
                TaskStatus localStatus;
                HttpClientResponse respose = client.get()
                        .uri("http://localhost:8080/hello")
                        .response()
                        .block();

                if (respose.status().code() == 200) {
                    localStatus = TaskStatus.SUCCESS;
                } else {
                    localStatus = TaskStatus.FAILED;
                }
                return localStatus;
            }
        }

        return LocalTask::new;
    }
}
