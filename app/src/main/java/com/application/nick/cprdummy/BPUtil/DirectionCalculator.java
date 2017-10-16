package com.application.nick.cprdummy.BPUtil;

import java.util.ArrayList;

/**
 * Created by Nick on 8/11/2017.
 */

public abstract class DirectionCalculator {
    private static final long TIME_DELAY = 200; //ms
    private static final int DEPTH_DIFFERENCE = 5;
    //private static final float DEPTH_MOVING_BOUNDARY = 0.002f; //if slope is greater than positive val then increasing. less than negative then decreasing. else straight

    private ArrayList<DataPoint> dataPoints;

    private BPManager.DEPTH_DIRECTION previousDirection;

    private boolean compressionStarted;

    public DirectionCalculator(ArrayList<DataPoint> dataPoints) {
        this.dataPoints = dataPoints;

        compressionStarted = false;
    }

    /**
     * call this after making changes to dataPoints list
     */
    public BPManager.DEPTH_DIRECTION update(DataPoint latestDataPoint) {
        BPManager.DEPTH_DIRECTION direction = getDepthDirection(); //get direction of motion of latest points
        latestDataPoint.direction = direction;

        handleDirectionChanges(direction);
        return direction;
    }

    public BPManager.DEPTH_DIRECTION getDepthDirection() {
        if(previousDirection == null) {
            return BPManager.DEPTH_DIRECTION.INCREASING; //must be increasing on start
        }

        int currentLogSize = dataPoints.size();

        //if there was a big jump in depth in either direction we can assume increasing or decreasing
        if(dataPoints.get(currentLogSize - 1).depth - dataPoints.get(currentLogSize - 2).depth > DEPTH_DIFFERENCE) {
            return BPManager.DEPTH_DIRECTION.INCREASING;
        } else if (dataPoints.get(currentLogSize - 2).depth - dataPoints.get(currentLogSize - 1).depth > DEPTH_DIFFERENCE) {
            return BPManager.DEPTH_DIRECTION.DECREASING;
        }
        //finally, for other cases we check slope
        float slope = (dataPoints.get(currentLogSize - 1).depth - dataPoints.get(0).depth) / (float)(dataPoints.get(currentLogSize - 1).time - dataPoints.get(0).time); //rise/run
        if(slope < 0) {
            return BPManager.DEPTH_DIRECTION.DECREASING;
        } else if(slope > 0) {
            return BPManager.DEPTH_DIRECTION.INCREASING;
        } else {
            return BPManager.DEPTH_DIRECTION.STRAIGHT;
        }
    }

    private void handleDirectionChanges(BPManager.DEPTH_DIRECTION direction) {
        if( !compressionStarted && direction == BPManager.DEPTH_DIRECTION.INCREASING && (previousDirection == null || previousDirection == BPManager.DEPTH_DIRECTION.DECREASING || previousDirection == BPManager.DEPTH_DIRECTION.STRAIGHT )) {
            // since the logsize is relatively large, we might not register the compression start until
            // a few values after it has happened. We want to make sure we are getting the true
            // compression start values, which will be at the minimum logged depth
            int minDepthIndex = getMinDepthLogIndex();
            //set all the datapoints in the log after min depth to increasing

            for(int i = minDepthIndex + 1; i < dataPoints.size(); i++) {
                dataPoints.get(i).direction = BPManager.DEPTH_DIRECTION.INCREASING;
            }

            handleCompressionStart(dataPoints.get(minDepthIndex).depth, dataPoints.get(minDepthIndex).time);
            compressionStarted = true;
        } else if ( compressionStarted && (direction == BPManager.DEPTH_DIRECTION.STRAIGHT || direction == BPManager.DEPTH_DIRECTION.INCREASING)  && (previousDirection == BPManager.DEPTH_DIRECTION.DECREASING)) {
            //same thing for compression end, we want to make sure we get the true end values
            int minDepthIndex = getMinDepthLogIndex();

            //set all the datapoints in the log before and including min depth to decreasing
            for(int i = 0; i <= minDepthIndex; i++) {
                dataPoints.get(i).direction = BPManager.DEPTH_DIRECTION.DECREASING;
            }

            handleCompressionEnd(dataPoints.get(minDepthIndex).depth, dataPoints.get(minDepthIndex).time);
            compressionStarted = false;
            if(direction == BPManager.DEPTH_DIRECTION.INCREASING) {
                //might now be increasing again already after previous compression ends
                handleCompressionStart(dataPoints.get(minDepthIndex).depth, dataPoints.get(minDepthIndex).time);
                compressionStarted = true;
            }
        } else if (compressionStarted && previousDirection == BPManager.DEPTH_DIRECTION.INCREASING && (direction == BPManager.DEPTH_DIRECTION.STRAIGHT || direction == BPManager.DEPTH_DIRECTION.DECREASING)) {
            //same for compression peak, we want to make sure we get the true peak values
            int maxDepthIndex = getMaxDepthLogIndex();

            //set all the datapoints in the log before and including max depth to increasing
            for(int i = 0; i <= maxDepthIndex; i++) {
                dataPoints.get(i).direction = BPManager.DEPTH_DIRECTION.INCREASING;
            }

            handleCompressionPeak(dataPoints.get(maxDepthIndex).depth, dataPoints.get(maxDepthIndex).time);
        }
        previousDirection = direction;
    }

    /**
     * @return the index of the max depth value in the depth log
     */
    private int getMaxDepthLogIndex() {
        int maxDepth = 0;
        int maxIndex = 0;
        for(int i = 0; i < dataPoints.size(); i++) {
            if(dataPoints.get(i).depth >= maxDepth) {
                maxDepth = dataPoints.get(i).depth;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    /**
     * @return the index of the min depth value in the depth log
     */
    private int getMinDepthLogIndex() {
        int minDepth = 1000;
        int minIndex = 0;
        for(int i = 0; i < dataPoints.size(); i++) {
            if(dataPoints.get(i).depth <= minDepth) {
                minDepth = dataPoints.get(i).depth;
                minIndex = i;
            }
        }
        return minIndex;
    }

    public void reset() {
        compressionStarted = false;
    }

    public abstract void handleCompressionStart(int depth, long time);
    public abstract void handleCompressionEnd(int depth, long time);
    public abstract void handleCompressionPeak(int depth, long time);
}
