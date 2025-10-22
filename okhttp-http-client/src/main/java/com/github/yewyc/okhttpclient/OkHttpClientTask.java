package com.github.yewyc.okhttpclient;

import com.github.yewyc.Task;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jboss.logging.Logger;

import java.io.IOException;

import static com.github.yewyc.TheBlackhole.consume;

public class OkHttpClientTask {

    private static final Logger LOGGER = Logger.getLogger(OkHttpClientTask.class);

    protected static Task createTask1() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://localhost:8080/hello")
                .build();
        return new Task("http-request-hello", () -> {
            try (Response response = client.newCall(request).execute()) {

                consume(response.isSuccessful());
                consume(response.body());
            } catch (IOException e) {
                LOGGER.error(e);
            }
        });
    }
}
