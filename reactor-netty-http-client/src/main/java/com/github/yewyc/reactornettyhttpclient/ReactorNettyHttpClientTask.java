package com.github.yewyc.reactornettyhttpclient;

import com.github.yewyc.Task;
import reactor.netty.http.client.HttpClient;

public class ReactorNettyHttpClientTask {

    protected static Task createTask1() {
        HttpClient client = HttpClient.create();
        return new Task("http-request-hello", () -> {
            client.get()
                    .uri("http://localhost:8080/hello")
                    .response()
                    .block();
        });
    }
}
