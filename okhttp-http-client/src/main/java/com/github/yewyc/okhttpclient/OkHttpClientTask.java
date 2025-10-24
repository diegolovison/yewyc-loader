package com.github.yewyc.okhttpclient;

import com.github.yewyc.Task;
import com.github.yewyc.TaskStatus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.logging.Logger;

import java.io.IOException;

public class OkHttpClientTask {

    private static final Logger LOGGER = Logger.getLogger(OkHttpClientTask.class);

    protected static Task createTask1() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://localhost:8080/hello")
                .build();
        return new Task("http-request-hello", () -> {
            TaskStatus localStatus;
            try {
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        localStatus = TaskStatus.SUCCESS;
                    } else {
                        localStatus = TaskStatus.FAILED;
                    }
                }
            } catch (IOException e) {
                localStatus = TaskStatus.FAILED;
            }
            return localStatus;
        });
    }
}
