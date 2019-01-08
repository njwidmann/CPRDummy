package com.application.nick.cprdummy.BPUtil;

import java.util.ArrayList;

/**
 * Created by Nick on 9/6/2018.
 */

public class MechanicalManager {

    private DirectionCalculator depthDirectionCalculator, ventsDirectionCalculator;
    private AvgDepthCalculator avgDepthCalculator;
    private BPMCalculator bpmCalculator;
    private LeaningCalculator leaningCalculator;
    private PauseCalculator pauseCalculator;
    private VentRateCalculator ventRateCalculator;

    public static final int DATA_POINT_LOG_SIZE = 5;
    public static final int DEFAULT_AVG_DEPTH = 20;

    private float avgRelDepth, avgAbsDepth, avgLeaningDepth, instantBPM;

    private long straightStartTime;
    private static final long STRAIGHT_TIMEOUT = 5000; //after straight 5sec in a row (5 sec), we are going to reset BPM and AvgDepth.

    protected ArrayList<DataPoint> dataPoints;

    public MechanicalManager() {

        dataPoints = new ArrayList<>();

        instantBPM = 0;
        avgRelDepth = DEFAULT_AVG_DEPTH;
        avgAbsDepth = DEFAULT_AVG_DEPTH;
        avgLeaningDepth = 0;

        avgDepthCalculator = new AvgDepthCalculator();
        bpmCalculator = new BPMCalculator();
        leaningCalculator = new LeaningCalculator();
        pauseCalculator = new PauseCalculator();
        ventRateCalculator = new VentRateCalculator();

        depthDirectionCalculator = new DirectionCalculator(dataPoints, DataPoint.VALUE_TYPE.DEPTH) {
            @Override
            public void handleCycleStart(int value, long time) {
                registerCompressionStart(value, time);
            }

            @Override
            public void handleCycleEnd(int value, long time) {
                registerCompressionEnd(value, time);
            }

            @Override
            public void handleCyclePeak(int value, long time) {
                registerCompressionPeak(value, time);
            }
        };

        ventsDirectionCalculator = new DirectionCalculator(dataPoints, DataPoint.VALUE_TYPE.VENTS) {
            @Override
            public void handleCycleStart(int value, long time) {
                registerVentStart(value, time);
            }

            @Override
            public void handleCycleEnd(int value, long time) {
                registerVentEnd(value, time);
            }

            @Override
            public void handleCyclePeak(int value, long time) {
                registerVentPeak(value, time);
            }
        };

    }

    public void addDataPoint(DataPoint dataPoint) {
        dataPoints.add(dataPoint);
        depthDirectionCalculator.update(dataPoint);
        ventsDirectionCalculator.update(dataPoint);

        if(dataPoint.depthDirection != DirectionCalculator.DIRECTION.STRAIGHT) {
            straightStartTime = dataPoint.time;
        }
        if(dataPoint.time - straightStartTime >= STRAIGHT_TIMEOUT) {
            handleStraightTimeout();
        }
    }

    public boolean isDataPointLogFull() {
        return dataPoints.size() >= DATA_POINT_LOG_SIZE;
    }

    public DataPoint getNextDataPoint() {
        return dataPoints.remove(0);
    }

    protected void handleStraightTimeout() {
        bpmCalculator.refreshBPM();
        avgDepthCalculator.refreshAvgDepth();
        avgRelDepth = DEFAULT_AVG_DEPTH;
        avgAbsDepth = DEFAULT_AVG_DEPTH;
        avgLeaningDepth = leaningCalculator.setLeaningToLatestValue();
        depthDirectionCalculator.reset();
        pauseCalculator.refresh();
    }

    protected void registerCompressionStart(int depth, long time) {
        bpmCalculator.registerCompressionStart(time);
        instantBPM = bpmCalculator.calculateBPM();
        pauseCalculator.registerCompressionStart(time, instantBPM);
        avgDepthCalculator.registerCompressionStart(depth);
    }
    protected void registerCompressionEnd(int depth, long time) {
        avgRelDepth = avgDepthCalculator.calculateAvgRelativeCompressionDepth();
        avgAbsDepth = avgDepthCalculator.calculateAvgAbsoluteCompressionDepth();
        avgLeaningDepth = leaningCalculator.registerLeaningDepth(depth);
    }
    protected void registerCompressionPeak(int depth, long time) {
        avgDepthCalculator.registerCompressionPeak(depth);
    }

    protected void registerVentStart(int depth, long time) {
        System.out.println("VENT START");
    }
    protected void registerVentPeak(int depth, long time) {
        ventRateCalculator.registerVent();
        System.out.println("VENT PEAK");
    }
    protected void registerVentEnd(int depth, long time) {
        System.out.println("VENT END");
    }

    public float getInstantBPM() {
        return instantBPM;
    }
    public float getBPM() {
        return pauseCalculator.getAvgBPM();
    }
    public float getPauseFraction() {
        return pauseCalculator.getPauseFraction();
    }
    public float getAvgLeaningDepth() {
        return avgLeaningDepth;
    }
    public float getAvgRelativeDepth() {
        return avgRelDepth;
    }
    public float getAvgAbsoluteDepth() {
        return avgAbsDepth;
    }
    public float getVentRate() {
        return ventRateCalculator.getRate();
    }
}
