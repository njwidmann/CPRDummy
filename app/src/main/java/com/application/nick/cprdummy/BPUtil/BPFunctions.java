package com.application.nick.cprdummy.BPUtil;

import static com.application.nick.cprdummy.BPUtil.BPManager.BASELINE_BP;

/**
 * Created by Nick on 9/6/2018.
 */

public class BPFunctions {

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
    private static final float[] RATE_PENALTY = {
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

    private static float adjustBPForBPM(float bp, float bpm) {
        return bp * (1 - getBPMPenaltyFactor(bpm));
    }

    private static float getBPMPenaltyFactor(float rate) {
        for(int i = 1; i < RATE.length; i++) {
            if(rate < RATE[i]) {
                float y0 = RATE_PENALTY[i-1];
                float slope = (RATE_PENALTY[i] - RATE_PENALTY[i-1]) / (RATE[i] - RATE[i-1]);
                float x = rate - RATE[i-1];
                return slope * x + y0;
            }
        }
        return 1; //if rate > 300 or rate < 0, return 1
    }

    /**
     * adjusts/penalizes bp based on pauses
     * @param bp current bp
     * @param x (average rate over last 5 seconds) / (max instantaneous rate over the last 5 seconds)
     * @return adjusted BP
     */
    private static float adjustBPForPauses(float bp, float x) {
        float bpDifferential = bp - BASELINE_BP;
        if(bpDifferential < 0) bpDifferential = 0;

        return calculatePauseScaleFactor(x) * bpDifferential + BASELINE_BP;
    }

    /**
     * we calculate a pause penalty as a function of (average rate over last 5 seconds) / (max instantaneous
     * rate over the last 5 seconds)
     * @param x (average rate over last 5 seconds) / (max instantaneous rate over the last 5 seconds)
     * @return BP scale factor between 0 and 1. 1 = no pause penalty
     */
    private static float calculatePauseScaleFactor(float x) {
        return -1f / (125 * (float)Math.pow(x, 4) + 3f) + 1;
    }

    private static float adjustSBPForLeaning(float sbp, float avgLeaningDepth) {
        double leaningPenalty = .0237*Math.pow(Math.log(avgLeaningDepth+1), 1.62);

        return (float)(sbp * (1-leaningPenalty));
    }

    private static float adjustDBPForLeaning(float dbp, float avgLeaningDepth) {
        double leaningPenalty = .0501*Math.pow(Math.log(avgLeaningDepth+1), 1.62);

        return (float)(dbp * (1-leaningPenalty));
    }

    private static float adjustSBPForVentRate(float sbp, float ventRate) {
        if(ventRate > 20) {
            sbp = sbp * (float)7.0/8;
        }
        return sbp;
    }

    private static float adjustDBPForVentRate(float dbp, float ventRate) {
        if(ventRate > 20) {
            dbp = dbp * (float)2/3;
        }
        return dbp;
    }

    private static float adjustEndTidalForVentRate(float endTidal, float ventRate) {
        if(ventRate > 20) {
            endTidal = endTidal / 2;
        }
        return endTidal;
    }

    /**
     * Finds the correct high end tidal value based on sbp and ventilation rate
     * @return high end title value
     */
    public static float calculateEndTidal(float sbp, float ventRate) {
        float endTidal = (float)(35.0/85 * sbp);
        return adjustEndTidalForVentRate(endTidal, ventRate);
    }

    public static float calculateDBP(float avgDepth, float bpm, float avgLeaningDepth, float pauseFraction, float ventRate) {
        float dbp = DBPCalculator.getDBP(avgDepth);
        dbp = BPFunctions.adjustBPForBPM(dbp, bpm);
        dbp = BPFunctions.adjustDBPForLeaning(dbp, avgLeaningDepth);
        dbp = BPFunctions.adjustBPForPauses(dbp, pauseFraction);
        dbp = BPFunctions.adjustDBPForVentRate(dbp, ventRate);
        return dbp;
    }

    public static float calculateSBP(float avgDepth, float bpm, float avgLeaningDepth, float pauseFraction, float ventRate) {
        float sbp = SBPCalculator.getSBP(avgDepth);
        sbp = BPFunctions.adjustBPForBPM(sbp, bpm);
        sbp = BPFunctions.adjustSBPForLeaning(sbp, avgLeaningDepth);
        sbp = BPFunctions.adjustBPForPauses(sbp, pauseFraction);
        sbp = BPFunctions.adjustSBPForVentRate(sbp, ventRate);
        return sbp;
    }

    /**
     * arbitrary function to simulate the behaviour of the BP waveform during compression
     * @param x relative depth difference / average relative depth
     * @return pressure fraction of (sbp - dbp)
     */
    public static float increaseFunction(float x) {
        return (float)(-1.1/(10*Math.pow(x,2) + 1) + 1.1);
    }

    /**
     * arbitrary function to simulate the behaviour of the BP waveform during expansion
     * @param x 1 - depth / peakDepth
     * @return pressure fraction of (peak pressure - dbp)
     */
    public static float decreaseFunction(float x) {
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
    public static float straightDecayFunction(float x) {
        double term1 = 1.0/(125 * Math.pow(x, 4) + 1);
        double term2 = Math.sin(17.0 * Math.PI * (x + 0.29)) / 10;
        float frac = (float)((term1 + term2 + 0.0921) / 1.192);
        if(x < 1) {
            return frac;
        } else {
            return frac / x;
        }
    }

    /**
     * Get's ET increase progress as a percentage (0-1) of high value based on x (elapsed_time /
     * INCREASE_TIME)
     * @param x elapsed_time / INCREASE_TIME
     * @return ET increase progress as a percentage (0-1)
     */
    public static float etIncreaseFunction(float x) {
        return (float)(-1.1/(10*Math.pow(x,2) + 1) + 1.1);
    }

    private static class DBPCalculator {

        private static final double c1 = -.00045097;
        private static final double c2 = 0.0307429;
        private static final double c3 = .24695878;
        private static final double c4 = BASELINE_BP;

        /**
         * gets diastolic blood pressure based just on average depth
         * depth = depth (mm)
         */
        static float getDBP(float depth) {
            return (float) (c1 * Math.pow(depth, 3) + c2 * Math.pow(depth, 2) + c3 * depth + c4);
        }
    }


    private static class SBPCalculator {

        private static final double c1 = -.0297;
        private static final double c2 = 3.2635;
        private static final double c3 = BASELINE_BP;


        /**
         * gets systolic blood pressure based just on average depth
         * depth = depth (mm)
         */
        static float getSBP(float depth) {
            return (float) (c1 * Math.pow(depth, 2) + c2 * depth + c3);
        }
    }


}
