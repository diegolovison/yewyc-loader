package com.github.yewyc.javahttpclient;

import com.github.yewyc.MeasureLatency;
import com.github.yewyc.Task;
import com.github.yewyc.TaskStatus;
import com.github.yewyc.WeightTask;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;


public class CumulativeDistributionPerfTest {

    private static final Logger LOGGER = Logger.getLogger(JavaHttpClientTask.class);

    public static void main(String[] args) {

        CumulativeDistributionPerfTest test = new CumulativeDistributionPerfTest();

        try (MeasureLatency measure = new MeasureLatency(60, 100, 1, 60)) {
            measure
                    .addWeightTask(test.getWeightTasks())
                    .start()
                    .generateReport()
                    .plot();
        }
    }

    private Task login;
    private Task unregister;
    private Task viewUser;
    private Task updateUser;
    private Task logout;
    private Task addVehicle;
    private Task deleteVehicle;
    private Task viewQuote;
    private Task acceptQuote;
    private Task viewVehicle;
    private Task acceptQuoteWebSocket;
    private Task viewInsurance;


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
    public CumulativeDistributionPerfTest() {
        this.login = this.login();
        this.unregister = this.unregister();
        this.viewUser = this.viewUser();
        this.updateUser = this.updateUser();
        this.logout = this.logout();
        this.addVehicle = this.addVehicle();
        this.deleteVehicle = this.deleteVehicle();
        this.viewQuote = this.viewQuote();
        this.acceptQuote = this.acceptQuote();
        this.viewVehicle = this.viewVehicle();
        this.acceptQuoteWebSocket = this.acceptQuoteWebSocket();
        this.viewInsurance = this.viewInsurance();
    }

    private List<WeightTask> getWeightTasks() {
        List<WeightTask> tasks = List.of(
            registerInvalid(),
            register(),
            loginUnregister(),
            loginViewLogout(),
            loginViewUpdateLogout(),
            loginAddViewLogout(),
            loginDeleteLogout(),
            loginAcceptViewLogout(),
            loginAcceptSocketViewLogout(),
            loginViewViewLogout()
        );
        return tasks;
    }

    // 10
    public WeightTask loginViewViewLogout() {
        return new WeightTask(List.of(
            this.login,
            this.viewVehicle,
            this.viewInsurance,
            this.logout
        ), 0.10);
    }

    // ok
    public WeightTask loginAcceptSocketViewLogout() {
        return new WeightTask(List.of(
            this.login,
            this.acceptQuoteWebSocket,
            this.viewVehicle,
            this.logout
        ), 0.05);
    }

    // ok
    public WeightTask loginAcceptViewLogout() {
        return new WeightTask(List.of(
            this.login,
            this.acceptQuote,
            this.viewVehicle,
            this.logout
        ), 0.20);
    }

    public WeightTask loginDeleteLogout() {
        return new WeightTask(List.of(
            this.login,
            this.deleteVehicle,
            this.logout
        ), 0.10);
    }

    public WeightTask loginAddViewLogout() {
        return new WeightTask(List.of(
            this.login,
            this.addVehicle,
            this.viewQuote,
            this.logout
        ), 0.25);
    }

    public WeightTask loginViewUpdateLogout() {
        return new WeightTask(List.of(
            this.login,
            this.viewUser,
            this.updateUser,
            this.logout
        ), 0.10);
    }

    public WeightTask loginViewLogout() {
        return new WeightTask(List.of(
            this.login,
            this.viewUser,
            this.logout
        ), 0.05);
    }

    public WeightTask loginUnregister() {
        return new WeightTask(List.of(
            this.login,
            this.unregister
        ), 0.05);
    }

    public WeightTask register() {
        return new WeightTask(List.of(
            //
        ), 0.08);
    }

    public WeightTask registerInvalid() {
        return new WeightTask(List.of(
            //
        ), 0.02);
    }

    public Task login() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("login", () -> {
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
        });
    }

    public Task unregister() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("unregister", () -> {
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
        });
    }

    public Task viewUser() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("viewUser", () -> {
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
        });
    }

    public Task updateUser() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("updateUser", () -> {
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
        });
    }

    public Task logout() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("logout", () -> {
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
        });
    }

    public Task addVehicle() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("addVehicle", () -> {
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
        });
    }

    public Task deleteVehicle() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("deleteVehicle", () -> {
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
        });
    }

    public Task viewQuote() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("viewQuote", () -> {
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
        });
    }

    public Task acceptQuote() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("acceptQuote", () -> {
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
        });
    }

    public Task viewVehicle() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("viewVehicle", () -> {
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
        });
    }

    public Task acceptQuoteWebSocket() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("acceptQuoteWebSocket", () -> {
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
        });
    }

    public Task viewInsurance() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/hello"))
                .GET()
                .build();
        HttpResponse.BodyHandler<String> handler = HttpResponse.BodyHandlers.ofString();
        return new Task("viewInsurance", () -> {
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
        });
    }
}
