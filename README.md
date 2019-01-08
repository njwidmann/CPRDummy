# CPR Dummy
Source code for our CPR manikin that simulates blood pressure and end tidal CO2 waveforms for the purpose of training the titration of CPR mechanics to physiology, as recommended by the American Heart Association.

## [Android Code](app/src/main)
### [Android UI Code](app/src/main/res)
User Interface XML files. These files just describe the physical layout of UI elements on the screen.
* [Main App UI Layout](app/src/main/res/layout/activity_main.xml) - This is the main UI of the app. Includes waveform graphs, numerical indicators, and user controls. Waveform graphs are made using [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart).
* [Bluetooth Device Selection Screen](/app/src/main/res/layout/activity_device_list.xml) - This is the first screen users are shown when using the app. Here they select the bluetooth device for the CPR manikin (e.g. HC-06). Make sure BT device is registered in the android tablet settings first.
* [Strings Definition File](app/src/main/res/values/strings.xml) - This is where UI strings for the app are defined
* [Colors Definition File](app/src/main/res/values/colors.xml) - This is where UI colors for the app are defined

### [Backend Android Code](src/main/java/com/application/nick/cprdummy)

* [DeviceListActivity.java](app/src/main/java/com/application/nick/cprdummy/DeviceListActivity.java) - Backend code for bluetooth device selection screen. 
* [MainActivity.java](app/src/main/java/com/application/nick/cprdummy/MainActivity.java) - MainActivity does the following:
  * Reads raw bluetooth input (depth and vent pressure) from manikin. 
  * Sends raw inputs to [BPMonitor.java](app/src/main/java/com/application/nick/cprdummy/BPMonitor.java) to calculate BP and ETCO2 from CPR mechanics. These calculated values are later sent back to MainActivity. 
  * Sets real-time values for UI elements (graphical waveforms and numerical indicators). 
  * Allows selection of training level:
    * Beginner - Shows all numerical and graphical indicators. Designed to get clinicians aquainted with the relationships between compression/ventilation depth/rate and BP/ETCO2.
    * Intermediate - Hides numerical and graphical indicators for compression/ventilation depth/rate. After the trainee has become comfortable with Beginner mode, they should move on to Intermediate where they will be forced to use BP/ETCO2 to guage their performance.
    * Expert - (TODO!) Starts to randomly slightly modify the relationships between mechanics and BP/ETCO2 to more accurately simulate different patient CPR requirements. Users at this point should understand that they may have to modify their CPR technique to achieve ideal BP/ETCO2 values
    * Damping Test - This is an experimental setting for a prototype manikin that simulates damping on recoil using a solenoid coil surrounding the manikin's spring. Arduino code for this manikin can be found [here](https://github.com/njwidmann/MoldingManikin).

## [Arduino Code](CPR_Dummy_Arduino_Code/CPR_Dummy_Arduino_Code.ino)
Arduino records compressions depth and ventilation air pressure and send values via bluetooth to android tablet for processing
