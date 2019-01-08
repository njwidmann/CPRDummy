
package com.application.nick.cprdummy.BPUtil;

import static java.lang.Math.max;

/**
 * Created by Nick on 8/9/2017.
 */

public class BPManager extends MechanicalManager {

    private static final String TAG = "MechanicalManager";

    public static final float BASELINE_BP = 5;

    private float sbp;
    private float dbp;

    private BPCalculator bpCalculator;
    private EndTitleCalculator endTitleCalculator;

    public BPManager() {
        super();

        sbp = BASELINE_BP;
        dbp = BASELINE_BP;

        endTitleCalculator = new EndTitleCalculator();
        bpCalculator = new BPCalculator();

    }

    public void addDataPoint(DataPoint dataPoint) {
        super.addDataPoint(dataPoint);

        dataPoint.sbp = getSBP();
        dataPoint.dbp = getDBP();
        dataPoint.endTitle = endTitleCalculator.getEndTitle();
    }

    protected void handleStraightTimeout() {
        super.handleStraightTimeout();
        sbp = 0;
        dbp = 0;
    }

    protected void registerCompressionStart(int depth, long time) {
        super.registerCompressionStart(depth, time);
        bpCalculator.registerCompressionStart(depth, getAvgRelativeDepth());

        sbp = calculateSBP();
        dbp = calculateDBP();

        //update sbp and dbp for all the datapoints in the log that came before compression start was recognized.
        //remember none of the datapoints in the log have been displayed yet.
        for(int i = 0; i < super.dataPoints.size(); i++) {
            if(super.dataPoints.get(i).depthDirection == DirectionCalculator.DIRECTION.INCREASING) {
                super.dataPoints.get(i).sbp = sbp;
                super.dataPoints.get(i).dbp = dbp;
            }
        }
    }
    protected void registerCompressionEnd(int depth, long time) {
        super.registerCompressionEnd(depth, time);
        bpCalculator.registerCompressionEnd(time);
    }
    protected void registerCompressionPeak(int depth, long time) {
        super.registerCompressionPeak(depth, time);
        bpCalculator.registerCompressionPeak(depth);
        endTitleCalculator.registerCompression(getSBP(), time);
    }

    protected void registerVentStart(int depth, long time) {
        super.registerVentStart(depth, time);
        endTitleCalculator.setLow();
    }
    protected void registerVentPeak(int depth, long time) {
        super.registerVentPeak(depth, time);
        endTitleCalculator.setHigh(getVentRate());
    }
    protected void registerVentEnd(int depth, long time) {
        super.registerVentEnd(depth, time);
        endTitleCalculator.setLow();
    }


    /**
     * call at the start of every compression
     * @return dbp
     */
    private float calculateDBP() {
        return BPFunctions.calculateDBP(getAvgRelativeDepth(), getInstantBPM(), getAvgLeaningDepth(), getPauseFraction(), getVentRate());
    }

    /**
     * call at the start of every compression
     * @return sbp
     */
    private float calculateSBP() {
        return BPFunctions.calculateSBP(getAvgRelativeDepth(), getInstantBPM(), getAvgLeaningDepth(), getPauseFraction(), getVentRate());
    }

    public float getDBP() {
        return dbp;
    }

    public float getSBP() {
        return sbp;
    }

    public int getEndTitle() {
        return endTitleCalculator.getHighValue();
    }

    public float getPressure(DataPoint dataPoint) {
        dataPoint.bp = bpCalculator.updateBP(dataPoint.depth, dataPoint.time, dataPoint.depthDirection, dataPoint.sbp, dataPoint.dbp);
        return dataPoint.bp;
    }


}

