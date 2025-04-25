package com.github.yewyc;

import java.util.Random;

public class CumulativeDistributionFunction {

    private static final Random random = new Random();

    public static int cdfChoice(double[] probs) {
        double cumSum = 0;
        double r = random.nextDouble();
        for (int i = 0; i < probs.length; i++) {
            cumSum += probs[i];
            if (r < cumSum) {
                return i;
            }
        }
        throw new IllegalStateException("Probability distribution should sum to more than 1");
    }
}
