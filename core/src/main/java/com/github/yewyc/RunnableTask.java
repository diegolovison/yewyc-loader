package com.github.yewyc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.github.yewyc.CumulativeDistributionFunction.cdfChoice;

public class RunnableTask {

    private static final Logger log = LoggerFactory.getLogger(RunnableTask.class);

    private final ExecutorService executor;
    private final double[] probabilities;
    private final List<InstanceTask> localWeightTasks;

    public RunnableTask(ExecutorService executor, List<WeightTask> weightTasks) {

        this.executor = executor;

        this.probabilities = weightTasks.stream()
                .mapToDouble(WeightTask::getProbability)
                .toArray();
        double sum = Arrays.stream(probabilities).sum();
        if (sum > 1.0) {
            throw new IllegalStateException("The sum of the probabilities cannot be greater than 1.0");
        }

        this.localWeightTasks = new ArrayList<>(weightTasks.size());
        for (WeightTask task : weightTasks) {
            InstanceTask instanceTask = new InstanceTask(task.initialize(this.executor), task.getProbability());
            this.localWeightTasks.add(instanceTask);
        }
    }

    public CompletableFuture<TaskResult> call() {

        Task task;
        if (localWeightTasks.size() > 1) {
            int prob = cdfChoice(this.probabilities);
            task = localWeightTasks.get(prob).getTask();
        } else {
            task = localWeightTasks.getFirst().getTask();
        }

        CompletableFuture<TaskStatus> taskStatusFuture = task.submit();
        return taskStatusFuture.handle((taskStatus, ex) -> {
            return new TaskResult(task, taskStatus);
        });
    }

    public void close() {
        for (InstanceTask instanceTask : this.localWeightTasks) {
            instanceTask.getTask().close();
        }
    }

    public List<InstanceTask> getLocalWeightTasks() {
        return localWeightTasks;
    }
}
