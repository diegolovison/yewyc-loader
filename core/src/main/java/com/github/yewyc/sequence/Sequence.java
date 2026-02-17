package com.github.yewyc.sequence;

import com.github.yewyc.scenario.ScenarioContext;
import com.github.yewyc.stats.SequentialTimeSeriesRecorder;
import com.github.yewyc.stats.Statistic;
import com.github.yewyc.stats.StatisticSequence;
import com.github.yewyc.step.Step;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Sequence {

    private final String name;
    private final double weight;
    private final List<Step> steps;
    private final Sequence next;
    private final Mark[] markings;
    private final SequentialTimeSeriesRecorder[] stepsRecorder;
    private SequentialTimeSeriesRecorder localRecorder;

    public Sequence(double weight, List<Step> steps) {
        this(weight, steps, null);
    }

    public Sequence(String name, double weight, List<Step> steps) {
        this(name, weight, steps, null);
    }

    public Sequence(double weight, List<Step> steps, Sequence next) {
        this("default", weight, steps, next);
    }

    public Sequence(String name, double weight, List<Step> steps, Sequence next) {
        this.name = name;
        this.weight = weight;
        this.steps = steps;
        this.next = next;
        this.markings = new Mark[this.steps.size()];
        for (int i = 0; i < this.markings.length; i++) {
            Step step = steps.get(i);
            this.markings[i] = new Mark();
            this.markings[i].name = step.getName();
            this.markings[i].whenFired = -1;
            this.markings[i].endTime = -1;
        }
        this.stepsRecorder = new SequentialTimeSeriesRecorder[this.steps.size()];
    }

    /**
     * slow operation
     * @param duration
     */
    public final void prepare(Duration duration) {
        this.localRecorder = new SequentialTimeSeriesRecorder(duration);
        for (int i = 0; i < this.stepsRecorder.length; i++) {
            this.stepsRecorder[i] = new SequentialTimeSeriesRecorder(duration);
        }
    }

    /**
     * fast operation
     * @param start
     */
    public final void start(long start) {
        this.localRecorder.start(start);
        for (int i = 0; i < this.stepsRecorder.length; i++) {
            this.stepsRecorder[i].start(start);
        }
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

    public void recordValue(long whenFired, long intendedTime, long endTime, boolean success) {
        this.localRecorder.recordValue(whenFired, endTime - intendedTime, success);
        for (int i = 0; i < this.markings.length; i++) {
            // todo success per step
            this.stepsRecorder[i].recordValue(this.markings[i].whenFired, this.markings[i].endTime - intendedTime, success);
            this.markings[i].whenFired = -1;
            this.markings[i].endTime = -1;
        }
    }

    public StatisticSequence collectStatistics() {
        Statistic sequenceStatistic = new Statistic(this.getName(), this.localRecorder.getTimes(), this.localRecorder.getHistograms(), this.localRecorder.getErrors());
        List<Statistic> stepStatistics = new ArrayList<>();
        for (int i = 0; i < this.stepsRecorder.length; i++) {
            SequentialTimeSeriesRecorder stepRecorder = this.stepsRecorder[i];
            Statistic stepStat = new Statistic(this.markings[i].name, stepRecorder.getTimes(), stepRecorder.getHistograms(), stepRecorder.getErrors());
            stepStatistics.add(stepStat);
        }
        return new StatisticSequence(sequenceStatistic, stepStatistics);
    }

    public String getName() {
        return this.name;
    }

    public void markBegin(int index) {
        this.markings[index].whenFired = System.nanoTime();
    }

    public void markEnd(int index) {
        this.markings[index].endTime = System.nanoTime();
    }

    private static class Mark {
        String name;
        long whenFired;
        long endTime;
    }
}
