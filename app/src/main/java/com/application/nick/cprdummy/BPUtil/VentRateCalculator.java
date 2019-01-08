package com.application.nick.cprdummy.BPUtil;

import java.util.ArrayList;

/**
 * Created by Nick on 8/17/2017.
 * This class handles rate calculations for ventilations. Calculates rate over the last 30 seconds.
 */

public class VentRateCalculator {

    private static final long LOG_TIME = 30;

    private ArrayList<Long> ventStartTimes;

    public VentRateCalculator() {
        ventStartTimes = new ArrayList<>();
    }

    /**
     * call this at the every ventilation so that we can measure ventilation rate
     */
    public void registerVent() {
        ventStartTimes.add(System.currentTimeMillis());
    }

    public float getRate() {
        long time = System.currentTimeMillis();
        // while: log size > 0 and time of the earliest logged is > LOG_TIME sec ago.... remove from log
        while (ventStartTimes.size() > 0 && time - ventStartTimes.get(0) > LOG_TIME * 1000) {
            ventStartTimes.remove(0);
        }

        //num ccs in log_time / log_time * 60 sec/min = rate in ccs/min
        return ventStartTimes.size() / (float)LOG_TIME * 60;
    }


}
