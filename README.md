# yewyc-loader

**y**ou **e**at **w**hat **y**ou **c**ook **load** **generator**

yewyc-loader is a lightweight and flexible load generator designed to measure the latency of your applications. It allows you to define tasks, control the load (operations per second and virtual threads), and generate reports with latency metrics and plots.

## Core Concepts

### Tasks

At the heart of yewyc-loader are **Tasks**. A Task represents a unit of work you want to perform and measure the latency for. You define a Task by providing:

- **Name**: A descriptive name for your task (e.g., "http-request-hello").
- **Action**: A `Runnable` that encapsulates the actual work to be done (e.g., making an HTTP request).
- **Track Data (optional)**: A boolean flag to enable detailed latency tracking for plotting.

### MeasureLatency

The `MeasureLatency` class is the engine that drives the load generation and latency measurement. It takes the following parameters:

- **Time (seconds)**: The duration of the load test.
- **Operations Per Second (opsPerSec)**: The desired rate at which tasks should be executed.
- **Virtual Threads**: The number of virtual threads to use for concurrent task execution. Leveraging Java 21's virtual threads for high concurrency with minimal overhead.
- **Warm-up Time (seconds)**:  A period before the actual measurement to allow the system to reach a steady state.
- **MeasureLatencyType**: Defines how latency is measured when multiple tasks are executed within a single operation:
    - `GLOBAL`: Measures the latency from the start of the first task to the end of the last task in an operation.
    - `INDIVIDUAL`: Measures the latency of each task individually within an operation, excluding the execution time of previous tasks in the same operation.

### Reporting and Plotting

After running a load test, `MeasureLatency` can:

- **Generate Reports**:  Prints latency metrics to the console, including:
    - Total Operations
    - Average, Minimum, and Maximum Latency
    - Percentile Latencies (90th, 95th, 99th, 99.9th, 99.99th)
    - Guidance on adjusting `opsPerSec` based on average latency.
- **Plot Latency**: Generates interactive HTML plots of latency over time using Tablesaw-Plotly, providing visual insights into latency distribution.

## Examples

### java-http-client

The `java-http-client` module demonstrates how to use yewyc-loader to measure the latency of HTTP requests using Java 11's `HttpClient`.

#### SUT (System Under Test) - Quarkus Getting Started Application

This example uses the [Quarkus Getting Started](https://github.com/quarkusio/quarkus-quickstarts/tree/main/getting-started) application as the System Under Test (SUT).

1. **Clone the Quarkus Quickstarts repository:**

   ```bash
   $ git clone https://github.com/quarkusio/quarkus-quickstarts.git
   $ cd quarkus-quickstarts
   ```

2. **Navigate to the Getting Started example:**

   ```bash
   $ cd getting-started
   ```

3. **Start Quarkus in development mode:**

   ```bash
   $ quarkus dev
   ```

   Wait until you see the "Tests paused" message in the console, indicating the application is ready.

#### Running the `JavaHttpClientExample`

1. **Navigate to the `java-http-client` example directory in `yewyc-loader`:**

   ```bash
   $ cd path/to/yewyc-loader/java-http-client
   ```

2. **Run the `JavaHttpClientExample.java` class:**

   You can run it directly from your IDE or using Maven:

   ```bash
   $ mvn compile exec:java -Dexec.mainClass="com.github.yewyc.javahttpclient.JavaHttpClientExample"
   ```

   This will execute a load test for 60 seconds with 1000 operations per second using 1 virtual thread, targeting the Quarkus application running at `http://localhost:8080`. It will measure GLOBAL latency and generate a report and plot.

   You should see output in the console with latency metrics and an HTML file (`target/tablesaw/plots/`) will be generated containing the interactive latency plot.

#### `JavaHttpClientExample.java` Code Breakdown

```java
// ... imports ...

public class JavaHttpClientExample {

    private static final Logger LOGGER = Logger.getLogger(JavaHttpClientExample.class);

    public static void main(String[] args) {

        JavaHttpClient client = new JavaHttpClient();

        // Task 1: HTTP GET request to /hello
        Task task1 = new Task("http-request-hello", () -> {
            // ... code to make HTTP request to /hello ...
        }, true); // trackData = true for plotting

        // Task 2: HTTP GET request to /hello/greeting/my-name
        Task task2 = new Task("http-request-my-name", () -> {
            // ... code to make HTTP request to /hello/greeting/my-name ...
        }, true); // trackData = true for plotting

        // Configure and start the load test
        MeasureLatency measure = new MeasureLatency(60, 1000, 1, 5, MeasureLatencyType.GLOBAL)
                .addTask(task1, task2) // Add the tasks to be executed
                .start();              // Start the load test

        measure.generateReport().plot(); // Generate report and plot after test completion
    }

    // ... JavaHttpClient class ...
}
```

## Contributing

[Contribution guidelines will be added here in the future.]


