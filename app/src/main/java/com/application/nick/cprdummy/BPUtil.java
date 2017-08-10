package com.application.nick.cprdummy;

import android.util.Log;

import java.util.ArrayList;

import static java.lang.Math.pow;

/**
 * Created by Nick on 8/9/2017.
 */

public class BPUtil {

    private static final String TAG = "BPUtil";

    public enum DEPTH_DIRECTION {
        INCREASING, DECREASING, STRAIGHT
    }

    private static float bpm = 0;
    private static float avgDepth = 0;
    private static DEPTH_DIRECTION previousDirection = DEPTH_DIRECTION.STRAIGHT;
    private static float pressure = 0;
    private static float sbp = 0;
    private static float dbp = 0;
    private static float lastPeakPressure = 0;
    private static long lastPeakTime = 0;
    private static float compressionStartPressure = 0;
    private static float avgLeaningDepth = 0;

    private static float calculateDBP(float bpm, float depth) {
        dbp = (float) DBPCalculator.getDBP(bpm, depth);
        return dbp;
    }

    private static float calculateSBP(float bpm, float depth) {
        sbp = (float) SBPCalculator.getSBP(bpm, depth);
        return dbp;
    }

    public static float getSBP() {
        return sbp;
    }

    public static float getDBP() {
        return dbp;
    }

    public static DEPTH_DIRECTION getDepthDirection(long time, int depth) {
        return PeakCalculator.update(time, depth);
    }

    public static float getBPM() {
        return bpm;
    }

    public static float getAvgLeaningDepth() {
        return avgLeaningDepth;
    }

    public static float getPressure(int depth, long time, DEPTH_DIRECTION direction) {
        float pressure = BPCalculator.updateBP(depth, time, direction);
        //adjust pressure for leaning
        return LeaningCalculator.adjustPressureForLeaning(pressure);
    }

    public static float getAvgDepth() {
        return avgDepth;
    }

    public static boolean compressionStarted = false;

    private static void handleCompressionStart(int depth, long time) {
        Log.i(TAG, "COMPRESSION START");
        BPMCalculator.registerCompressionStart(time);
        AvgDepthCalculator.registerCompressionStart(depth);
        compressionStarted = true;
        Log.i(TAG, String.valueOf(pressure));
        compressionStartPressure = pressure;

        calculateSBP(bpm, avgDepth);
    }

    private static void handleCompressionEnd(int depth, long time) {
        Log.i(TAG, "COMPRESSION END");
        compressionStarted = false;
        bpm = BPMCalculator.calculateBPM();
        avgDepth = AvgDepthCalculator.calculateAvgCompressionDepth();

        avgLeaningDepth = LeaningCalculator.registerLeaningDepth(depth);

        calculateDBP(bpm, avgDepth);
    }

    private static void handlePeakCompression(int depth, long time) {
        Log.i(TAG, "PEAK DEPTH");
        AvgDepthCalculator.registerCompressionEnd(depth);
        lastPeakTime = time;
        lastPeakPressure = pressure;

    }

    private static void handleDirectionChanges(int depth, long time, DEPTH_DIRECTION direction) {
        if( !compressionStarted && direction == DEPTH_DIRECTION.INCREASING && (previousDirection == DEPTH_DIRECTION.DECREASING || previousDirection == DEPTH_DIRECTION.STRAIGHT )) {
            handleCompressionStart(depth, time);
        } else if ( compressionStarted && (direction == DEPTH_DIRECTION.STRAIGHT || direction == DEPTH_DIRECTION.INCREASING)  && (previousDirection == DEPTH_DIRECTION.DECREASING)) {
            handleCompressionEnd(depth, time);
            if(direction == DEPTH_DIRECTION.INCREASING) handleCompressionStart(depth, time); //might now be increasing again already after previous compression ends
        } else if (compressionStarted && previousDirection == DEPTH_DIRECTION.INCREASING && (direction == DEPTH_DIRECTION.STRAIGHT || direction == DEPTH_DIRECTION.DECREASING)) {
            handlePeakCompression(depth, time);
        }
        previousDirection = direction;
    }

    private static class AvgDepthCalculator {
        private static final int MAX_DEPTH = 65;
        private static final int MIN_DEPTH = 5;


