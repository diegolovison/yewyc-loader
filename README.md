## Creating a Benchmark

This guide explains how to create a performance benchmark using the yewyc-loader framework.

### Overview

A benchmark consists of three main components:

1. **Steps** - Individual operations (e.g., login, search, create booking)
2. **Sequences** - Ordered collections of steps representing user workflows
3. **Benchmark Main Class** - Orchestrates the load test execution

### Step 1: Define Your Steps

Create step classes that extend [`Step`](core/src/main/java/com/github/yewyc/step/Step.java). Each step represents a single operation:

```java
public class LoginStep extends Step {
    @Override
    public CompletionStage<Void> execute(ScenarioContext ctx, EventLoop loop, long id) {

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.email = "user@example.com";
        loginRequest.password = "password123";

        byte[] requestBody = getRequestBody(loginRequest);

        String baseUrl = ctx.get("baseUrl");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + ENDPOINT_PATH))
                .header(HEADER_CONTENT_TYPE, HEADER_VALUE_JSON)
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        HttpClient httpClient = ctx.get("httpClient");
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAcceptAsync(response -> {
                    LoginResponse res = parseResponse(response);
                    if (res.token != null) {
                        // Store response data in context for subsequent steps
                        ctx.put("authToken", res.token);
                    } else {
                        throw new RuntimeException("Token missing in response");
                    }
                }, loop);
    }
}
```

**Key Points:**
- Use [`ScenarioContext`](core/src/main/java/com/github/yewyc/scenario/ScenarioContext.java) to share data between steps
- Return `CompletionStage<Void>` for async execution
- Store results in context for use by subsequent steps

### Step 2: Create Sequences

Combine steps into sequences that represent complete user workflows. Each sequence has a weight determining its execution frequency:

```java
public class BookRoomSequence extends Sequence {
    public BookRoomSequence() {
        super(25, List.of(  // 25% of all operations
            new LoginStep(),
            new RoomSearchStep(),
            new CreateBookingStep(),
            new LogoutStep()
        ));
    }
}
```

**Weight Distribution Example:**
- Registration: 5%
- Profile View: 15%
- Search: 30%
- Booking: 25%
- Review: 10%
- Cancellation: 4%
- Account Closure: 1%

### Step 3: Create the Benchmark Main Class

Set up the benchmark configuration and execution:

```java
public class MyBenchmark {
    public static void main(String[] args) {
        // Configuration
        int threads = 2;
        int connections = 10;
        String url = "http://localhost:8080";
        Duration duration = Duration.ofSeconds(60);
        
        BenchmarkRecord record = new BenchmarkRecord(
            threads,                        // threads
            duration,                       // test duration
            1000,                           // rate
            connections,                    // concurrent connections
            Duration.ofSeconds(6),          // warmup duration
            LoadStrategy.RATE_LIMIT         // load strategy
        );
        
        // Define scenario factory
        ScenarioFactory scenarioFactory = (loop) -> {
            List<Sequence> sequences = List.of(
                new RegistrationSequence(),
                new SearchSequence(),
                new BookingSequence()
            );
            
            Scenario scenario = new Scenario(loop) {
                @Override
                public void start() {
                    HttpClient httpClient = HttpClient.newBuilder()
                        .executor(loop)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                    
                    // Initialize shared context
                    this.getContext().put("httpClient", httpClient);
                    this.getContext().put("baseUrl", url);
                }
            };
            scenario.setSequences(sequences);
            return scenario;
        };
        
        // Execute benchmark
        try (Benchmark benchmark = new Benchmark(record)) {
            benchmark
                .start(scenarioFactory)
                .generateReport(new WrkStats(threads, connections, url))
                .plot();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
```

### Project Structure

```
my-benchmark/
├── pom.xml
└── src/main/java/
    └── com/example/benchmark/
        ├── MyBenchmark.java          # Main class
        ├── sequence/                  # Sequence implementations
        │   ├── RegistrationSequence.java
        │   └── BookingSequence.java
        └── step/                      # Step implementations
            ├── LoginStep.java
            └── SearchStep.java
```

### Maven Dependencies

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.yewyc</groupId>
    <artifactId>yewyc-loader-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Running the Benchmark

The benchmark will:
1. Execute the warmup phase
2. Run the load test for the specified duration
3. Generate performance statistics
4. Create visualization plots

---

## Load Strategies

### `BlockingLoadGenerator.java`

**Request Flow:**
```
Request 1 ──► Response 1 ──► Request 2 ──► Response 2 ──► Request 3
     └─────────────┘              └─────────────┘
     (Wait for response)      (Wait for response)
```

**Key Points:**
- Sends next request immediately after receiving response
- Self-regulating: slow responses naturally reduce request rate
- Maintains constant number of in-flight requests
- Suffers from coordinated omission in latency measurements

### `RateLimitedBlockingLoadGenerator.java`

