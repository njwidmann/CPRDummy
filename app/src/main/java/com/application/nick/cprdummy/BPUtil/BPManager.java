
package com.application.nick.cprdummy.BPUtil;

import android.provider.ContactsContract;
import android.util.Log;

import java.util.ArrayList;

import static java.lang.Math.max;
import static java.lang.Math.pow;

/**
 * Created by Nick on 8/9/2017.
 */

public class BPManager {

    private static final String TAG = "BloodPressureManager";

    public static final float BASELINE_BP = 5;
    public static final int DATA_POINT_LOG_SIZE = 5;
    public static final int DEFAULT_AVG_DEPTH = 20;

    public enum DEPTH_DIRECTION {
        INCREASING, DECREASING, STRAIGHT
    }

    private static final int STRAIGHT_TIMEOUT = 50; //after straight 50x in a row (5 sec), we are going to reset BPM and AvgDepth.

    private float bpm;
    private float avgRelDepth;
    private float avgAbsDepth;
    private int straightCounter;
    private float maxSBP;
    private float maxDBP;
    private float avgLeaningDepth;

    private ArrayList<DataPoint> dataPoints;

    AvgDepthCalculator avgDepthCalculator;
    BPCalculator bpCalculator;
    BPMCalculator bpmCalculator;
    EndTitleCalculator endTitleCalculator;
    LeaningCalculator leaningCalculator;
    DirectionCalculator directionCalculator;
    BaselineBPCalculator baselineBPCalculator;
    //SpeedCalculator speedCalculator;
    //PeakSBPCalculator peakSBPCalculator;

    BPManager self;

    public BPManager() {

        self = this;

        dataPoints = new ArrayList<>();

        bpm = 0;
        avgRelDepth = DEFAULT_AVG_DEPTH;
        avgAbsDepth = DEFAULT_AVG_DEPTH;
        maxSBP = 0;
        maxDBP = 0;
        avgLeaningDepth = 0;

        avgDepthCalculator = new AvgDepthCalculator();

        bpmCalculator = new BPMCalculator();

        endTitleCalculator = new EndTitleCalculator();

        leaningCalculator = new LeaningCalculator();

        baselineBPCalculator = new BaselineBPCalculator();

        directionCalculator = new DirectionCalculator(dataPoints) {

            @Override
            public void handleCompressionStart(int depth, long time) {
                Log.i(TAG, "COMPRESSION START");
                bpmCalculator.registerCompressionStart(time);
                bpm = bpmCalculator.calculateBPM();
                baselineBPCalculator.registerCompressionStart(time, bpm);
                avgDepthCalculator.registerCompressionStart(depth);
                bpCalculator.registerCompressionStart(depth, avgRelDepth);

                calculateMaxSBP(avgRelDepth);
                calculateMaxDBP(avgRelDepth);
                //update sbp and dbp for all the datapoints in the log that came before compression start was recognized.
                //remember none of the datapoints in the log have been displayed yet.
                float sbp = getSBP(time);
                float dbp = getDBP(time);
                for(int i = 0; i < dataPoints.size(); i++) {
                    if(dataPoints.get(i).direction == DEPTH_DIRECTION.INCREASING) {
                        dataPoints.get(i).sbp = sbp;
                        dataPoints.get(i).dbp = dbp;
                    }
                }
            }

            @Override
            public void handleCompressionEnd(int depth, long time) {
                Log.i(TAG, "COMPRESSION END");
                avgRelDepth = avgDepthCalculator.calculateAvgRelativeCompressionDepth();
                avgAbsDepth = avgDepthCalculator.calculateAvgAbsoluteCompressionDepth();

                avgLeaningDepth = leaningCalculator.registerLeaningDepth(depth);

                bpCalculator.registerCompressionEnd(time);

            }

            @Override
            public void handleCompressionPeak(int depth, long time) {
                Log.i(TAG, "PEAK DEPTH");
                avgDepthCalculator.registerCompressionPeak(depth);
                bpCalculator.registerCompressionPeak(depth);
                endTitleCalculator.registerCompression(getSBP(time));
            }
        };

        bpCalculator = new BPCalculator();

    }

