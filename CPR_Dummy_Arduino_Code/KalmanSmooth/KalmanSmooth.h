/*
  KalmanSmooth.h
  Library for smoothing real-time data with Kalman Filter
*/
#ifndef KalmanSmooth_h
#define KalmanSmooth_h

#include "Arduino.h"

class KalmanSmooth
{
  public:
    KalmanSmooth(float q, float r, float threshold);
    float smooth(float measured);
  private:
	float smoothed;
	float err;
	float Q;
	float R;
	float THRESHOLD;
};

#endif