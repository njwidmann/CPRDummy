package com.application.nick.cprdummy.BPUtil;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Nick on 8/17/2017.
 * This class handles pause time calculations by keeping a running average of instantaneous rates
 * during the last n seconds. It also provides a better representation of rate for user display
 * because it averages it over the last n seconds.
 */

public class PauseCalculator {

    private static final long LOG_TIME = 5;

    private ArrayList<Long> compressionStartTimes;
    private ArrayList<Float> bpmLog;

    public PauseCalculator() {
        compressionStartTimes = new ArrayList<>();
        bpmLog = new ArrayList<>();
    }

    /**
     * we calculate a pause penalty based on average rate over last 5 seconds and max instantaneous
     * rate over the last 5 seconds so we need to register all compression start times and instantaneous
     * rates
     * @param time compression start time
     * @param bpm instantaneous rate when compression starts
     */
    public void registerCompressionStart(long time, float bpm) {
        bpmLog.add(bpm);
        compressionStartTimes.add(time);

        // while: log size > 0 and time of the earliest logged is > LOG_TIME sec ago.... remove from log
        while (compressionStartTimes.size() > 0 &&
                time - compressionStartTimes.get(0) > LOG_TIME * 1000) {
            compressionStartTimes.remove(0);
            bpmLog.remove(0);
        }
    }

    /**
     * Pause fraction representing: (average rate over last 5 seconds) / (max instantaneous
     * rate over the last 5 seconds) ... 1 = no pause
     * @return pause fraction
     */
    public float getPauseFraction() {
        return getAvgBPM() / getMaxBPM();
    }

    public float getAvgBPM() {
        //num ccs in log_time / log_time * 60 sec/min = rate in ccs/min
        return compressionStartTimes.size() / (float)LOG_TIME * 60;
    }

    private float getMaxBPM() {
        if(bpmLog.size() == 0) return 1;
        float maxBPM = 0;
        for(int i = bpmLog.size() - 1; i >= 0; i--) {
            if(bpmLog.get(i) > maxBPM) {
                maxBPM = bpmLog.get(i);
            }
        }
        return maxBPM;
    }



    public void refresh() {
        compressionStartTimes.clear();
        bpmLog.clear();
    }

}
