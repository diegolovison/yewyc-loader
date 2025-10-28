package com.github.yewyc.reactornettyhttpclient;

import com.github.yewyc.Task;
import com.github.yewyc.TaskStatus;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientResponse;

public class ReactorNettyHttpClientTask {

    protected static Task createTask1() {
        HttpClient client = HttpClient.create();
        return new Task("http-request-hello") {
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
        };
    }
}
