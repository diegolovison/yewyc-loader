package com.github.yewyc;

import java.util.concurrent.Callable;

public class CallableUtils {

    public static Task callTask(Callable<Task> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