**Request Flow:**
```
Intended: T0    T1    T2    T3    T4
Actual:   R0 ──► R1 ──► R2 ──► R3 ──► R4
          └──┘    └──┘    └──┘    └──┘
         (Wait)  (Wait)  (Wait)  (Wait)
```

**Key Points:**
- Waits for the current request to complete, but inserts a delay if the response was too fast
- Waits for response before sending next request
- Avoids coordinated omission by recording intended vs actual times
- May fall behind schedule if server is slow

### `NonBlockingLoadGenerator.java`

**Request Flow:**
```
Intended: T0    T1    T2    T3    T4
Actual:   R0    R1    R2    R3    R4
          │     │     │     │     │
          └─────┴─────┴─────┴─────┘
        (No waiting for responses)
```

**Key Points:**
- Sends requests at fixed intervals regardless of responses
- Does **NOT** wait for responses
- Can accumulate in-flight requests if server is slow
- Best represents independent user arrivals

## Coordinated Omission Problem - How strategies handle it:

| Strategy | Coordinated Omission |
|----------|---------------------|
| **Closed-Loop** | ❌ Yes |
| **Scheduled Closed** | ✅ No |
| **Open-Loop** | ✅ No |

// Document created by Bob with some personal changes

---

## Event Loop Architecture & Low GC Design

### Steps Run on the Event Loop

All `Step` implementations execute on Netty's `EventLoop`, which is a single-threaded event loop that processes I/O operations and callbacks. This architecture is crucial for understanding how to write efficient, low-garbage steps.

**Key Characteristics:**

1. **Single-threaded execution per EventLoop** - No thread synchronization needed within a `Step`
2. **Non-blocking I/O** - All operations should be asynchronous
3. **Low garbage collection** - Minimize object allocations to reduce GC pressure

### Writing Low-GC Steps

To achieve low garbage collection overhead, follow these guidelines:

#### ✅ DO:
- **Reuse objects** - Store reusable objects in `ScenarioContext` (e.g., `HttpClient`, connection pools)
- **Use primitive arrays** - Prefer `byte[]` over wrapper objects for request/response bodies
- **Leverage async APIs** - Use `CompletionStage` and `thenAcceptAsync` with the event loop executor
- **Pool resources** - Initialize expensive resources once in `Scenario.start()`

#### ❌ DON'T:
- **Create new clients per request** - Reuse `HttpClient` instances
- **Block the event loop** - Never use blocking I/O or `Thread.sleep()`
- **Allocate large objects repeatedly** - Cache and reuse buffers
- **Use excessive string concatenation** - Use `StringBuilder` or pre-formatted strings

### Instructions for scenarios creation

When creating a benchmark scenarios, follow this template structure:

#### 1. **Scenario Context Initialization**
```java
@Override
public void start() {
    // Initialize shared resources ONCE
    HttpClient httpClient = HttpClient.newBuilder()
        .executor(loop)  // Use event loop as executor
        .connectTimeout(Duration.ofSeconds(10))
        .build();
    
    // Store in context for reuse across all steps
    this.getContext().put("httpClient", httpClient);
    this.getContext().put("baseUrl", "http://localhost:8080");
    
    // Initialize any connection pools, caches, etc.
}
```

#### 2. **Step Implementation Pattern**
```java
public class YourStep extends Step {
    @Override
    public CompletionStage<Void> execute(ScenarioContext ctx, EventLoop loop, long id) {
        // 1. Retrieve reusable resources from context
        HttpClient httpClient = ctx.get("httpClient");
        
        // 2. Prepare request (minimize allocations)
        HttpRequest request = buildRequest(ctx);
        
        // 3. Execute async and chain with thenAcceptAsync
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAcceptAsync(response -> {
                    // 4. Process response and update context
                    processResponse(response, ctx);
                }, loop);  // Always pass the event loop!
    }
}
```

#### 3. **Sequence Weight Distribution**
```java
// Weights should sum to 100 (representing percentages)
List<Sequence> sequences = List.of(
    new HighFrequencySequence(),    // 40% - Common operations
    new MediumFrequencySequence(),  // 35% - Regular operations
    new LowFrequencySequence()      // 25% - Rare operations
);
```

#### 4. **Common Pitfalls to Avoid**

**❌ WRONG - Blocking the event loop:**
```java
return CompletableFuture.supplyAsync(() -> {
    Thread.sleep(1000);  // NEVER block!
    return makeHttpCall();
});
```

**✅ CORRECT - Async with event loop:**
```java
return httpClient.sendAsync(request, handler)
        .thenAcceptAsync(response -> {
            // Process async
        }, loop);
```

**❌ WRONG - Creating new clients:**
```java
HttpClient client = HttpClient.newHttpClient();  // Creates GC pressure!
```

**✅ CORRECT - Reusing from context:**
```java
HttpClient client = ctx.get("httpClient");  // Reuse!
```
