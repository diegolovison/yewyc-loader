package com.github.yewyc.wrk;

import com.github.yewyc.benchmark.Benchmark;
import com.github.yewyc.loadgenerator.LoadStrategy;

public class Wrk extends WrkAbstract {

    public static void main(String[] args) {
        Wrk wrk = new Wrk();
        wrk.exec(args);
    }

    @Override
    protected LoadStrategy getMode() {
        return LoadStrategy.wrk;
    }

    @Override
    protected void validate(Benchmark benchmark) {
        if (benchmark.getRecord().rate() != 0) {
            throw new IllegalStateException("Rate should be equals 0");
        }
    }
}
