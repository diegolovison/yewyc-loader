package com.github.yewyc.wrk;

import com.github.yewyc.benchmark.Benchmark;

// -XX:+FlightRecorder -XX:FlightRecorderOptions=stackdepth=128 -XX:StartFlightRecording=maxsize=1g,dumponexit=true,filename=data.jfr,settings=default
public class Wrk2 extends WrkAbstract {

    public static void main(String[] args) {
        Wrk2 wrk2 = new Wrk2();
        wrk2.exec(args);
    }

    @Override
    protected void validate(Benchmark benchmark) {
        if (benchmark.getRecord().rate() <= 0) {
            throw new IllegalStateException("Rate should be greater than 0");
        }
    }
}
