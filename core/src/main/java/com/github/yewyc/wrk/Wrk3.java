package com.github.yewyc.wrk;

import com.github.yewyc.loadgenerator.LoadStrategy;

public class Wrk3 extends WrkAbstract {

    public static void main(String[] args) {
        Wrk3 wrk2 = new Wrk3();
        wrk2.exec(args);
    }

    @Override
    protected LoadStrategy getMode() {
        return LoadStrategy.OPEN;
    }
}
