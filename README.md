# CPR Dummy
Source code for our CPR manikin that simulates blood pressure (BP) and end tidal CO2 (ETCO2) waveforms for the purpose of training the titration of CPR mechanics to physiology, as recommended by the American Heart Association.

## [Android Code](app/src/main)
### [Android UI Code](app/src/main/res)
User Interface XML files. These files just describe the physical layout of UI elements on the screen.
* [activity_main.xml](app/src/main/res/layout/activity_main.xml) - This is the main UI of the app. Includes waveform graphs, numerical indicators, and user controls. Waveform graphs are made using [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart).
* [activity_device_list.xml](/app/src/main/res/layout/activity_device_list.xml) - This is the first screen users are shown when using the app. Here they select the bluetooth device for the CPR manikin (e.g. HC-06). Make sure BT device is registered in the android tablet settings first.
* [strings.xml](app/src/main/res/values/strings.xml) - This is where UI strings for the app are defined
* [colors.xml](app/src/main/res/values/colors.xml) - This is where UI colors for the app are defined

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
* [BPMonitor.java](app/src/main/java/com/application/nick/cprdummy/BPMonitor.java) - BPMonitor real-time raw depth/vent data from the [MainActivity](app/src/main/java/com/application/nick/cprdummy/MainActivity.java). Each set of raw values is added to a queue of [DataPoints](app/src/main/java/com/application/nick/cprdummy/BPUtil/DataPoint.java) for processing inside a separate thread, called UpdateThread. UpdateThread uses [BPManager.java](app/src/main/java/com/application/nick/cprdummy/BPUtil/BPManager.java) to calculate BP/ETCO2 for each [DataPoint](app/src/main/java/com/application/nick/cprdummy/BPUtil/DataPoint.java). BPMonitor then updates the UI through a series of abstract methods implemented in [MainActivity](app/src/main/java/com/application/nick/cprdummy/MainActivity.java).
* [BPUtil/MechanicalManager.java](app/src/main/java/com/application/nick/cprdummy/BPUtil/MechanicalManager.java) - MechanicalManager is used to calculate traditional mechanical metrics of CPR from raw depth/vent values, including the following:
  * Average absolute depth
  * Average relative depth
  * Average leaning depth (leaning = absolute - relative)
  * Average compression rate
  * Pauses in compressions
  * Ventilation rate
* [BPUtil/BPManager.java](app/src/main/java/com/application/nick/cprdummy/BPUtil/BPManager.java) - BPManager is a child class of MechanicalManager. It inherits all the same methods used to calculate the mechanical factors listed above but also includes additional code for calculating SBP, DBP, and instantaneous BP throughout a compression, as well as max, min, and instantaneous ETCO2. This is the class that is used in [BPMonitor](app/src/main/java/com/application/nick/cprdummy/BPMonitor.java) to get the mechanical factors and corresponding BP/ETCO2 values to display in the UI for each real-time [DataPoint](app/src/main/java/com/application/nick/cprdummy/BPUtil/DataPoint.java) in the UpdateThread queue.
* [BPUtil/DataPoint.java](app/src/main/java/com/application/nick/cprdummy/BPUtil/DataPoint.java) - DataPoint is a container class for relating all of the calculated mechanical factors and BP/ETCO2 to a set of raw values inputted at a specific time. Used to keep everything in order in the UpdateThread queue in [BPMonitor.java](app/src/main/java/com/application/nick/cprdummy/BPMonitor.java).
* 
## [Arduino Code](CPR_Dummy_Arduino_Code/CPR_Dummy_Arduino_Code.ino)
Arduino records compressions depth and ventilation air pressure and send values via bluetooth to android tablet for processing
