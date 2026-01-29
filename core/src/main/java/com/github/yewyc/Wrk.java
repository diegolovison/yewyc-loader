package com.github.yewyc;

import com.github.yewyc.benchmark.Benchmark;

public class Wrk extends WrkAbstract {

    public static void main(String[] args) {
        Wrk wrk = new Wrk();
        wrk.exec(args);
    }

    @Override
    protected void validate(Benchmark benchmark) {
        if (benchmark.getRecord().rate() != 0) {
            throw new IllegalStateException("Rate should be equals 0");
        }
    }
}
