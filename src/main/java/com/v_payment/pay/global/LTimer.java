package com.v_payment.pay.global;

public class LTimer {
    public static long getCurrTime() {
        return System.currentTimeMillis();
    }

    public static long getDiff(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}
