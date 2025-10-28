package com.github.yewyc.okhttpclient;

import com.github.yewyc.Task;
import com.github.yewyc.TaskStatus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.concurrent.Callable;

public class OkHttpClientTask {

    private static final Logger LOGGER = Logger.getLogger(OkHttpClientTask.class);

    protected static Callable<Task> createTask1() {

        class LocalTask extends Task {

            private final OkHttpClient client;
            private final Request request;

            public LocalTask() {
                super("");
                this.client = new OkHttpClient();
                this.request = new Request.Builder()
                        .url("http://localhost:8080/hello")
                        .build();
            }

            @Override
            public TaskStatus run() {
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
            }
        }

        return LocalTask::new;
    }
}
