package com.application.nick.cprdummy.BPUtil;

import java.util.ArrayList;

/**
 * Created by Nick on 8/11/2017.
 */

public class EndTitleCalculator {
    private static final int LOW_VALUE = 0;

    private static final long LOG_TIME = 10; //time we keep track of SBP for average, which affects ETCO2 val

    private boolean high;

    private ArrayList<Long> compressionStartTimes;
    private ArrayList<Float> sbpLog;

    private int highValue = 0;

    public EndTitleCalculator() {
        high = false;
        sbpLog = new ArrayList<>();
        compressionStartTimes = new ArrayList<>();
    }

    public void registerCompression(float sbp, long time) {
        sbpLog.add(sbp);
        compressionStartTimes.add(time);

        // while: log size > 0 and time of the earliest logged is > LOG_TIME sec ago.... remove from log
        while (compressionStartTimes.size() > 0 &&
                time - compressionStartTimes.get(0) > LOG_TIME * 1000) {
            compressionStartTimes.remove(0);
            sbpLog.remove(0);
        }
    }

    public int getEndTitle() {
        //handle breathing cycle changes
        if (high) {
            return highValue;
        } else {
            return LOW_VALUE;
        }
    }

    public void setHigh(float ventRate) {
        high = true;
        highValue = (int)BPFunctions.calculateEndTidal(getAvgSBP(), ventRate);
    }

    public void setLow() {
        high = false;
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

    public void refresh() {
        //clearSBPLog();
    }
}
