package com.github.yewyc.step;

import com.github.yewyc.scenario.ScenarioContext;
import io.netty.channel.EventLoop;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class JDBCLookupStep extends BlockingStep {

    public JDBCLookupStep(double weight) {
        super(weight);
    }

    @Override
    public CompletionStage<Void> execute(ScenarioContext ctx, EventLoop loop, long id) {
        // Simulate DB Latency
        try {
            Thread.sleep(50);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ctx.put("dbResult", "UserFound");
        return CompletableFuture.completedFuture(null);
    }
}
