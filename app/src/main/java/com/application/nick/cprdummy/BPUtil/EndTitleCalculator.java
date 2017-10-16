package com.application.nick.cprdummy.BPUtil;

import java.util.ArrayList;

/**
 * Created by Nick on 8/11/2017.
 */

public class EndTitleCalculator {
    private static final int LOW_VALUE = 0;
    private static final long HIGH_TIME = 4000L;
    private static final long LOW_TIME = 2000L;

    private static final int[] SBP_RATINGS = {
            50, // sbp <50 = bad
            65, // sbp 50-65 = poor
            80, // sbp 65-80 = average
            200 //sbp >80 = excellent
    };

    private static final int[] END_TITLE = {
            4, //sbp <50 -> ET=4
            8,
            15,
            30
    };

    private long cycleStartTime;

    private boolean low;
    private boolean high;

    private ArrayList<Float> sbpLog;

    private int highValue = 0;

    public EndTitleCalculator() {
        cycleStartTime = 0;
        low = true;
        high = false;
        sbpLog = new ArrayList<>();
    }

    public void registerCompression(float sbp) {
        sbpLog.add(sbp);
    }

    public int getEndTitle(long currentTime) {

        //handle breathing cycle changes
        if (low && currentTime - cycleStartTime > LOW_TIME) {
            low = false;
            high = true;
            cycleStartTime = currentTime;
            highValue = calculateMaxEndTitle(); //recalculate after every cycle based on the average sbp from that cycle
            clearSBPLog(); //clear log so we can start fresh for the next cycle
        } else if (high && currentTime - cycleStartTime > HIGH_TIME) {
            high = false;
            low = true;
            cycleStartTime = currentTime;
        }
        if (low) {
            return LOW_VALUE;
        } else {
            return highValue;
        }
    }

    /**
     * This method finds the correct high end title value based on last breathing cycle's average sbp.
     * If the log is empty, returns lowest value.
     * @return high end title value
     */
    public int calculateMaxEndTitle() {
        int sbpIndex = 0;
        float avgSBP = getAvgSBP();
        while(avgSBP > SBP_RATINGS[sbpIndex]) {
            sbpIndex++;
        }
        return END_TITLE[sbpIndex];
    }

    /**
     * @return average sbp in the log. If log is empty, returns an average of 0.
     */
    private float getAvgSBP() {
        if(sbpLog.size() == 0) return 0;
        float sbpSum = 0;
        for(int i = 0; i < sbpLog.size(); i++) {
            sbpSum += sbpLog.get(i);
        }
        return sbpSum / sbpLog.size();
    }

    /**
     * call this after every breathing cycle so that we can recalculate the next end title value
     * based only on the next cycle.
     */
    private void clearSBPLog() {
        sbpLog.clear();
    }

    /**
     * Gets the current high value in the ETC02 breathing cycle
     */
    public int getHighValue() {
        return highValue;
    }
}
