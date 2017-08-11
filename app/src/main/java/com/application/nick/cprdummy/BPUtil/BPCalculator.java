package com.application.nick.cprdummy.BPUtil;

import android.util.Log;

/**
 * Created by Nick on 8/11/2017.
 */

public class BPCalculator {

    private static final String TAG = "BPCalculator";

    //constants used for BP calculation
    private static final float const1 = 4000000;

    private float pressure;
    private float sbp, dbp;
    private float avgDepth;
    private int compressionStartDepth;
    private float peakPressure;
    private float bpm;
    private long peakTime;

    public BPCalculator() {
        pressure = 0;
    }

    /**
     * updates blood pressure based on depth, time, and depth moving direction. Takes into account
     * SBP, DBP, BPM, Avg Depth, compression start depth, and previous compression peak time/pressure.
     * This function is used to simulate the BP curve between SBP and DBP
     * @param depth current depth
     * @param time current time (ms)
     * @param direction current moving depth direction (INCREASING, DECREASING, or STRAIGHT)
     * @return updated BP
     */
    public float updateBP(int depth, long time, BPManager.DEPTH_DIRECTION direction) {

        if(direction == BPManager.DEPTH_DIRECTION.INCREASING) {
            int depthDifference = depth - compressionStartDepth;
            //Log.i(TAG, String.valueOf(compressionStartPressure));
            pressure = (float)((sbp + dbp) * Math.log10((depthDifference + avgDepth) / avgDepth)  + dbp);
        } else {
            float elapsedTime = time - peakTime;
            pressure = 1.0f / (bpm * elapsedTime / const1 + 1 / peakPressure);

        }

        return pressure;
    }

    public void updateCompressionStartValues(int compressionStartDepth, float avgDepth, float sbp, float dbp) {
        this.compressionStartDepth = compressionStartDepth;
        this.avgDepth = avgDepth;
        this.sbp = sbp;
        this.dbp = dbp;
    }

    public void updateCompressionPeakValues(float bpm, float peakPressure, long peakTime) {
        this.bpm = bpm;
        this.peakPressure = peakPressure;
        this.peakTime = peakTime;
    }
    /**
     * returns the last updated blood pressure value
     * @return BP
     */
    public float getBP() {
        return pressure;
    }

}
