package com.application.nick.cprdummy.BPUtil;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Nick on 8/17/2017.
 * This class handles
 */

public class PauseCalculator {

    private static final long LOG_TIME = 5;
    private static final float BASELINE_BP = BPManager.BASELINE_BP;

    private ArrayList<Long> compressionStartTimes;
    private ArrayList<Float> bpmLog;

    public PauseCalculator() {
        compressionStartTimes = new ArrayList<>();
        bpmLog = new ArrayList<>();
    }

    /**
     * we calculate a pause penalty based on average rate over last 5 seconds and max instantaneous
     * rate over the last 5 seconds so we need to register all compression start times and instantaneous
     * rates
     * @param time compression start time
     * @param bpm instantaneous rate when compression starts
     */
    public void registerCompressionStart(long time, float bpm) {
        bpmLog.add(bpm);
        Log.i("BaselineBPCalculator", "bpmLog = " + bpmLog);
        compressionStartTimes.add(time);
    }

    /**
     * we calculate a pause penalty as a function of (average rate over last 5 seconds) / (max instantaneous
     * rate over the last 5 seconds)
     * @return BP scale factor between 0 and 1. 1 = no pause penalty
     */
    private float calculateBPScaleFactor() {
        float x = getAvgBPM() / getMaxBPM();
        return -1f / (125 * (float)Math.pow(x, 4) + 3f) + 1;
    }

    public float getAvgBPM() {
        //num ccs in log_time / log_time * 60 sec/min = rate in ccs/min
        return compressionStartTimes.size() / (float)LOG_TIME * 60;
    }

    private float getMaxBPM() {
        if(bpmLog.size() == 0) return 1;
        float maxBPM = 0;
        for(int i = bpmLog.size() - 1; i >= 0; i--) {
            if(bpmLog.get(i) > maxBPM) {
                maxBPM = bpmLog.get(i);
            }
        }
        return maxBPM;
    }

    public float scaleBP(float bp, long time) {

        while (compressionStartTimes.size() > 0 && //log size > 0
                time - compressionStartTimes.get(0) > LOG_TIME * 1000) { // and time of the earliest logged is > 5sec ago.... remove from log
            compressionStartTimes.remove(0);
            bpmLog.remove(0);
        }

        float bpDifferential = bp - BASELINE_BP;
        if(bpDifferential < 0) bpDifferential = 0;

        return calculateBPScaleFactor() * bpDifferential + BASELINE_BP;
    }

    public void refresh() {
        compressionStartTimes.clear();
        bpmLog.clear();
    }

}
