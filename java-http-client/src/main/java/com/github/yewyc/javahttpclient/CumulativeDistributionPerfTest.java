package com.github.yewyc.javahttpclient;

import com.github.yewyc.*;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import static com.github.yewyc.CallableUtils.callTask;

/**
 * Extract from: https://spec.org/jenterprise2018web/docs/RunRules/#business-transaction-mix-requirements
 *
 * Each Business Transaction that is run by the Insurance Driver is a sequence of operations to the Insurance service.
 * Business Transaction sequences are selected by the Driver based on the mix shown in Table 2.
 *
 * TABLE 2 : Business Transaction Mix Requirements
 *
 * Business Transaction Sequence	                        Percent Mix
 * Register Invalid                                         2
 * Register                                                 8
 * Login / Unregister                                       5
 * Login / View User / Logout                               5
 * Login / View User / Update User / Logout                 10
 * Login / Add Vehicle / View Quote / Logout                25
 * Login / Delete Vehicle / Logout                          10
 * Login / Accept Quote / View Vehicle / Logout             20
 * Login / Accept Quote + WebSocket / View Vehicle / Logout 5
 * Login / View Vehicle / View Insurance / Logout           10
 */
public class CumulativeDistributionPerfTest {

    private static final Logger LOGGER = Logger.getLogger(JavaHttpClientTask.class);

    public static void main(String[] args) {

        CumulativeDistributionPerfTest test = new CumulativeDistributionPerfTest();

        try (Benchmark measure = new Benchmark(60, 100, 1, 60)) {
            measure
                    .addWeightTask(test.getWeightTasks())
                    .start()
                    .generateReport()
                    .plot();
        }
    }

    private List<WeightTask> getWeightTasks() {
        List<WeightTask> tasks = List.of(
            new WeightTask(registerInvalid(), 0.1),
            new WeightTask(register(), 0.1),
            new WeightTask(loginUnregister(), 0.1),
            new WeightTask(loginViewLogout(), 0.1),
            new WeightTask(loginViewUpdateLogout(), 0.1),
            new WeightTask(loginAddViewLogout(), 0.1),
            new WeightTask(loginDeleteLogout(), 0.1),
            new WeightTask(loginAcceptViewLogout(), 0.1),
            new WeightTask(loginAcceptSocketViewLogout(), 0.1),
            new WeightTask(loginViewViewLogout(), 0.1)
        );
        return tasks;
    }

