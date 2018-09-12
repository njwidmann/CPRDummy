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
     * @param time vent time
     */
    public void registerVent(long time) {
        ventStartTimes.add(time);

        while (ventStartTimes.size() > 0 && //log size > 0
                time - ventStartTimes.get(0) > LOG_TIME * 1000) { // and time of the earliest logged is > 5sec ago.... remove from log
            ventStartTimes.remove(0);
        }
    }

    public float getRate() {
        //num ccs in log_time / log_time * 60 sec/min = rate in ccs/min
        return ventStartTimes.size() / (float)LOG_TIME * 60;
    }

    public void refresh() {
        //ventStartTimes.clear();
    }

}
