package com.application.nick.cprdummy.BPUtil;

import android.util.Log;

/**
 * Created by Nick on 8/11/2017.
 */

public class BPCalculator {

    private static final String TAG = "BloodPressureCalculator";

    //constants used for BP calculation
    private static final float BASELINE_BP = BPManager.BASELINE_BP;
    private static final float DECAY_TIME = 2000;

    private float pressure;
    private float avgDepth;
    private int compressionStartDepth;
    private float peakPressure;
    private long compressionEndTime;
    private int peakDepth;
    private boolean compressionStarted;
    private boolean compressionEnded;

    public BPCalculator() {
        pressure = BASELINE_BP;
        compressionEnded = true;
        compressionStarted = false;
    }

    /**
     * updates blood pressure based on depth, time, and depth moving direction. Takes into account
     * SBP, DBP, BPM, Avg Relative Depth, compression start depth, and previous compression peak time/pressure.
     * This function is used to simulate the BP curve between SBP and DBP
     * @param depth current depth
     * @param time current time (ms)
     * @param direction current moving depth direction (INCREASING, DECREASING, or STRAIGHT)
     * @return updated BP
     */
    public float updateBP(int depth, long time, DirectionCalculator.DIRECTION direction, float sbp, float dbp) {

        if(direction == DirectionCalculator.DIRECTION.INCREASING) {
            //pressure = sbp;
            int depthDifference = depth - compressionStartDepth;
            if(avgDepth == 0) avgDepth = 50; //avoid divide by zero errors
            float x = depthDifference / avgDepth;
            pressure = BPFunctions.increaseFunction(x) * (sbp - dbp) + dbp;

        } else if (direction == DirectionCalculator.DIRECTION.DECREASING){

            float x = 1 - (float)depth / peakDepth;
            float pressureDifferential = peakPressure - dbp;
            pressure = pressureDifferential * BPFunctions.decreaseFunction(x) + dbp;

        } else if(direction == DirectionCalculator.DIRECTION.STRAIGHT && compressionEnded) {

            float elapsedTime = time - compressionEndTime;
            float pressureDifferential = dbp - BASELINE_BP;
            pressure = pressureDifferential * BPFunctions.straightDecayFunction(elapsedTime / DECAY_TIME) + BASELINE_BP;
        }

        if(pressure < BASELINE_BP) {
            pressure = BASELINE_BP;
        }

        //Log.i(TAG, "Time = " + time + "; Depth = " + depth + "; AvgDepth = " + avgDepth + "; Pressure = " + pressure + "; SBP = " + sbp + "; DBP = " + dbp);

        return pressure;
    }

    public void registerCompressionStart(int compressionStartDepth, float avgDepth) {
        this.compressionStartDepth = compressionStartDepth;
        this.avgDepth = avgDepth;

        compressionStarted = true;
        compressionEnded = false;
    }

    public void registerCompressionPeak(int peakDepth) {
        this.peakPressure = pressure;
        this.peakDepth = peakDepth;
    }

    public void registerCompressionEnd(long compressionEndTime) {
        this.compressionEndTime = compressionEndTime;

        compressionEnded = true;
        compressionStarted = false;
    }

}
