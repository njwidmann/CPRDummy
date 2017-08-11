package com.application.nick.cprdummy.BPUtil;

/**
 * Created by Nick on 8/11/2017.
 */

public class EndTitleCalculator {
    private static final int HIGH_VALUE = 30;
    private static final int LOW_VALUE = 0;
    private static final long HIGH_TIME = 2000L;
    private static final long LOW_TIME = 1000L;

    private long cycleStartTime;

    private boolean low;
    private boolean high;

    public EndTitleCalculator() {
        cycleStartTime = 0;
        low = true;
        high = false;
    }

    public float getEndTitle(long currentTime) {
        //TODO adjust highs based on BPM and AvgDepth

        //handle breathing cycle changes
        if (low && currentTime - cycleStartTime > LOW_TIME) {
            low = false;
            high = true;
            cycleStartTime = currentTime;
        } else if (high && currentTime - cycleStartTime > HIGH_TIME) {
            high = false;
            low = true;
            cycleStartTime = currentTime;
        }
        if (low) {
            return LOW_VALUE;
        } else {
            return HIGH_VALUE;
        }
    }
}
