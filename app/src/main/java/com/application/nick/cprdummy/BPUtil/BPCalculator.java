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
    boolean compressionStarted;
    boolean compressionEnded;

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
    public float updateBP(int depth, long time, BPManager.DIRECTION direction, float sbp, float dbp) {

        if(direction == BPManager.DIRECTION.INCREASING) {
            //pressure = sbp;
            int depthDifference = depth - compressionStartDepth;
            if(avgDepth == 0) avgDepth = 50; //avoid divide by zero errors
            float x = depthDifference / avgDepth;
            pressure = increaseFunction(x) * (sbp - dbp) + dbp;

        } else if (direction == BPManager.DIRECTION.DECREASING){

            float x = 1 - (float)depth / peakDepth;
            float pressureDifferential = peakPressure - dbp;
            pressure = pressureDifferential * decreaseFunction(x) + dbp;

        } else if(direction == BPManager.DIRECTION.STRAIGHT && compressionEnded) {

            float elapsedTime = time - compressionEndTime;
            float pressureDifferential = dbp - BASELINE_BP;
            pressure = pressureDifferential * straightDecayFunction(elapsedTime / DECAY_TIME) + BASELINE_BP;
        }

        if(pressure < BASELINE_BP) {
            pressure = BASELINE_BP;
        }

        Log.i(TAG, "Time = " + time + "; Depth = " + depth + "; AvgDepth = " + avgDepth + "; Pressure = " + pressure + "; SBP = " + sbp + "; DBP = " + dbp);

        return pressure;
    }

    /**
     * arbitrary function to simulate the behaviour of the BP waveform during compression
     * @param x relative depth difference / average relative depth
     * @return
     */
    private float increaseFunction(float x) {
        return (float)(-1.1/(10*Math.pow(x,2) + 1) + 1.1);
    }

    /**
     * arbitrary function to simulate the behaviour of the BP waveform during expansion
     * @param x 1 - depth / peakDepth
     * @return pressure fraction of (peak pressure - dbp)
     */
    private float decreaseFunction(float x) {
        double x2 = 4.5*x + .135;
        double term1 = 1.0/(150.0 * Math.pow(x2, 4) + 1);
        double term2 = Math.sin(x2)/Math.sqrt(x2) + .463;
        return (float)((term1 + term2) / 1.782);
    }

    /**
     * arbitrary function to simulate the behaviour of the BP waveform during pause after expansion
     * @param x elapsed time/DECAY_TIME
     * @return pressure fraction of (dbp - baseline pressure)
     */
    private float straightDecayFunction(float x) {
        double term1 = 1.0/(125 * Math.pow(x, 4) + 1);
        double term2 = Math.sin(17.0 * Math.PI * (x + 0.29)) / 10;
        float frac = (float)((term1 + term2 + 0.0921) / 1.192);
        if(x < 1) {
            return frac;
        } else {
            return frac / x;
        }
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
