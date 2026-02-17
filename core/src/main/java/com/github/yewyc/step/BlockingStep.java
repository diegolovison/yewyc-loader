package com.github.yewyc.step;

public abstract class BlockingStep extends Step {

    protected BlockingStep() {
        this(null);
    }

    protected BlockingStep(String name) {
        super(name);
    }
}
