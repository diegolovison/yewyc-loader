package com.github.yewyc.javahttpclient;

import com.github.yewyc.Task;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public abstract class HttpTask extends Task {

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
