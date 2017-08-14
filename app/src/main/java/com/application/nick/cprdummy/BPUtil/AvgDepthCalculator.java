package com.application.nick.cprdummy.BPUtil;

import java.util.ArrayList;

/**
 * Created by Nick on 8/11/2017.
 */

public class AvgDepthCalculator {

    private static final int LOGSIZE = 3; //number of last depths to average

    private ArrayList<Integer> compressionStartDepths;
    private ArrayList<Integer> compressionPeakDepths;

    private float avgRelDepth, avgAbsDepth;

    public AvgDepthCalculator() {
        compressionStartDepths = new ArrayList<>();
        compressionPeakDepths = new ArrayList<>();

        avgRelDepth = 0;
        avgAbsDepth = 0;
    }

    public int getLastStartDepth() {
        return compressionStartDepths.get(compressionStartDepths.size() - 1);
    }

    public void registerCompressionStart(int depth) {
        compressionStartDepths.add(depth);
        if(compressionStartDepths.size() > LOGSIZE) {
            compressionStartDepths.remove(0);
        }
    }

    public void registerCompressionPeak(int depth) {
        compressionPeakDepths.add(depth);
        if(compressionPeakDepths.size() > LOGSIZE) {
            compressionPeakDepths.remove(0);
        }
    }

    /**
     * averages last LOGSIZE depths to return average relative depth
     * @return avg relative depth
     */
    public float calculateAvgRelativeCompressionDepth() {
        if(compressionPeakDepths.size() < 1) {
            return 0; //We can't average if there are no values
        }
        int totalDepth = 0;
        for(int i = 0; i < compressionStartDepths.size(); i++) {
            long depth = compressionPeakDepths.get(i) - compressionStartDepths.get(i);
            totalDepth += depth;
        }
        avgRelDepth = (float) totalDepth / compressionPeakDepths.size();

        return avgRelDepth;
    }

    public float calculateAvgAbsoluteCompressionDepth() {
        if(compressionPeakDepths.size() < 1) {
            return 0; //We can't average if there are no values
        }
        int totalDepth = 0;
        for(int i = 0; i < compressionPeakDepths.size(); i++) {
            long depth = compressionPeakDepths.get(i);
            totalDepth += depth;
        }
        avgAbsDepth = (float) totalDepth / compressionPeakDepths.size();

        return avgAbsDepth;
    }

    public void refreshAvgDepth() {
        compressionStartDepths.clear();
        compressionPeakDepths.clear();
    }
}
