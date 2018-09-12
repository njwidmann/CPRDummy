package com.application.nick.cprdummy.BPUtil;

import java.util.ArrayList;

/**
 * Created by Nick on 8/11/2017.
 */

public abstract class DirectionCalculator {

    public enum DIRECTION {
        INCREASING, DECREASING, STRAIGHT
    }

    private ArrayList<DataPoint> dataPoints;

    private DIRECTION previousDirection;

    private boolean cycleStarted;

    private DataPoint.VALUE_TYPE value_type;

    public DirectionCalculator(ArrayList<DataPoint> dataPoints, DataPoint.VALUE_TYPE value_type) {
        this.dataPoints = dataPoints;
        this.value_type = value_type; //either depth or vents (we want to be able to keep track of direction changes for both)

        cycleStarted = false;
    }

    /**
     * call this after making changes to dataPoints list
     */
    public DIRECTION update(DataPoint latestDataPoint) {
        DIRECTION direction = getDirection(); //get direction of motion of latest points
        latestDataPoint.setDirection(value_type, direction);

        handleDirectionChanges(direction);
        return direction;
    }

    public DIRECTION getDirection() {
        if(previousDirection == null) {
            return DIRECTION.STRAIGHT; //must be straight on start
        }

        float slope = findSlope(dataPoints);

        if(slope < 0) {
            return DIRECTION.DECREASING;
        } else if(slope > 0) {
            return DIRECTION.INCREASING;
        } else {
            return DIRECTION.STRAIGHT;
        }
    }

    /**
     * calculates trendline using this method
     * https://classroom.synonym.com/calculate-trendline-2709.html
     */
    private float findSlope(ArrayList<DataPoint> data) {

        int n = data.size();

        float a = 0;
        float b1 = 0;
        float b2 = 0;
        float c = 0;
        for(int i = 0; i < n; i++) {
            long time = data.get(i).time - data.get(0).time; //first time = 0
            a += n * time * data.get(i).getValue(value_type);
            b1 += time;
            b2 += data.get(i).getValue(value_type);
            c += n * time * time;
        }
        float b = b1*b2;
        float d = b1*b1;
        return (a-b)/(c-d);
    }

    private void handleDirectionChanges(DIRECTION direction) {
        if( !cycleStarted && direction == DIRECTION.INCREASING && (previousDirection == null || previousDirection == DIRECTION.DECREASING || previousDirection == DIRECTION.STRAIGHT )) {
            // since the logsize is relatively large, we might not register the cycle start until
            // a few values after it has happened. We want to make sure we are getting the true
            // cycle start value, which will be at the minimum logged value
            int minIndex = getMinLogIndex();

            //set all the datapoints in the log after min to increasing
            for(int i = minIndex + 1; i < dataPoints.size(); i++) {
                dataPoints.get(i).setDirection(value_type, DIRECTION.INCREASING);
            }

            handleCycleStart(dataPoints.get(minIndex).getValue(value_type), dataPoints.get(minIndex).time);
            cycleStarted = true;
        } else if ( cycleStarted && (direction == DIRECTION.STRAIGHT || direction == DIRECTION.INCREASING)  && (previousDirection == DIRECTION.DECREASING)) {
            //same thing for compression end, we want to make sure we get the true end values
            int minIndex = getMinLogIndex();

            //set all the datapoints in the log before and including min to decreasing
            for(int i = 0; i <= minIndex; i++) {
                dataPoints.get(i).setDirection(value_type, DIRECTION.DECREASING);
            }

            handleCycleEnd(dataPoints.get(minIndex).getValue(value_type), dataPoints.get(minIndex).time);
            cycleStarted = false;
            if(direction == DIRECTION.INCREASING) {
                //might now be increasing again already after previous cycle ends
                handleCycleStart(dataPoints.get(minIndex).getValue(value_type), dataPoints.get(minIndex).time);
                cycleStarted = true;
            }
        } else if (cycleStarted && previousDirection == DIRECTION.INCREASING && (direction == DIRECTION.STRAIGHT || direction == DIRECTION.DECREASING)) {
            //same for compression peak, we want to make sure we get the true peak values
            int maxIndex = getMaxLogIndex();

            //set all the datapoints in the log before and including max depth to increasing
            for(int i = 0; i <= maxIndex; i++) {
                dataPoints.get(i).setDirection(value_type, DIRECTION.INCREASING);
            }

            handleCyclePeak(dataPoints.get(maxIndex).getValue(value_type), dataPoints.get(maxIndex).time);
        }
        previousDirection = direction;
    }

    /**
     * @return the index of the max value in the log
     */
    private int getMaxLogIndex() {
        int max = 0;
        int maxIndex = 0;
        for(int i = 0; i < dataPoints.size(); i++) {
            if(dataPoints.get(i).getValue(value_type) >= max) {
                max = dataPoints.get(i).getValue(value_type);
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    /**
     * @return the index of the min value in the log
     */
    private int getMinLogIndex() {
        int min = 1000;
        int minIndex = 0;
        for(int i = 0; i < dataPoints.size(); i++) {
            if(dataPoints.get(i).getValue(value_type) <= min) {
                min = dataPoints.get(i).getValue(value_type);
                minIndex = i;
            }
        }
        return minIndex;
    }

    public void reset() {
        cycleStarted = false;
    }

    public abstract void handleCycleStart(int value, long time);
    public abstract void handleCycleEnd(int value, long time);
    public abstract void handleCyclePeak(int value, long time);

    public static String getDirectionString(DIRECTION direction) {
        switch (direction) {
            case INCREASING: return "INCREASING";
            case DECREASING: return "DECREASING";
            default: return "STRAIGHT";
        }

    }
}
