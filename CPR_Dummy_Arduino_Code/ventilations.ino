#include <KalmanSmooth.h>

const int ventilation_pin = A5;

float p0 = 0;
const int initialCalibrationSampleSize = 100;
float Q = 0.001;
float R = 0.1;
KalmanSmooth kalman_filter_p(Q, R, 5);

void setupVentilations() {
  calibrateP0();
}

void calibrateP0() {
  for(int i = 0; i < initialCalibrationSampleSize; i++) {
    p0+=analogRead(ventilation_pin);
  }
  p0 = p0 / (initialCalibrationSampleSize);
}

int readVentilationVelocity() {
  return kalman_filter_p.smooth(analogRead(ventilation_pin) - p0);
}

