package com.application.nick.cprdummy.BPUtil;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Nick on 8/11/2017.
 */

public class BPMCalculator {

    private static final int MAX_LOG_SIZE = 2;
    public static final int DEFAULT_BPM = 70; //if there's only one start time in the log we return default

    private ArrayList<Long> compressionStartTimes;
    private float bpm;

    public BPMCalculator() {
        compressionStartTimes = new ArrayList<>();
        bpm = 0;
    }

    public void registerCompressionStart(long time) {
        compressionStartTimes.add(time);
        if(compressionStartTimes.size() > MAX_LOG_SIZE) {
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
            return DEFAULT_BPM; //we need at least 2 values in the log to know the time of at least 1 compression cycle
        }
        bpm = 60000f / calculateAverageCompressionTime();
        return bpm;
    }

    public void refreshBPM() {
        compressionStartTimes.clear();
        bpm = 0;
    }

    public float adjustPressureForBPM(float pressure) {
        return pressure * (1 - getBPMPenaltyFactor(calculateBPM()));
    }

    private static final int[] RATE = {
            0,
            60,
            90,
            100,
            120,
            130,
            160,
            220,
            300

    };
    private static final float[] PENALTY = {
            0.5f,
            0.5f,
            .1f,
            0,
            0,
            0.1f,
            0.2f,
            0.5f,
            0.5f
    };

    private float getBPMPenaltyFactor(float rate) {
        for(int i = 1; i < RATE.length; i++) {
            if(rate < RATE[i]) {
                float y0 = PENALTY[i-1];
                float slope = (PENALTY[i] - PENALTY[i-1]) / (RATE[i] - RATE[i-1]);
                float x = rate - RATE[i-1];
                return slope * x + y0;
            }
        }
        return 1; //if rate > 300 or rate < 0, return 1
    }
}
