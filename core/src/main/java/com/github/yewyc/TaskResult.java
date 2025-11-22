package com.github.yewyc;

public class TaskResult {

    private final Task task;
    private final TaskStatus status;

    public TaskResult(Task task, TaskStatus status) {
        this.task = task;
        this.status = status;
    }

    public Task getTask() {
        return task;
    }

    public TaskStatus getStatus() {
        return status;
    }
}
