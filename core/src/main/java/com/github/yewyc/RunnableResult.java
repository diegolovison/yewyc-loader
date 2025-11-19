package com.github.yewyc;

import java.util.List;

public class RunnableResult {

    private long start;
    private long end;
    private List<InstanceTask> instanceTasks;

    public RunnableResult(long start, long end, List<InstanceTask> localWeightTasks) {
        this.start = start;
        this.end = end;
        this.instanceTasks = localWeightTasks;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public List<InstanceTask> getInstanceTasks() {
        return instanceTasks;
    }
}