        private static final int LOGSIZE = 3;

        private static ArrayList<Integer> compressionStartDepths = new ArrayList<>();
        private static ArrayList<Integer> compressionEndDepths = new ArrayList<>();

        static {
            for(int i = 0; i < LOGSIZE; i++) {
                compressionStartDepths.add(0);
                compressionEndDepths.add(0);
            }
        }

        public static int getLastStartDepth() {
            return compressionStartDepths.get(LOGSIZE - 1);
        }

        public static void registerCompressionStart(int depth) {
            compressionStartDepths.add(depth);
            if(compressionStartDepths.size() > LOGSIZE) {
                compressionStartDepths.remove(0);
            }
        }

        public static void registerCompressionEnd(int depth) {
            compressionEndDepths.add(depth);
            if(compressionEndDepths.size() > LOGSIZE) {
                compressionEndDepths.remove(0);
            }
        }

        public static float calculateAvgCompressionDepth() {
            int totalDepth = 0;
            for(int i = 0; i < LOGSIZE; i++) {
                long depth = compressionEndDepths.get(i) - compressionStartDepths.get(i);
                totalDepth += depth;
            }
            avgDepth = (float) totalDepth / LOGSIZE;
            if(avgDepth > MAX_DEPTH) {
                avgDepth = MAX_DEPTH;
            } else if(avgDepth < MIN_DEPTH) {
                avgDepth = MIN_DEPTH;
            }

            return avgDepth;
        }
    }

    private static class LeaningCalculator {
        private static final int LOGSIZE = 3;

        private static ArrayList<Integer> leaningDepths = new ArrayList<>();
        private static float avgLeaningDepth = 0;

        static {
            for(int i = 0; i < LOGSIZE; i++) {
                leaningDepths.add(0);
            }
        }

        /**
         * call this after every compression
         * @param depth leaning depth at the end of the compression
         * @return average leaning depth based on log
         */
        public static float registerLeaningDepth(int depth) {
            leaningDepths.add(depth);
            if(leaningDepths.size() > LOGSIZE) {
                leaningDepths.remove(0);
            }
            return calculateAverageLeaningDepth();
        }

        /**
         * calculates avg leaning depth based on Log
         * @return average leaning
         */
        private static float calculateAverageLeaningDepth() {
            float totalDepth = 0;
            for(int i = 0; i < LOGSIZE - 1; i++) {
                totalDepth += leaningDepths.get(i);
            }
            avgLeaningDepth = totalDepth / (LOGSIZE - 1);
            return avgLeaningDepth;
        }

        public static float adjustPressureForLeaning(float pressure) {
            if(avgLeaningDepth > 8) {
                pressure *= 0.8f;
            } else if (avgLeaningDepth > 4) {
                pressure *= 0.9f;
            }
            return pressure;
        }
    }

    private static class BPCalculator {

        private static final float const1 = 4000000;

        private static float updateBP(int depth, long time, DEPTH_DIRECTION direction) {

            if(direction == DEPTH_DIRECTION.INCREASING) {
                int depthDifference = depth - AvgDepthCalculator.getLastStartDepth();
                //Log.i(TAG, String.valueOf(compressionStartPressure));
                pressure = (float)((sbp + dbp) * Math.log10((depthDifference + getAvgDepth()) / getAvgDepth())  + dbp);
            } else {
                float elapsedTime = time - lastPeakTime;
                pressure = 1.0f / (bpm * elapsedTime / const1 + 1 / lastPeakPressure);
            }

            return pressure;
        }
    }

    private static class BPMCalculator {

        private static final int MAX_BPM = 130; //Because of data limitations
        private static final int MIN_BPM = 70; //Because of data limitations

        private static final int LOGSIZE = 3;
        private static final Long MAX_LONG_VALUE = 2147483647L;

        private static ArrayList<Long> compressionStartTimes = new ArrayList<>();


        static {
            //we init log with ridiculously long compression times so that BPM is essentially 0 at start
            long dummyTime = 10000;
            for(int i = 0; i < LOGSIZE; i++) {
                compressionStartTimes.add(dummyTime * i);
            }
        }

        public static void registerCompressionStart(long time) {
            compressionStartTimes.add(time);
            if(compressionStartTimes.size() > LOGSIZE) {
                compressionStartTimes.remove(0);
            }
        }

