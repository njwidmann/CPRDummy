package com.application.nick.cprdummy.BPUtil;

import java.util.ArrayList;

/**
 * Created by Nick on 8/11/2017.
 */

public abstract class DirectionCalculator {
    private static final int LOGSIZE = 2;
    private static final long TIME_DELAY = 200; //ms
    private static final int DEPTH_DIFFERENCE = 5;
    private static final float DEPTH_MOVING_BOUNDARY = 0.002f; //if slope is greater than positive val then increasing. less than negative then decreasing. else straight

    private ArrayList<Integer> depthLog;
    private ArrayList<Long> timeLog;

    private BPManager.DEPTH_DIRECTION previousDirection;

    private boolean compressionStarted;

    public DirectionCalculator() {
        depthLog = new ArrayList<>();
        timeLog = new ArrayList<>();
        for(int i = 0; i < LOGSIZE; i++) {
            depthLog.add(0);
            timeLog.add(0L);
        }


        compressionStarted = false;
    }

    public BPManager.DEPTH_DIRECTION update(long time, int depth) {
        timeLog.add(time);
        depthLog.add(depth);
        if(timeLog.size() > LOGSIZE) {
            timeLog.remove(0);
            depthLog.remove(0);
        }
        BPManager.DEPTH_DIRECTION direction = getDepthDirection();

        handleDirectionChanges(depth, time, direction);

        return direction;
    }

    public BPManager.DEPTH_DIRECTION getDepthDirection() {
        if(previousDirection == null) {
            return BPManager.DEPTH_DIRECTION.INCREASING; //must be increasing on start
        }
        //if the difference between the last two changes is large enough, we can assume
        // it was just a small change and still relatively straight
        if(timeLog.get(LOGSIZE - 1) - timeLog.get(LOGSIZE - 2) > TIME_DELAY) {
            return BPManager.DEPTH_DIRECTION.STRAIGHT;
        }
        //if there was a big jump in depth in either direction we can assume increasing or decreasing
        if(depthLog.get(LOGSIZE - 1) - depthLog.get(LOGSIZE - 2) > DEPTH_DIFFERENCE) {
            return BPManager.DEPTH_DIRECTION.INCREASING;
        } else if (depthLog.get(LOGSIZE - 2) - depthLog.get(LOGSIZE - 1) > DEPTH_DIFFERENCE) {
            return BPManager.DEPTH_DIRECTION.DECREASING;
        }
        //finally, for other cases we check slope
        float slope = (depthLog.get(LOGSIZE - 1) - depthLog.get(0)) / (float)(timeLog.get(LOGSIZE - 1) - timeLog.get(0)); //rise/run
        if(slope < -DEPTH_MOVING_BOUNDARY) {
            return BPManager.DEPTH_DIRECTION.DECREASING;
        } else if(slope > DEPTH_MOVING_BOUNDARY) {
            return BPManager.DEPTH_DIRECTION.INCREASING;
        } else {
            return BPManager.DEPTH_DIRECTION.STRAIGHT;
        }
    }

    private void handleDirectionChanges(int depth, long time, BPManager.DEPTH_DIRECTION direction) {
        if( !compressionStarted && direction == BPManager.DEPTH_DIRECTION.INCREASING && (previousDirection == null || previousDirection == BPManager.DEPTH_DIRECTION.DECREASING || previousDirection == BPManager.DEPTH_DIRECTION.STRAIGHT )) {
            handleCompressionStart(depth, time);
            compressionStarted = true;
        } else if ( compressionStarted && (direction == BPManager.DEPTH_DIRECTION.STRAIGHT || direction == BPManager.DEPTH_DIRECTION.INCREASING)  && (previousDirection == BPManager.DEPTH_DIRECTION.DECREASING)) {
            handleCompressionEnd(depth, time);
            compressionStarted = false;
            if(direction == BPManager.DEPTH_DIRECTION.INCREASING) {
                handleCompressionStart(depth, time); //might now be increasing again already after previous compression ends
                compressionStarted = true;
            }
        } else if (compressionStarted && previousDirection == BPManager.DEPTH_DIRECTION.INCREASING && (direction == BPManager.DEPTH_DIRECTION.STRAIGHT || direction == BPManager.DEPTH_DIRECTION.DECREASING)) {
            handleCompressionPeak(depth, time);
        }
        previousDirection = direction;
    }

    public abstract void handleCompressionStart(int depth, long time);
    public abstract void handleCompressionEnd(int depth, long time);
    public abstract void handleCompressionPeak(int depth, long time);
}
