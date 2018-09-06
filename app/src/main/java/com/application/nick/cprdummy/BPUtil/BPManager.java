
package com.application.nick.cprdummy.BPUtil;

import android.util.Log;

import java.util.ArrayList;

import static java.lang.Math.max;

/**
 * Created by Nick on 8/9/2017.
 */

public class BPManager {

    private static final String TAG = "BloodPressureManager";

    public static final float BASELINE_BP = 5;
    public static final int DATA_POINT_LOG_SIZE = 5;
    public static final int DEFAULT_AVG_DEPTH = 20;

    public enum DIRECTION {
        INCREASING, DECREASING, STRAIGHT
    }

    private static final long STRAIGHT_TIMEOUT = 5000; //after straight 5sec in a row (5 sec), we are going to reset BPM and AvgDepth.

    private float bpm;
    private float avgRelDepth;
    private float avgAbsDepth;
    private long straightStartTime;
    private float sbp;
    private float dbp;
    private float avgLeaningDepth;

    private ArrayList<DataPoint> dataPoints;

    AvgDepthCalculator avgDepthCalculator;
    BPCalculator bpCalculator;
    BPMCalculator bpmCalculator;
    EndTitleCalculator endTitleCalculator;
    LeaningCalculator leaningCalculator;
    DirectionCalculator directionCalculator;
    PauseCalculator pauseCalculator;

    BPManager self;

    public BPManager() {

        self = this;

        dataPoints = new ArrayList<>();

        bpm = 0;
        avgRelDepth = DEFAULT_AVG_DEPTH;
        avgAbsDepth = DEFAULT_AVG_DEPTH;
        sbp = 0;
        dbp = 0;
        avgLeaningDepth = 0;

        avgDepthCalculator = new AvgDepthCalculator();

        bpmCalculator = new BPMCalculator();

        endTitleCalculator = new EndTitleCalculator();

        leaningCalculator = new LeaningCalculator();

        pauseCalculator = new PauseCalculator();

        directionCalculator = new DirectionCalculator(dataPoints, DataPoint.VALUE_TYPE.DEPTH) {

            @Override
            public void handleCycleStart(int depth, long time) {
                Log.i(TAG, "COMPRESSION START");
                bpmCalculator.registerCompressionStart(time);
                bpm = bpmCalculator.calculateBPM();
                pauseCalculator.registerCompressionStart(time, bpm);
                avgDepthCalculator.registerCompressionStart(depth);
                bpCalculator.registerCompressionStart(depth, avgRelDepth);

                float sbp = calculateSBP(avgRelDepth, time);
                float dbp = calculateDBP(avgRelDepth, time);

                //update sbp and dbp for all the datapoints in the log that came before compression start was recognized.
                //remember none of the datapoints in the log have been displayed yet.
                for(int i = 0; i < dataPoints.size(); i++) {
                    if(dataPoints.get(i).depthDirection == DIRECTION.INCREASING) {
                        dataPoints.get(i).sbp = sbp;
                        dataPoints.get(i).dbp = dbp;
                    }
                }
            }

            @Override
            public void handleCycleEnd(int depth, long time) {
                Log.i(TAG, "COMPRESSION END");
                avgRelDepth = avgDepthCalculator.calculateAvgRelativeCompressionDepth();
                avgAbsDepth = avgDepthCalculator.calculateAvgAbsoluteCompressionDepth();

                avgLeaningDepth = leaningCalculator.registerLeaningDepth(depth);

                bpCalculator.registerCompressionEnd(time);

            }

            @Override
            public void handleCyclePeak(int depth, long time) {
                Log.i(TAG, "PEAK DEPTH");
                avgDepthCalculator.registerCompressionPeak(depth);
                bpCalculator.registerCompressionPeak(depth);
                endTitleCalculator.registerCompression(getSBP());
            }
        };

        bpCalculator = new BPCalculator();

    }

    public void addDataPoint(DataPoint dataPoint) {
        dataPoint.sbp = getSBP();
        dataPoint.dbp = getDBP();
        dataPoint.endTitle = endTitleCalculator.getEndTitle(dataPoint.time);
        dataPoints.add(dataPoint);
        directionCalculator.update(dataPoint);

        if(dataPoint.depthDirection != DIRECTION.STRAIGHT) {
            straightStartTime = dataPoint.time;
        }
        if(dataPoint.time - straightStartTime >= STRAIGHT_TIMEOUT) {
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
        sbp = 0;
        dbp = 0;
        directionCalculator.reset();
        pauseCalculator.refresh();
    }

    /**
     * call at the start of every compression
     * @param depth current depth
     * @param time current time
     * @return dbp
     */
    private float calculateDBP(float depth, long time) {
        dbp = (float) DBPCalculator.getDBP(depth);
        dbp = bpmCalculator.adjustPressureForBPM(dbp);
        dbp = leaningCalculator.adjustDBPForLeaning(dbp);
        dbp = pauseCalculator.scaleBP(dbp, time);
        return dbp;
    }

    /**
     * call at the start of every compression
     * @param depth current depth
     * @param time current time
     * @return sbp
     */
    private float calculateSBP(float depth, long time) {
        sbp = (float)SBPCalculator.getSBP(depth);
        sbp = bpmCalculator.adjustPressureForBPM(sbp);
        sbp = leaningCalculator.adjustSBPForLeaning(sbp);
        sbp = pauseCalculator.scaleBP(sbp, time);
        return sbp;
    }

    public float getDBP() {
        return dbp;
    }

    public float getSBP() {
        return sbp;
    }

    public int getEndTitle() {
        return endTitleCalculator.getHighValue();
    }

    public float getBPM() {
        return pauseCalculator.getAvgBPM();
    }

    public float getAvgLeaningDepth() {
        return avgLeaningDepth;
    }

    public float getPressure(DataPoint dataPoint) {

        return bpCalculator.updateBP(dataPoint.depth, dataPoint.time, dataPoint.depthDirection, dataPoint.sbp, dataPoint.dbp);

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

