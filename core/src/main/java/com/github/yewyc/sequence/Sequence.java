package com.github.yewyc.sequence;

import com.github.yewyc.scenario.ScenarioContext;
import com.github.yewyc.stats.SequentialTimeSeriesRecorder;
import com.github.yewyc.stats.Statistic;
import com.github.yewyc.step.Step;

import java.time.Duration;
import java.util.List;

public class Sequence {

    private final double weight;
    private final List<Step> steps;
    private final Sequence next;

    private SequentialTimeSeriesRecorder localRecorder;

    public Sequence(double weight, List<Step> steps) {
        this(weight, steps, null);
    }

    public Sequence(double weight, List<Step> steps, Sequence next) {
        this.weight = weight;
        this.steps = steps;
        this.next = next;
    }

    /**
     * slow operation
     * @param duration
     */
    public final void prepare(Duration duration) {
        this.localRecorder = new SequentialTimeSeriesRecorder(duration);
    }

    /**
     * fast operation
     * @param start
     */
    public final void start(long start) {
        this.localRecorder.start(start);
    }

    public double getWeight() {
        return weight;
    }

    public List<Step> getSteps() {
        return this.steps;
    }

    public boolean isOk(ScenarioContext ctx) {
        return true;
    }

    public Sequence getNext() {
        return next;
    }

    public void recordValue(long whenFired, long duration, boolean success) {
        this.localRecorder.recordValue(whenFired, duration, success);
    }

    public Statistic collectStatistics() {
        return new Statistic(this.localRecorder.getTimes(), this.localRecorder.getHistograms(), this.localRecorder.getErrors());
    }
}
