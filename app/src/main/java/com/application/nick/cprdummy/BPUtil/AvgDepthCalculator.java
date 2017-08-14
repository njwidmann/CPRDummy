package com.application.nick.cprdummy.BPUtil;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Nick on 8/11/2017.
 */

public class AvgDepthCalculator {

    private static final int LOGSIZE = 3; //number of last depths to average

    private ArrayList<Integer> compressionStartDepths;
    private ArrayList<Integer> compressionEndDepths;

    private float avgDepth;

    public AvgDepthCalculator() {
        compressionStartDepths = new ArrayList<>();
        compressionEndDepths = new ArrayList<>();
        //for(int i = 0; i < LOGSIZE; i++) {
        //    compressionStartDepths.add(0);
        //    compressionEndDepths.add(0);
        //}

        avgDepth = 0;
    }

    public int getLastStartDepth() {
        //return compressionStartDepths.get(LOGSIZE - 1);
        return compressionStartDepths.get(compressionStartDepths.size() - 1);
    }

    public void registerCompressionStart(int depth) {
        compressionStartDepths.add(depth);
        if(compressionStartDepths.size() > LOGSIZE) {
            compressionStartDepths.remove(0);
        }
    }

    public void registerCompressionPeak(int depth) {
        compressionEndDepths.add(depth);
        if(compressionEndDepths.size() > LOGSIZE) {
            compressionEndDepths.remove(0);
        }
    }

    /**
     * averages last LOGSIZE depths to return average depth
     * @return avg depth
     */
    public float calculateAvgCompressionDepth() {
        //if(compressionEndDepths.size() < LOGSIZE) {
        if(compressionEndDepths.size() < 1) {
            return 0; //We can't average if there are no values
        }
        int totalDepth = 0;
        //for(int i = 0; i < LOGSIZE; i++) {
        for(int i = 0; i < compressionStartDepths.size(); i++) {
            long depth = compressionEndDepths.get(i) - compressionStartDepths.get(i);
            totalDepth += depth;
        }
        avgDepth = (float) totalDepth / compressionEndDepths.size();

        /*if(avgDepth > MAX_DEPTH) {
            avgDepth = MAX_DEPTH;
        } else if(avgDepth < MIN_DEPTH) {
            avgDepth = MIN_DEPTH;
        }*/

        return avgDepth;
    }

    public void refreshAvgDepth() {
        compressionStartDepths.clear();
        compressionEndDepths.clear();
    }
}
