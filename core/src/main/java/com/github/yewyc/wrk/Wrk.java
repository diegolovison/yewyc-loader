package com.github.yewyc.wrk;

import com.github.yewyc.loadgenerator.LoadStrategy;

public class Wrk extends WrkAbstract {

    public static void main(String[] args) {
        Wrk wrk = new Wrk();
        wrk.exec(args);
    }

    @Override
    protected LoadStrategy getMode() {
        return LoadStrategy.CLOSED;
    }
}
