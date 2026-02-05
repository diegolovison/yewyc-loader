package com.github.yewyc.wrk;

import com.github.yewyc.benchmark.Benchmark;
import com.github.yewyc.channel.LoadStrategy;

// -XX:+FlightRecorder -XX:FlightRecorderOptions=stackdepth=128 -XX:StartFlightRecording=maxsize=1g,dumponexit=true,filename=data.jfr,settings=default
public class Wrk3 extends WrkAbstract {

    public static void main(String[] args) {
        Wrk3 wrk2 = new Wrk3();
        wrk2.exec(args);
    }

    @Override
    protected LoadStrategy getMode() {
        return LoadStrategy.wrk3;
    }

    @Override
    protected void validate(Benchmark benchmark) {
        if (benchmark.getRecord().rate() <= 0) {
            throw new IllegalStateException("Rate should be greater than 0");
        }
    }
}
