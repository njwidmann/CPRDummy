package com.application.nick.cprdummy;

/**
 * Created by Nick on 8/10/2017.
 */

public abstract class EndTitleMonitor {

    private static final int HIGH_VALUE = 30;
    private static final int LOW_VALUE = 0;
    private static final long HIGH_TIME = 2000L;
    private static final long LOW_TIME = 1000L;

    private static boolean RUNNING = true;

    private long cycleStartTime;
    private long initialStartTime;
    private long lastLoggedTime;

    private boolean high, low;

    public EndTitleMonitor() {

        new UpdateThread().start();

    }

    private  long getElapsedCycleTime() {
        return getCurrentTime() - cycleStartTime;
    }

    private long getElapsedTotalTime() {
        return getCurrentTime() - initialStartTime;
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
            low = true;
            while(BloodPressureMonitor.getStartTime() <= 0) {
                //wait for simulation to start
            }

            cycleStartTime = getCurrentTime();
            initialStartTime = cycleStartTime;
            lastLoggedTime = 0;

            while(RUNNING) {
                    if (low && getElapsedCycleTime() > LOW_TIME) {
                        low = false;
                        high = true;
                        cycleStartTime = getCurrentTime();
                    } else if (high && getElapsedCycleTime() > HIGH_TIME) {
                        high = false;
                        low = true;
                        cycleStartTime = getCurrentTime();
                    }
                    if(getCurrentTime() - lastLoggedTime > 200) {
                        if (low) plotEndTitle(getElapsedTotalTime() / 1000f, LOW_VALUE);
                        else if (high) plotEndTitle(getElapsedTotalTime() / 1000f, HIGH_VALUE);
                        lastLoggedTime = getCurrentTime();
                    }
            }
        }
    }

    public abstract void plotEndTitle(float time, float value);
}