        private static float calculateAverageCompressionTime() {
            long totalTime = 0L;
            for(int i = 0; i < LOGSIZE - 1; i++) {
                long time1 = compressionStartTimes.get(i);
                long time2 = compressionStartTimes.get(i + 1);
                totalTime += (time2 - time1);
            }
            return (float) totalTime / (LOGSIZE - 1);
        }

        public static float calculateBPM() {
            bpm = 60000f / calculateAverageCompressionTime();
            if(bpm > MAX_BPM) {
                bpm = MAX_BPM;
            } else if(bpm < MIN_BPM) {
                bpm = MIN_BPM;
            }
            return bpm;
        }
    }

    private static class PeakCalculator {
        private static final int LOGSIZE = 2;
        private static final long TIME_DELAY = 200; //ms
        private static final int DEPTH_DIFFERENCE = 5;
        private static final float DEPTH_MOVING_BOUNDARY = 0.002f; //if slope is greater than positive val then increasing. less than negative then decreasing. else straight

        private static ArrayList<Integer> depthLog = new ArrayList<>();
        private static ArrayList<Long> timeLog = new ArrayList<>();

        static {
            for(int i = 0; i < LOGSIZE; i++) {
                depthLog.add(0);
                timeLog.add(0L);
            }
        }

        private static long lastLoggedTime = 0;
        private static int lastLoggedDepth = 0;

        private static DEPTH_DIRECTION previousDirection = DEPTH_DIRECTION.STRAIGHT;

        public static DEPTH_DIRECTION update(long time, int depth) {
            lastLoggedTime = time;
            lastLoggedDepth = depth;
            timeLog.add(time);
            depthLog.add(depth);
            if(timeLog.size() > LOGSIZE) {
                timeLog.remove(0);
                depthLog.remove(0);
            }
            DEPTH_DIRECTION direction = getDepthDirection();

            handleDirectionChanges(depth, time, direction);

            return direction;
        }

        public static DEPTH_DIRECTION getDepthDirection() {
            //if the difference between the last two changes is large enough, we can assume
            // it was just a small change and still relatively straight
            if(timeLog.get(LOGSIZE - 1) - timeLog.get(LOGSIZE - 2) > TIME_DELAY) {
                return DEPTH_DIRECTION.STRAIGHT;
            }
            //if there was a big jump in depth in either direction we can assume increasing or decreasing
            if(depthLog.get(LOGSIZE - 1) - depthLog.get(LOGSIZE - 2) > DEPTH_DIFFERENCE) {
                return DEPTH_DIRECTION.INCREASING;
            } else if (depthLog.get(LOGSIZE - 2) - depthLog.get(LOGSIZE - 1) > DEPTH_DIFFERENCE) {
                return DEPTH_DIRECTION.DECREASING;
            }
            //finally, for other cases we check slope
            float slope = (depthLog.get(LOGSIZE - 1) - depthLog.get(0)) / (float)(timeLog.get(LOGSIZE - 1) - timeLog.get(0)); //rise/run
            if(slope < -DEPTH_MOVING_BOUNDARY) {
                return DEPTH_DIRECTION.DECREASING;
            } else if(slope > DEPTH_MOVING_BOUNDARY) {
                return DEPTH_DIRECTION.INCREASING;
            } else {
                return DEPTH_DIRECTION.STRAIGHT;
            }
        }
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
         * x = compression rate (bpm)
         * y = depth (mm)
         */
        static double getDBP(float x, float y) {
            if(x < 10) {
                return 0;
            }

            return p00 + p10*x + p01*y + p20*pow(x,2) + p11*x*y + p02*pow(y,2) + p30*pow(x,3) + p21*pow(x,2)*y
                    + p12*x*pow(y,2) + p03*pow(y,3);
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
         * x = compression rate (bpm)
         * y = depth (mm)
         */
        static double getSBP(float x, float y) {
            if(x < 10) {
                return 0;
            }

            return p00 + p10*x + p01*y + p20*pow(x,2) + p11*x*y + p02*pow(y,2) + p30*pow(x,3) + p21*pow(x,2)*y
                    + p12*x*pow(y,2) + p03*pow(y,3);
        }
    }


}
