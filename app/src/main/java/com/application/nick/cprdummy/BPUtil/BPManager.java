
package com.application.nick.cprdummy.BPUtil;

import android.util.Log;
import static java.lang.Math.pow;

/**
 * Created by Nick on 8/9/2017.
 */

public class BPManager {

    private static final String TAG = "BPUtil";

    //Because of data limitations
    private static final int MAX_BPM = 130;
    private static final int MIN_BPM = 70;
    private static final int MAX_DEPTH = 65;
    private static final int MIN_DEPTH = 5;

    public enum DEPTH_DIRECTION {
        INCREASING, DECREASING, STRAIGHT
    }

    private static final int STRAIGHT_TIMEOUT = 20; //after straight 20x in a row, we are going to reset BPM and AvgDepth.

    private float bpm;
    private float avgDepth;
    private int straightCounter;
    private float sbp;
    private float dbp;
    private float avgLeaningDepth;

    AvgDepthCalculator avgDepthCalculator;
    BPCalculator bpCalculator;
    BPMCalculator bpmCalculator;
    EndTitleCalculator endTitleCalculator;
    LeaningCalculator leaningCalculator;
    DirectionCalculator directionCalculator;

    BPManager self;

    public BPManager() {

        self = this;

        bpm = 0;
        avgDepth = 0;
        sbp = 0;
        dbp = 0;
        avgLeaningDepth = 0;

        avgDepthCalculator = new AvgDepthCalculator();

        bpmCalculator = new BPMCalculator();

        endTitleCalculator = new EndTitleCalculator();

        leaningCalculator = new LeaningCalculator();

        directionCalculator = new DirectionCalculator() {

            @Override
            public void handleCompressionStart(int depth, long time) {
                Log.i(TAG, "COMPRESSION START");
                bpmCalculator.registerCompressionStart(time);
                avgDepthCalculator.registerCompressionStart(depth);
                calculateSBP(bpm, avgDepth);
                bpCalculator.updateCompressionStartValues(depth, avgDepth, sbp, dbp);
            }

            @Override
            public void handleCompressionEnd(int depth, long time) {
                Log.i(TAG, "COMPRESSION END");
                bpm = bpmCalculator.calculateBPM();
                avgDepth = avgDepthCalculator.calculateAvgCompressionDepth();

                avgLeaningDepth = leaningCalculator.registerLeaningDepth(depth);

                calculateDBP(bpm, avgDepth);
            }

            @Override
            public void handleCompressionPeak(int depth, long time) {
                Log.i(TAG, "PEAK DEPTH");
                avgDepthCalculator.registerCompressionPeak(depth);
                bpCalculator.updateCompressionPeakValues(bpm, bpCalculator.getBP(), time);
            }
        };

        bpCalculator = new BPCalculator();

    }

    private void handleStraightTimeout() {
        bpmCalculator.refreshBPM();
        bpm = 0;
        avgDepthCalculator.refreshAvgDepth();
        avgDepth = 0;
        avgLeaningDepth = leaningCalculator.setLeaningToLatestValue();
        sbp = 0;
        dbp = 0;
    }

    private float calculateDBP(float bpm, float depth) {
        dbp = (float) DBPCalculator.getDBP(bpm, depth);
        return dbp;
    }

    private float calculateSBP(float bpm, float depth) {
        sbp = (float) SBPCalculator.getSBP(bpm, depth);
        return dbp;
    }

    public float getSBP() {
        return sbp;
    }

    public float getDBP() {
        return dbp;
    }

    public DEPTH_DIRECTION getDepthDirection(long time, int depth) {
        DEPTH_DIRECTION direction = directionCalculator.update(time, depth);
        if(direction == DEPTH_DIRECTION.STRAIGHT) {
            straightCounter++;
        } else {
            straightCounter = 0;
        }
        if(straightCounter >= STRAIGHT_TIMEOUT) {
            handleStraightTimeout();
        }
        return direction;
    }

    public float getBPM() {
        return bpm;
    }

    public float getAvgLeaningDepth() {
        return avgLeaningDepth;
    }

    public float getPressure(int depth, long time, DEPTH_DIRECTION direction) {
        float pressure = bpCalculator.updateBP(depth, time, direction);
        //adjust pressure for leaning
        return leaningCalculator.adjustPressureForLeaning(pressure);
    }

    public float getAvgDepth() {
        return avgDepth;
    }

    public float getEndTitle(long time) {
        return endTitleCalculator.getEndTitle(time);
    }


    private static class DBPCalculator {
        static final double p00 =       204.8;
        static final double p10 =      -6.622;
        static final double p01 =      -1.147;
        static final double p20 =     0.07062;
        static final double p11 =     0.03438;
        static final double p02 =    0.002094;
        static final double p30 =  -0.0002408;
        static final double p21 =  -8.431*pow(10,-5);
        static final double p12 =  -0.0001101;
        static final double p03 =   5.439*pow(10,-7);

        /**
         * gets diastolic blood pressure
         * bpm = compression rate (bpm)
         * depth = depth (mm)
         */
        static double getDBP(float bpm, float depth) {
            if(bpm < 10) {
                return 0;
            }

            //due to limitations in our data, our model is only accurate inside a certain range
            if(bpm > MAX_BPM) {
                bpm = MAX_BPM;
            } else if(bpm < MIN_BPM) {
                bpm = MIN_BPM;
            }
            if(depth > MAX_DEPTH) {
                depth = MAX_DEPTH;
            } else if(depth < MIN_DEPTH) {
                depth = MIN_DEPTH;
            }

            return p00 + p10*bpm + p01*depth + p20*pow(bpm,2) + p11*bpm*depth + p02*pow(depth,2) + p30*pow(bpm,3) + p21*pow(bpm,2)*depth
                    + p12*bpm*pow(depth,2) + p03*pow(depth,3);
        }
    }

    private static class SBPCalculator {

        static final double p00 =       408.6;
        static final double p10 =       -12.7;
        static final double p01 =      -4.279;
        static final double p20 =      0.1351;
        static final double p11 =     0.06428;
        static final double p02 =     0.07256;
        static final double p30 =   -0.000459;
        static final double p21 =  -0.0001749;
        static final double p12 =  -0.0001279;
        static final double p03 =  -0.0006677;

        /**
         * gets systolic blood pressure
         * bpm = compression rate (bpm)
         * depth = depth (mm)
         */
        static double getSBP(float bpm, float depth) {
            if(bpm < 10) {
                return 0;
            }

            //due to limitations in our data, our model is only accurate inside a certain range
            if(bpm > MAX_BPM) {
                bpm = MAX_BPM;
            } else if(bpm < MIN_BPM) {
                bpm = MIN_BPM;
            }
            if(depth > MAX_DEPTH) {
                depth = MAX_DEPTH;
            } else if(depth < MIN_DEPTH) {
                depth = MIN_DEPTH;
            }

            return p00 + p10*bpm + p01*depth + p20*pow(bpm,2) + p11*bpm*depth + p02*pow(depth,2) + p30*pow(bpm,3) + p21*pow(bpm,2)*depth
                    + p12*bpm*pow(depth,2) + p03*pow(depth,3);
        }
    }


}

