package com.application.nick.cprdummy.BPUtil;

/**
 * Created by Nick on 10/9/2017.
 */

public class DataPoint {

    public long time;
    public int depth;
    public int endTitle;
    public BPManager.DEPTH_DIRECTION direction;
    public float sbp;
    public float dbp;

    public DataPoint(long time, int depth) {
        this.time = time;
        this.depth = depth;
    }

    public DataPoint(long time, int depth, DataPoint dataPoint) {
        this.time = time;
        this.depth = depth;
        this.endTitle = dataPoint.endTitle;
        this.direction = dataPoint.direction;
        this.sbp = dataPoint.sbp;
        this.dbp = dataPoint.dbp;
    }
}
