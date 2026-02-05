### wrk - `ClosedLoadGenerator.java`

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

### wrk2 - `SemiOpenLoadGenerator.java`

**Request Flow:**
```
Intended: T0    T1    T2    T3    T4
Actual:   R0 ──► R1 ──► R2 ──► R3 ──► R4
          └──┘    └──┘    └──┘    └──┘
         (Wait)  (Wait)  (Wait)  (Wait)
```

**Key Points:**
- Tracks intended send times
- Waits for response before sending next request
- Avoids coordinated omission by recording intended vs actual times
- May fall behind schedule if server is slow

### wrk3 - `OpenLoadGenerator.java`

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

| Strategy | Coordinated Omission | Solution |
|----------|---------------------|----------|
| **Closed-Loop** | ❌ Yes | None - accepts the limitation |
| **Scheduled Closed** | ✅ No | Records intended send time |
| **Open-Loop** | ✅ No | Sends regardless of responses |

// Document created by Bob with some personal changes
