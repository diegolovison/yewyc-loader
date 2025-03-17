package com.github.yewyc;

import org.jgroups.Address;

import java.util.List;

public class RemoteTask {

    private final Address source;
    private final List<Task> tasks;

    public RemoteTask(Address source, List<Task> tasks) {
        this.source = source;
        this.tasks = tasks;
    }

    public Address getSource() {
        return source;
    }

    public List<Task> getTasks() {
        return tasks;
    }
}
