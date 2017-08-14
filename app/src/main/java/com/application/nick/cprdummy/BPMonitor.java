package com.application.nick.cprdummy;

import android.util.Log;

import com.application.nick.cprdummy.BPUtil.BPManager;

import java.util.ArrayList;

/**
 * Created by Nick on 8/9/2017.
 */

public abstract class BPMonitor {

    private static final String TAG = "BloodPressureMonitor";

    private boolean RUNNING = true;

    private ArrayList<Integer> depthLog = new ArrayList<>();
    private ArrayList<Long> timeLog = new ArrayList<>();
    private long startTime = -1L;
    private long lastLoggedSystemTime;
    private long lastLoggedTime = 0;
    private int lastLoggedDepth;

    BPManager bpManager;

    public BPMonitor() {

        bpManager = new BPManager();

        new UpdateThread().start();
    }

    public void updateDepth(long currentTime, int currentDepth) {
        if(currentDepth < 0) currentDepth = 0;
        if(currentTime > lastLoggedTime) { //we don't want to log any points out of order. There's a chance this could happen because we are creating straight data on Android
            lastLoggedTime = currentTime;
            lastLoggedDepth = currentDepth;
            if (startTime < 0) {
                startTime = currentTime;
                lastLoggedSystemTime = getCurrentTime();
            }
            depthLog.add(currentDepth);
            timeLog.add(currentTime);
        }
    }

    public float getBPM() { return bpManager.getBPM(); }
    public float getAvgDepth() { return bpManager.getAvgAbsoluteDepth(); }
    public float getAvgLeaningDepth() { return bpManager.getAvgLeaningDepth(); }
    public float getSBP() {return bpManager.getSBP();}
    public float getDBP() {return bpManager.getDBP();}

    private long getStartTime() {
        return startTime;
    }

    private long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

    public void refresh() {
        startTime = -1L;
        bpManager = new BPManager();
    }

    public void release() {
        RUNNING = false;
    }

    private class UpdateThread extends Thread {
        @Override
        public void run() {
            while(RUNNING) {
                if(getStartTime() > 0 && getCurrentTime() - lastLoggedSystemTime > 100) {
                    //add straight data. arduino only logs on changes so we can assume constant depth if no update
                    updateDepth(lastLoggedTime + 100, lastLoggedDepth);
                }
                if(timeLog.size() > 0 && depthLog.size() > 0) {
                    try {
                        int depth = depthLog.remove(0);
                        long time = timeLog.remove(0);

                        BPManager.DEPTH_DIRECTION direction = bpManager.getDepthDirection(time, depth);
                        float pressure = bpManager.getPressure(depth, time, direction);
                        float endTitle = bpManager.getEndTitle(time);
                        if(direction == BPManager.DEPTH_DIRECTION.INCREASING) {
                            Log.i(TAG, "Time = " + time + "; Depth = " + depth + "; Direction = INCREASING; BPM = " + bpManager.getBPM() + "; AvgDepth = " + bpManager.getAvgRelativeDepth() + "; Pressure = " + pressure + "; SBP = " + bpManager.getSBP() + "; DBP = " + bpManager.getDBP());
                        } else if(direction == BPManager.DEPTH_DIRECTION.DECREASING) {
                            Log.i(TAG, "Time = " + time + "; Depth = " + depth + "; Direction = DECREASING; BPM = " + bpManager.getBPM() + "; AvgDepth = " + bpManager.getAvgRelativeDepth() + "; Pressure = " + pressure + "; SBP = " + bpManager.getSBP() + "; DBP = " + bpManager.getDBP());
                        } else {
                            Log.i(TAG, "Time = " + time + "; Depth = " + depth + "; Direction = STRAIGHT; BPM = " + bpManager.getBPM() + "; AvgDepth = " + bpManager.getAvgRelativeDepth() + "; Pressure = " + pressure + "; SBP = " + bpManager.getSBP() + "; DBP = " + bpManager.getDBP());
                        }

                        plotAll((time - startTime)/1000f, lastLoggedDepth, pressure, endTitle);
                        lastLoggedSystemTime = getCurrentTime();

                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }
        }
    }

    public abstract void plotDepth(int depth);
    public abstract void plotPressure(float time, float pressure);
    public abstract void plotEndTitle(float time, float endTitle);
    public abstract void plotAll(float time, int depth, float pressure, float endTitle);
}