    public void addDataPoint(DataPoint dataPoint) {
        dataPoint.sbp = getSBP(dataPoint.time);
        dataPoint.dbp = getDBP(dataPoint.time);
        dataPoint.endTitle = endTitleCalculator.getEndTitle(dataPoint.time);
        dataPoints.add(dataPoint);
        directionCalculator.update(dataPoint);

        if(dataPoint.direction == DEPTH_DIRECTION.STRAIGHT) {
            straightCounter++;
        } else {
            straightCounter = 0;
        }
        if(straightCounter >= STRAIGHT_TIMEOUT) {
            handleStraightTimeout();
        }
    }

    public boolean isDataPointLogFull() {
        return dataPoints.size() >= DATA_POINT_LOG_SIZE;
    }

    public DataPoint getNextDataPoint() {
        return dataPoints.remove(0);
    }

    private void handleStraightTimeout() {
        bpmCalculator.refreshBPM();
        bpm = 0;
        avgDepthCalculator.refreshAvgDepth();
        avgRelDepth = DEFAULT_AVG_DEPTH;
        avgAbsDepth = DEFAULT_AVG_DEPTH;
        avgLeaningDepth = leaningCalculator.setLeaningToLatestValue();
        maxSBP = 0;
        maxDBP = 0;
        directionCalculator.reset();
        baselineBPCalculator.refresh();
    }

    private float calculateMaxDBP(float depth) {
        maxDBP = (float) DBPCalculator.getDBP(depth);
        maxDBP = bpmCalculator.adjustPressureForBPM(maxDBP);
        maxDBP = leaningCalculator.adjustDBPForLeaning(maxDBP);
        return maxDBP;
    }

    private float calculateMaxSBP(float depth) {
        maxSBP = (float)SBPCalculator.getSBP(depth);
        maxSBP = bpmCalculator.adjustPressureForBPM(maxSBP);
        maxSBP = leaningCalculator.adjustSBPForLeaning(maxSBP);
        return maxSBP;
    }

    public float getDBP(long time) {
        return baselineBPCalculator.scaleBP(maxDBP, time);
    }

    public float getSBP(long time) {
        return baselineBPCalculator.scaleBP(maxSBP, time);
    }

    public int getEndTitle() {
        return endTitleCalculator.getHighValue();
    }

    public float getBPM() {
        return bpm;
    }

    public float getAvgLeaningDepth() {
        return avgLeaningDepth;
    }

    public float getPressure(DataPoint dataPoint) {

        return bpCalculator.updateBP(dataPoint.depth, dataPoint.time, dataPoint.direction, dataPoint.sbp, dataPoint.dbp);

    }

    public float getAvgRelativeDepth() {
        return avgRelDepth;
    }
    public float getAvgAbsoluteDepth() {
        return avgAbsDepth;
    }

    private static class DBPCalculator {

        private static final double c1 = -.00045097;
        private static final double c2 = 0.0307429;
        private static final double c3 = .24695878;
        private static final double c4 = BASELINE_BP;

        /**
         * gets diastolic blood pressure
         * depth = depth (mm)
         */
        static double getDBP(float depth) {
            return c1 * Math.pow(depth, 3) + c2 * Math.pow(depth, 2) + c3 * depth + c4;
        }
    }


    private static class SBPCalculator {

        private static final double c1 = -.0297;
        private static final double c2 = 3.2635;
        private static final double c3 = BASELINE_BP;


        /**
         * gets systolic blood pressure
         * depth = depth (mm)
         */
        static double getSBP(float depth) {
            return c1 * Math.pow(depth, 2) + c2 * depth + c3;
        }
    }


}

