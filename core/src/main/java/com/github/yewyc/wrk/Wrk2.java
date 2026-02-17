package com.github.yewyc.wrk;

import com.github.yewyc.loadgenerator.LoadStrategy;

// -XX:+FlightRecorder -XX:FlightRecorderOptions=stackdepth=128 -XX:StartFlightRecording=maxsize=1g,dumponexit=true,filename=data.jfr,settings=default
public class Wrk2 extends WrkAbstract {

    public static void main(String[] args) {
        Wrk2 wrk2 = new Wrk2();
        wrk2.exec(args);
    }

    @Override
    protected LoadStrategy getMode() {
        return LoadStrategy.RATE_LIMIT;
    }
}
