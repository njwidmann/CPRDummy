package com.application.nick.cprdummy.BPUtil;

/**
 * Created by Nick on 10/9/2017.
 */

public class DataPoint {

    public long time;
    public int depth;
    public int endTitle;
    public int vents;
    public BPManager.DIRECTION depthDirection, ventsDirection;
    public float sbp;
    public float dbp;

    public enum VALUE_TYPE {DEPTH, VENTS}

    public DataPoint(long time, int depth, int vents) {
        this.time = time;
        this.depth = depth;
        this.vents = vents;
    }

    public DataPoint(long time, int depth, DataPoint dataPoint) {
        this.time = time;
        this.depth = depth;
        this.vents = dataPoint.vents;
        this.endTitle = dataPoint.endTitle;
        this.depthDirection = dataPoint.depthDirection;
        this.ventsDirection = dataPoint.ventsDirection;
        this.sbp = dataPoint.sbp;
        this.dbp = dataPoint.dbp;
    }

    public int getValue(VALUE_TYPE valueType) {
        if(valueType == VALUE_TYPE.DEPTH) {
            return depth;
        } else if (valueType == VALUE_TYPE.VENTS) {
            return vents;
        } else {
            return 0;
        }
    }

    public void setDirection(VALUE_TYPE valueType, BPManager.DIRECTION direction) {
        if(valueType == VALUE_TYPE.DEPTH) {
            depthDirection = direction;
        } else if (valueType == VALUE_TYPE.VENTS) {
            ventsDirection = direction;
        }
    }

    public BPManager.DIRECTION getDirection(VALUE_TYPE valueType) {
        if(valueType == VALUE_TYPE.DEPTH) {
            return depthDirection;
        } else if (valueType == VALUE_TYPE.VENTS) {
            return ventsDirection;
        } else {
            return BPManager.DIRECTION.STRAIGHT; //just return straight by default
        }
    }
}
