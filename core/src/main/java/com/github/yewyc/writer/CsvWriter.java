package com.github.yewyc.writer;

import com.github.yewyc.stats.StatisticPhase;
import com.github.yewyc.stats.StatisticTick;

import java.util.ArrayList;
import java.util.List;

import static com.github.yewyc.stats.Statistic.scale;

public class CsvWriter {
    // \t is easy to copy and paste everywhere
    private static final String separator = "\t";
    private List<String> content;

    public CsvWriter() {
        this.content = new ArrayList<>();
    }

    public void append(String rootPhase, String sequence, String step, List<StatisticTick> ticks) {
        if (this.content.isEmpty()) {
            this.content.add("phase" + separator + "sequence" + separator + "step" + separator + "it" + separator + "count" + separator + "mean(ms)");
        }
        for (int i = 0; i < ticks.size(); i++) {
            StatisticTick tick = ticks.get(i);
            this.content.add(rootPhase + separator + sequence + separator + step + separator + (i + 1) + separator + tick.counter() + separator + (tick.latency().getMean() / scale));
        }
    }

    public void dump() {
        for (String line : this.content) {
            System.out.println(line);
        }
    }
}
