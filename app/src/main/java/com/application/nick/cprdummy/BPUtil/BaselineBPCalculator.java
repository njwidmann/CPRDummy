package com.application.nick.cprdummy.BPUtil;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Nick on 8/17/2017.
 * This class handles
 */

public class BaselineBPCalculator {

    private static final long LOG_TIME = 5;
    private static final float BASELINE_BP = BPManager.BASELINE_BP;

    private ArrayList<Long> compressionStartTimes;
    private ArrayList<Float> bpmLog;

    public BaselineBPCalculator() {
        compressionStartTimes = new ArrayList<>();
        bpmLog = new ArrayList<>();
    }

    public void registerCompressionStart(long time, float bpm) {
        bpmLog.add(bpm);
        compressionStartTimes.add(time);
    }

    private float calculateBPScaleFactor() {
        float logTimeBPM = compressionStartTimes.size() / (float)LOG_TIME * 60;
        float x = logTimeBPM / getAvgBPM();
        //Log.i("BaselineBPCalculator", "x =" + x);
        return -1f / (125 * (float)Math.pow(x, 4) + 1.25f) + 1;
    }

    private float getAvgBPM() {
        if(bpmLog.size() == 0) return 1;
        float bpmSum = 0;
        for(int i = bpmLog.size() - 1; i >= 0; i--) {
            bpmSum += bpmLog.get(i);
        }
        return bpmSum / bpmLog.size();
    }

    public float scaleBP(float bp, long time) {

        while (compressionStartTimes.size() > 0 && time - compressionStartTimes.get(0) > LOG_TIME * 1000) {
            compressionStartTimes.remove(0);
            bpmLog.remove(0);
        }
        //Log.i("BaselineBPCalculator", "size = " + compressionStartTimes.size() + "; scale factor = " + calculateBPScaleFactor());
        float bpDifferential = bp - BASELINE_BP;
        if(bpDifferential < 0) bpDifferential = 0;

        return calculateBPScaleFactor() * bpDifferential + BASELINE_BP;
    }

    public void refresh() {
        compressionStartTimes.clear();
        bpmLog.clear();
    }

}
