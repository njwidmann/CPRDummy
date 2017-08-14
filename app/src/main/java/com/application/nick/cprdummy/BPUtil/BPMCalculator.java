package com.application.nick.cprdummy.BPUtil;

import java.util.ArrayList;

/**
 * Created by Nick on 8/11/2017.
 */

public class BPMCalculator {

    private static final int LOGSIZE = 3;

    private ArrayList<Long> compressionStartTimes;
    private float bpm;

    public BPMCalculator() {
        compressionStartTimes = new ArrayList<>();
        bpm = 0;
    }

    public void registerCompressionStart(long time) {
        compressionStartTimes.add(time);
        if(compressionStartTimes.size() > LOGSIZE) {
            compressionStartTimes.remove(0);
        }
    }

    private float calculateAverageCompressionTime() {
        long totalTime = 0L;
        for(int i = 0; i < compressionStartTimes.size() - 1; i++) {
            long time1 = compressionStartTimes.get(i);
            long time2 = compressionStartTimes.get(i + 1);
            totalTime += (time2 - time1);
        }
        return (float) totalTime / (compressionStartTimes.size() - 1);
    }

    public float calculateBPM() {
        if(compressionStartTimes.size() < 2) {
            return 0; //we need at least 2 values in the log to know the time of at least 1 compression cycle
        }
        bpm = 60000f / calculateAverageCompressionTime();
        return bpm;
    }

    public void refreshBPM() {
        compressionStartTimes.clear();
        bpm = 0;
    }
}
