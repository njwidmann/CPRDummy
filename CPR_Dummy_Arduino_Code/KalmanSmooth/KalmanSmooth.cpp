/*
  KalmanSmooth.cpp
  Library for smoothing real-time data with Kalman Filter
*/

#include "Arduino.h"
#include "KalmanSmooth.h"

KalmanSmooth::KalmanSmooth(float q, float r, float threshold)
{
	Q = q;
	R = r;
	THRESHOLD = threshold;
	smoothed = 0;
	err = 1;
}

float KalmanSmooth::smooth(float measured)
{
	if(abs(measured) < THRESHOLD) {
		measured = 0;
	}

	float gain = err / (err + R);

	smoothed = smoothed + gain * (measured - smoothed);

	err = (1 - gain) * err + Q;
	
	return smoothed;
}
	