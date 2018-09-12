package com.application.nick.cprdummy;

import android.util.Log;

import com.application.nick.cprdummy.BPUtil.BPManager;
import com.application.nick.cprdummy.BPUtil.DataPoint;
import com.application.nick.cprdummy.BPUtil.DirectionCalculator;

import java.util.ArrayList;

/**
 * Created by Nick on 8/9/2017.
 */

public abstract class BPMonitor {

    private static final String TAG = "BloodPressureMonitor";

    private boolean RUNNING = true;

    private ArrayList<DataPoint> tempDataPointLog = new ArrayList<>();
    private long startTime = -1L;
    private int lastLoggedDepth;
    private int lastLoggedVents;
    private DataPoint lastLoggedDataPoint;

    BPManager bpManager;

    public BPMonitor() {

        bpManager = new BPManager();

        new UpdateThread().start();
    }

    public void update(long currentTime, int currentDepth, int currentVents) {
        if(currentDepth < 0) currentDepth = 0;
        if(currentVents < 0) currentVents = 0;

        lastLoggedDepth = currentDepth;
        lastLoggedVents = currentVents;
        if (startTime < 0) {
            startTime = currentTime;
        }
        DataPoint nextDataPoint = new DataPoint(currentTime, currentDepth, currentVents);
        tempDataPointLog.add(nextDataPoint);
    }

    public float getBPM() { return bpManager.getBPM(); }
    public float getAvgDepth() { return bpManager.getAvgAbsoluteDepth(); }
    public float getAvgLeaningDepth() { return bpManager.getAvgLeaningDepth(); }
    public float getSBP() {return bpManager.getSBP();}
    public float getDBP() {return bpManager.getDBP();}
    public int getEndTitle() {
        return bpManager.getEndTitle();
    }
    public float getVentRate() { return bpManager.getVentRate(); }

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

                if(tempDataPointLog.size() > 1) {
                    try {
                        sleep(10);
                        //System.out.println("DataPoint log size = " + tempDataPointLog.size());
                        DataPoint nextDataPoint = tempDataPointLog.remove(0);
                        if(nextDataPoint != null) {
                            bpManager.addDataPoint(nextDataPoint);


                            if (bpManager.isDataPointLogFull()) {
                                DataPoint dataPoint = bpManager.getNextDataPoint();

                                float pressure = bpManager.getPressure(dataPoint);
                                float endTitle = dataPoint.endTitle;
                                //Log.i(TAG, dataPoint.toString());

                                //make waveform more smooth by plotting intermediate datapoints
                                if (lastLoggedDataPoint != null) {
                                    int deltaDepthTotal = dataPoint.depth - lastLoggedDataPoint.depth;
                                    if (Math.abs(deltaDepthTotal) > 1) {
                                        int deltaDepth = deltaDepthTotal / Math.abs(deltaDepthTotal);
                                        long deltaTime = dataPoint.time - lastLoggedDataPoint.time;
                                        deltaTime /= Math.abs(deltaDepthTotal);
                                        for (int i = 1; i < Math.abs(deltaDepthTotal); i++) {
                                            long time = lastLoggedDataPoint.time + deltaTime * i;
                                            int depth = lastLoggedDataPoint.depth + deltaDepth * i;
                                            DataPoint intermediateDataPoint = new DataPoint(time, depth, dataPoint);
                                            float intermediatePressure = bpManager.getPressure(intermediateDataPoint);
                                            plotPressure((intermediateDataPoint.time - startTime) / 1000f, intermediatePressure);
                                        }
                                    }
                                }
                                // plot everything. We want to plot lastLoggedDepth and lastLoggedVents
                                // so that the depth and vents bar graphs are real time. Everything else
                                // has a slight delay
                                plotAll((dataPoint.time - startTime) / 1000f, lastLoggedDepth, lastLoggedVents, pressure, endTitle);

                                lastLoggedDataPoint = dataPoint;
                            }
                       }

                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    public abstract void plotDepth(int depth);
    public abstract void plotVents(int vents);
    public abstract void plotPressure(float time, float pressure);
    public abstract void plotEndTitle(float time, float endTitle);
    public abstract void plotAll(float time, int depth, int vents, float pressure, float endTitle);
}