    public Callable<Task> loginViewViewLogout() {
        class LocalTask extends Task {

            private Task login;
            private Task logout;
            private Task viewVehicle;
            private Task viewInsurance;

            public LocalTask() {
                super("");
                this.login = callTask(CumulativeDistributionPerfTest.this.login());
                this.viewVehicle = callTask(CumulativeDistributionPerfTest.this.viewVehicle());
                this.viewInsurance = callTask(CumulativeDistributionPerfTest.this.viewInsurance());
                this.logout = callTask(CumulativeDistributionPerfTest.this.logout());
            }

            @Override
            public TaskStatus run() {
                this.login.run();
                this.viewVehicle.run();
                this.viewInsurance.run();
                this.logout.run();
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    public Callable<Task> loginAcceptSocketViewLogout() {
        class LocalTask extends Task {

            private Task login;
            private Task logout;
            private Task viewVehicle;
            private Task acceptQuoteWebSocket;

            public LocalTask() {
                super("");
                this.login = callTask(CumulativeDistributionPerfTest.this.login());
                this.acceptQuoteWebSocket = callTask(CumulativeDistributionPerfTest.this.acceptQuoteWebSocket());
                this.viewVehicle = callTask(CumulativeDistributionPerfTest.this.viewVehicle());
                this.logout = callTask(CumulativeDistributionPerfTest.this.logout());
            }

            @Override
            public TaskStatus run() {
                this.login.run();
                this.acceptQuoteWebSocket.run();
                this.viewVehicle.run();
                this.logout.run();
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    // ok
    public Callable<Task> loginAcceptViewLogout() {
        class LocalTask extends Task {

            private Task login;
            private Task logout;
            private Task acceptQuote;
            private Task viewVehicle;

            public LocalTask() {
                super("");
                this.login = callTask(CumulativeDistributionPerfTest.this.login());
                this.acceptQuote = callTask(CumulativeDistributionPerfTest.this.acceptQuote());
                this.viewVehicle = callTask(CumulativeDistributionPerfTest.this.viewVehicle());
                this.logout = callTask(CumulativeDistributionPerfTest.this.logout());
            }

            @Override
            public TaskStatus run() {
                this.login.run();
                this.acceptQuote.run();
                this.viewVehicle.run();
                this.logout.run();
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    public Callable<Task> loginDeleteLogout() {
        class LocalTask extends Task {

            private Task login;
            private Task logout;
            private Task deleteVehicle;

            public LocalTask() {
                super("");
                this.login = callTask(CumulativeDistributionPerfTest.this.login());
                this.deleteVehicle = callTask(CumulativeDistributionPerfTest.this.deleteVehicle());
                this.logout = callTask(CumulativeDistributionPerfTest.this.logout());
            }

            @Override
            public TaskStatus run() {
                this.login.run();
                this.deleteVehicle.run();
                this.logout.run();
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    public Callable<Task> loginAddViewLogout() {
        class LocalTask extends Task {

            private Task login;
            private Task logout;
            private Task addVehicle;
            private Task viewQuote;

            public LocalTask() {
                super("");
                this.login = callTask(CumulativeDistributionPerfTest.this.login());
                this.addVehicle = callTask(CumulativeDistributionPerfTest.this.addVehicle());
                this.viewQuote = callTask(CumulativeDistributionPerfTest.this.viewQuote());
                this.logout = callTask(CumulativeDistributionPerfTest.this.logout());
            }

            @Override
            public TaskStatus run() {
                this.login.run();
                this.addVehicle.run();
                this.viewQuote.run();
                this.logout.run();
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    public Callable<Task> loginViewUpdateLogout() {
        class LocalTask extends Task {

            private Task login;
            private Task viewUser;
            private Task updateUser;
            private Task logout;

            public LocalTask() {
                super("");
                this.login = callTask(CumulativeDistributionPerfTest.this.login());
                this.viewUser = callTask(CumulativeDistributionPerfTest.this.viewUser());
                this.updateUser = callTask(CumulativeDistributionPerfTest.this.updateUser());
                this.logout = callTask(CumulativeDistributionPerfTest.this.logout());
            }

            @Override
            public TaskStatus run() {
                this.login.run();
                this.viewUser.run();
                this.updateUser.run();
                this.logout.run();
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    public Callable<Task> loginViewLogout() {
        class LocalTask extends Task {

            private Task login;
            private Task viewUser;
            private Task logout;

            public LocalTask() {
                super("");
                this.login = callTask(CumulativeDistributionPerfTest.this.login());
                this.viewUser = callTask(CumulativeDistributionPerfTest.this.viewUser());
                this.logout = callTask(CumulativeDistributionPerfTest.this.logout());
            }

            @Override
            public TaskStatus run() {
                this.login.run();
                this.viewUser.run();
                this.logout.run();
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    public Callable<Task> loginUnregister() {
        class LocalTask extends Task {

            private Task login;
            private Task unregister;

            public LocalTask() {
                super("");
                this.login = callTask(CumulativeDistributionPerfTest.this.login());
                this.unregister = callTask(CumulativeDistributionPerfTest.this.unregister());
            }

            @Override
            public TaskStatus run() {
                this.login.run();
                this.unregister.run();
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    public Callable<Task> register() {
        class LocalTask extends Task {

            public LocalTask() {
                super("");
            }

            @Override
            public TaskStatus run() {
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    public Callable<Task> registerInvalid() {
        class LocalTask extends Task {

            public LocalTask() {
                super("");
            }

            @Override
            public TaskStatus run() {
                return TaskStatus.SUCCESS;
            }
        }

        return LocalTask::new;
    }

    public Callable<Task> login() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("login");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> unregister() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("unregister");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> viewUser() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("viewUser");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> updateUser() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("updateUser");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> logout() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("logout");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> addVehicle() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("addVehicle");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> deleteVehicle() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("deleteVehicle");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> viewQuote() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("viewQuote");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> acceptQuote() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("acceptQuote");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> viewVehicle() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("viewVehicle");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> acceptQuoteWebSocket() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("acceptQuoteWebSocket");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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

    public Callable<Task> viewInsurance() {

        class LocalTask extends Task {

            private final HttpClient client;
            private final HttpRequest request;
            private final HttpResponse.BodyHandler<String> handler;

            public LocalTask() {
                super("viewInsurance");
                this.client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
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
}
