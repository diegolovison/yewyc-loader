package com.github.yewyc;

import org.jgroups.Address;

import java.util.List;

public class RemoteTask {

    private final Address source;
    private final List<WeightTask> weightTasks;

    public RemoteTask(Address source, List<WeightTask> weightTasks) {
        this.source = source;
        this.weightTasks = weightTasks;
    }

    public Address getSource() {
        return source;
    }

    public List<WeightTask> getWeightTasks() {
        return weightTasks;
    }
}
