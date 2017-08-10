package com.application.nick.cprdummy;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Nick on 8/9/2017.
 */

public abstract class BloodPressureMonitor {

    private static final String TAG = "BloodPressureMonitor";

    private static boolean RUNNING = true;

    private static ArrayList<Integer> depthLog = new ArrayList<>();
    private static ArrayList<Long> timeLog = new ArrayList<>();
    private static long startTime = -1L;
    private static long lastLoggedSystemTime;
    private static long lastLoggedTime;
    private static int lastLoggedDepth;

    public BloodPressureMonitor() {

        new UpdateThread().start();
    }

    public void updateDepth(long currentTime, int currentDepth) {
        if(currentDepth < 0) currentDepth = 0;
        lastLoggedTime = currentTime;
        lastLoggedDepth = currentDepth;
        if(startTime < 0) {
            startTime = currentTime;
        }
        depthLog.add(currentDepth);
        timeLog.add(currentTime);
        lastLoggedSystemTime = getCurrentTime();
    }

    public static long getStartTime() {
        return startTime;
    }

    private long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

    public void release() {
        RUNNING = false;
    }

    private class UpdateThread extends Thread {
        @Override
        public void run() {
            while(RUNNING) {
                if(getStartTime() > 0 && getCurrentTime() - lastLoggedSystemTime > 200) {
                    //add straight data. arduino only logs on changes so we can assume constant depth if no update
                    updateDepth(lastLoggedTime + 200, lastLoggedDepth);
                }
                if(timeLog.size() > 0 && depthLog.size() > 0) {
                    try {
                        int depth = depthLog.remove(0);
                        long time = timeLog.remove(0);

                        BPUtil.DEPTH_DIRECTION direction = BPUtil.getDepthDirection(time, depth);
                        float pressure = BPUtil.getPressure(depth, time, direction);
                        float endTitle = BPUtil.getEndTitle(time);
                        if(direction == BPUtil.DEPTH_DIRECTION.INCREASING) {
                            Log.i(TAG, "Time = " + time + "; Depth = " + depth + "; Direction = INCREASING; BPM = " + BPUtil.getBPM() + "; AvgDepth = " + BPUtil.getAvgDepth() + "; Pressure = " + pressure + "; SBP = " + BPUtil.getSBP() + "; DBP = " + BPUtil.getDBP());
                        } else if(direction == BPUtil.DEPTH_DIRECTION.DECREASING) {
                            Log.i(TAG, "Time = " + time + "; Depth = " + depth + "; Direction = DECREASING; BPM = " + BPUtil.getBPM() + "; AvgDepth = " + BPUtil.getAvgDepth() + "; Pressure = " + pressure + "; SBP = " + BPUtil.getSBP() + "; DBP = " + BPUtil.getDBP());
                        } else {
                            Log.i(TAG, "Time = " + time + "; Depth = " + depth + "; Direction = STRAIGHT; BPM = " + BPUtil.getBPM() + "; AvgDepth = " + BPUtil.getAvgDepth() + "; Pressure = " + pressure + "; SBP = " + BPUtil.getSBP() + "; DBP = " + BPUtil.getDBP());
                        }

                        plotAll((time - startTime)/1000f, depth, pressure, endTitle);

                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }
        }
    }

    public abstract void plotDepth(float time, int depth);
    public abstract void plotPressure(float time, float pressure);
    public abstract void plotEndTitle(float time, float endTitle);
    public abstract void plotAll(float time, int depth, float pressure, float endTitle);
}
