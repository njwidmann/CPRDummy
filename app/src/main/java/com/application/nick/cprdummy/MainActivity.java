package com.application.nick.cprdummy;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class MainActivity extends Activity {

    Handler bluetoothIn;

    final int handlerState = 0;        				 //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final String TAG = "MainActivity";

    private static final int DEFAULT_VISIBLE_TIME_RANGE = 5;
    private static final int MINIMUM_VISIBLE_TIME_RANGE = 5;
    private static final int MAXIMUM_VISIBLE_TIME_RANGE = 60;
    private static final int TIME_PADDING = 2;
    private static final int DEFAULT_MAXIMUM_Y_BP = 100;
    private static final int MAXIMUM_Y_END_TITLE = 50;
    private static final int MAXIMUM_Y_DEPTH = 55;
    private static final int MAXIMUM_Y_VENTS = 100;

    private static final int SBP_TARGET = 85;
    private static final int DBP_TARGET = 30;
    private static final int DAMPING_DEFAULT = 25;

    private static final float DEPTH_SCALE_FACTOR = (float)50.0/75; //each depth count is not exactly 1mm

    // String for MAC address
    private static String address;

    LineChart chartBP, chartEndTitle;
    BarChart chartDepth, chartVents;
    XAxis xAxisBP, xAxisEndTitle, xAxisDepth, xAxisVents;
    YAxis yAxisBP, yAxisEndTitle, yAxisDepth, yAxisVents;
    List<Entry> entriesBP, entriesEndTitle, entriesDBPTarget, entriesSBPTarget;
    List<BarEntry> entriesDepth, entriesVents;

    int maxYBP = DEFAULT_MAXIMUM_Y_BP;

    SeekBar slider;

    private float time = 0;
    private int visibleTimeRange;

    private boolean initialized;

    private BPMonitor bpMonitor;

    private TextView sbpdbpTextField, bpmTextField, avgDepthTextField, avgLeaningDepthTextField, co2TextField, dampingTextField, dampingLabel;

    private int negativeDepthOffset = 0;

    private int damping = DAMPING_DEFAULT;
    //private long netTime = 0;

    public enum TrainingLevel {
        BEGINNER,
        INTERMEDIATE,
        EXPERT,
        DAMPING_TEST
    }

    private TrainingLevel currentTrainingLevel = TrainingLevel.BEGINNER;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        visibleTimeRange = DEFAULT_VISIBLE_TIME_RANGE;

        //init line charts
        chartBP = (LineChart) findViewById(R.id.chartBP);
        chartEndTitle = (LineChart) findViewById(R.id.chartEndTitle);

        //init bar chart for depth
        chartDepth = (BarChart) findViewById(R.id.chartDepth);

        //init bar chart for vents
        chartVents = (BarChart) findViewById(R.id.chartVents);

        //create data lists
        entriesDepth = new ArrayList<>();
        entriesVents = new ArrayList<>();
        entriesBP = new ArrayList<>();
        entriesDBPTarget = new ArrayList<>();
        entriesSBPTarget = new ArrayList<>();
        entriesEndTitle = new ArrayList<>();

        //create starting point
        entriesDepth.add(new BarEntry(0,0));
        entriesVents.add(new BarEntry(0,0));
        entriesBP.add(new Entry(0, 0));
        entriesEndTitle.add(new Entry(0, 0));

        entriesDBPTarget.add(new Entry(0, DBP_TARGET));
        entriesDBPTarget.add(new Entry(100000, DBP_TARGET));
        entriesSBPTarget.add(new Entry(0, SBP_TARGET));
        entriesSBPTarget.add(new Entry(100000, SBP_TARGET));

        //create datasets
        //LineDataSet depthDataSet = new LineDataSet(entriesDepth, getString(R.string.depth_label));
        LineDataSet dataSetBP = new LineDataSet(entriesBP, getString(R.string.pressure_label));
        LineDataSet dataSetDBPTarget = new LineDataSet(entriesDBPTarget, getString(R.string.pressure_label));
        LineDataSet dataSetSBPTarget = new LineDataSet(entriesSBPTarget, getString(R.string.pressure_label));
        LineDataSet dataSetEndTitle = new LineDataSet(entriesEndTitle, getString(R.string.end_title_label));
        BarDataSet dataSetDepth = new BarDataSet(entriesDepth, getString(R.string.chart_description_depth));
        BarDataSet dataSetVents = new BarDataSet(entriesVents, getString(R.string.chart_description_vents));

        //format datasets
        dataSetDepth.setAxisDependency(YAxis.AxisDependency.LEFT); //use left axis
        dataSetDepth.setDrawValues(false); //no text labels for data points
        dataSetDepth.setColor(Color.GREEN); //bar is green

        dataSetVents.setAxisDependency(YAxis.AxisDependency.LEFT); //use left axis
        dataSetVents.setDrawValues(false); //no text labels for data points
        dataSetVents.setColor(Color.CYAN); //bar is green

        dataSetBP.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSetBP.setDrawValues(false);
        dataSetBP.setColor(Color.RED);
        dataSetBP.setLineWidth(5);
        dataSetBP.setDrawCircles(false);

        dataSetDBPTarget.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSetDBPTarget.setDrawValues(false);
        dataSetDBPTarget.setColor(Color.WHITE);
        dataSetDBPTarget.setLineWidth(5);
        dataSetDBPTarget.setDrawCircles(false);
        dataSetDBPTarget.enableDashedLine(15,15,0);

        dataSetSBPTarget.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSetSBPTarget.setDrawValues(false);
        dataSetSBPTarget.setColor(Color.WHITE);
        dataSetSBPTarget.setLineWidth(5);
        dataSetSBPTarget.setDrawCircles(false);
        dataSetSBPTarget.enableDashedLine(15,15,0);

        dataSetEndTitle.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSetEndTitle.setDrawValues(false);
        dataSetEndTitle.setColor(Color.CYAN);
        dataSetEndTitle.setLineWidth(5);
        dataSetEndTitle.setDrawCircles(false);

        // use the interface ILineDataSet for list of datasets
        List<ILineDataSet> dataSetsBP = new ArrayList<ILineDataSet>();
        dataSetsBP.add(dataSetBP);
        dataSetsBP.add(dataSetDBPTarget);
        dataSetsBP.add(dataSetSBPTarget);

        //attach datasets to chartBP
        LineData dataBP = new LineData(dataSetsBP);
        chartBP.setData(dataBP);

        LineData dataEndTitle = new LineData(dataSetEndTitle);
        chartEndTitle.setData(dataEndTitle);

        BarData dataDepth = new BarData(dataSetDepth);
        chartDepth.setData(dataDepth);

        BarData dataVents = new BarData(dataSetVents);
        chartVents.setData(dataVents);

        //format axes
        xAxisBP = chartBP.getXAxis();
        xAxisBP.setDrawLabels(false);
        yAxisBP = chartBP.getAxisLeft(); //y axis is on the left
        yAxisBP.setTextColor(Color.WHITE);
        chartBP.getAxisRight().setEnabled(false); //no right side y axis

        xAxisEndTitle = chartEndTitle.getXAxis();
        xAxisEndTitle.setDrawLabels(false);
        yAxisEndTitle = chartEndTitle.getAxisLeft(); //y axis is on the left
        yAxisEndTitle.setTextColor(Color.WHITE);
        yAxisEndTitle.setLabelCount(3);
        chartEndTitle.getAxisRight().setEnabled(false); //no right side y axis

        xAxisDepth = chartDepth.getXAxis();
        xAxisDepth.setDrawLabels(false);
        yAxisDepth = chartDepth.getAxisLeft(); //y axis is on the left
        yAxisDepth.setTextColor(Color.WHITE);
        yAxisDepth.setLabelCount(5);
        chartDepth.getAxisRight().setEnabled(false); //no right side y axis

        xAxisVents = chartVents.getXAxis();
        xAxisVents.setDrawLabels(false);
        yAxisVents = chartVents.getAxisLeft(); //y axis is on the left
        yAxisVents.setTextColor(Color.WHITE);
        yAxisVents.setLabelCount(5);
        chartVents.getAxisRight().setEnabled(false); //no right side y axis

        //format chart description and legend
        Description chartDescriptionBP = new Description();
        chartDescriptionBP.setText(getString(R.string.chart_description_BP));
        chartDescriptionBP.setTextColor(Color.WHITE);
        chartBP.setDescription(chartDescriptionBP);
        chartBP.getLegend().setEnabled(false);
        chartBP.setTouchEnabled(false);

        Description chartDescriptionEndTitle = new Description();
        chartDescriptionEndTitle.setText(getString(R.string.chart_description_end_title));
        chartDescriptionEndTitle.setTextColor(Color.WHITE);
        chartEndTitle.setDescription(chartDescriptionEndTitle);
        chartEndTitle.getLegend().setEnabled(false);
        chartEndTitle.setTouchEnabled(false);

        Description chartDescriptionDepth = new Description();
        chartDescriptionDepth.setText(getString(R.string.chart_description_depth));
        chartDescriptionDepth.setTextColor(Color.WHITE);
        chartDepth.setDescription(chartDescriptionDepth);
        chartDepth.getLegend().setEnabled(false);
        chartDepth.setTouchEnabled(false);

        Description chartDescriptionVents = new Description();
        chartDescriptionVents.setText(getString(R.string.chart_description_vents));
        chartDescriptionVents.setTextColor(Color.WHITE);
        chartVents.setDescription(chartDescriptionVents);
        chartVents.getLegend().setEnabled(false);
        chartVents.setTouchEnabled(false);

        //default axis boundaries
        xAxisBP.setAxisMinimum(0);
        xAxisBP.setAxisMaximum(visibleTimeRange);
        yAxisBP.setAxisMinimum(0);
        yAxisBP.setAxisMaximum(DEFAULT_MAXIMUM_Y_BP);

        xAxisEndTitle.setAxisMinimum(0);
        xAxisEndTitle.setAxisMaximum(visibleTimeRange);
        yAxisEndTitle.setAxisMinimum(0);
        yAxisEndTitle.setAxisMaximum(MAXIMUM_Y_END_TITLE);

        yAxisDepth.setAxisMaximum(0);
        yAxisDepth.setAxisMinimum(-MAXIMUM_Y_DEPTH);

        yAxisVents.setAxisMaximum(MAXIMUM_Y_VENTS);
        yAxisVents.setAxisMinimum(0);

        //button to refresh chart and clear all data
        final Button refreshButton = (Button)findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                refreshChart();
            }
        });

        final Spinner trainingLevelDropdown = (Spinner) findViewById(R.id.training_level_dropdown);
        ArrayAdapter<CharSequence> trainingLevelDropdownAdapter = ArrayAdapter.createFromResource(this, R.array.training_levels, android.R.layout.simple_spinner_item);
        trainingLevelDropdownAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        trainingLevelDropdown.setAdapter(trainingLevelDropdownAdapter);
        trainingLevelDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setTrainingLevel(TrainingLevel.valueOf((String)parent.getItemAtPosition(position)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });

        //slider to control visible x range
        slider = (SeekBar)findViewById(R.id.slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean changeFromUser) {
                if(currentTrainingLevel == TrainingLevel.DAMPING_TEST) {
                    mConnectedThread.write(value);
                    damping = value;
                } else {
                    value += MINIMUM_VISIBLE_TIME_RANGE; //slider values go from 0 to [(max time range) - (min time range)]
                    visibleTimeRange = value;
                    reformatAxis(time, 0);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        slider.setProgress(DEFAULT_VISIBLE_TIME_RANGE - MINIMUM_VISIBLE_TIME_RANGE);

        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) { //if message is what we want
                    String readMessage = (String) msg.obj; // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage); //keep appending to string until "~"
                    if(recDataString.length() > 100) {
                        recDataString.delete(0, recDataString.length());
                    }

                    Log.i(TAG, recDataString.toString());

                    int startOfLineIndex = recDataString.indexOf("#"); //determine start-of-line
                    if (startOfLineIndex >= 0) { // make sure there is a '#'
                        if(startOfLineIndex > 0) { //if the '#' isn't the first character
                            recDataString.delete(0,startOfLineIndex); //delete extra text before the '#'
                            startOfLineIndex = 0; //'#' is now first char
                        }

                        int endOfLineIndex = recDataString.indexOf("~"); // determine the end-of-line
                        if(endOfLineIndex > 0) { // make sure there is data before ~
                            String dataInPrint = recDataString.substring(startOfLineIndex, endOfLineIndex); // extract string

                            processBTMessage(dataInPrint);

                            recDataString.delete(0, endOfLineIndex); //clear all string data

                        }
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        bpMonitor = new BPMonitor() {
            @Override
            public void plotDepth(int depth) {
                entriesDepth.get(0).setY(-depth);
            }

            @Override
            public void plotVents(int vents) {
                entriesVents.get(0).setY(vents);
            }

            @Override
            public void plotPressure(float time, float pressure) {
                entriesBP.add(new Entry(time, pressure));

            }

            @Override
            public void plotEndTitle(float time, float endTitle) {
                entriesEndTitle.add(new Entry(time, endTitle));
            }

            @Override
            public void plotAll(float time, int depth, int vents, float pressure, final float endTitle) {
                plotDepth(depth);
                plotVents(vents);
                plotPressure(time, pressure);
                plotEndTitle(time, endTitle);

                deleteOldDataPoints(); //delete data points earlier than max visible time range
                reformatAxis(time, pressure); //adjust axes if necessary to include new points

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        updateAvgDepthIndicator();
                        updateBPMIndicator();
                        updateAvgLeaningDepthIndicator();
                        updateSBPDBPIndicator();
                        updateCO2Indicator();
                        updateDampingIndicator(damping);
                    }
                });
            }
        };

        sbpdbpTextField = (TextView) findViewById(R.id.sbpdbp_textfield);
        bpmTextField = (TextView) findViewById(R.id.bpm_textfield);
        avgDepthTextField = (TextView) findViewById(R.id.avgdepth_textfield);
        avgLeaningDepthTextField = (TextView) findViewById(R.id.avgleaningdepth_textfield);
        co2TextField = (TextView) findViewById(R.id.c02_textfield);
        dampingTextField = (TextView) findViewById(R.id.damping_textfield);
        dampingLabel = (TextView) findViewById(R.id.damping_label);
        dampingTextField.setVisibility(View.INVISIBLE);
        dampingLabel.setVisibility(View.INVISIBLE);

    }

    public void setTrainingLevel(final TrainingLevel level) {
        this.currentTrainingLevel = level;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (level == TrainingLevel.BEGINNER || level == TrainingLevel.DAMPING_TEST) {
                    GridLayout mechanicsGridLayout = (GridLayout) findViewById(R.id.mechanics_gridlayout) ;
                    //set column weight of mechanics grid layout to 25% of the screen
                    GridLayout.LayoutParams params = (GridLayout.LayoutParams) mechanicsGridLayout.getLayoutParams();
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 25f);
                    mechanicsGridLayout.setLayoutParams(params);
                } else {
                    //set column weight of mechanics grid layout to 0 (hide it)
                    GridLayout mechanicsGridLayout = (GridLayout) findViewById(R.id.mechanics_gridlayout) ;
                    GridLayout.LayoutParams params = (GridLayout.LayoutParams) mechanicsGridLayout.getLayoutParams();
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 0f);
                    mechanicsGridLayout.setLayoutParams(params);
                }
                if(level == TrainingLevel.DAMPING_TEST) {
                    slider.setProgress(damping);
                    TextView sliderLabel = (TextView) findViewById(R.id.slider_label);
                    sliderLabel.setText(getString(R.string.damping));
                    dampingTextField.setVisibility(View.VISIBLE);
                    dampingLabel.setVisibility(View.VISIBLE);
                } else {
                    slider.setProgress(visibleTimeRange - MINIMUM_VISIBLE_TIME_RANGE);
                    TextView sliderLabel = (TextView) findViewById(R.id.slider_label);
                    sliderLabel.setText(getString(R.string.x_scale_slider_label));
                    dampingTextField.setVisibility(View.INVISIBLE);
                    dampingLabel.setVisibility(View.INVISIBLE);
                }
            }
        });

    }

    public void updateSBPDBPIndicator() {
        sbpdbpTextField.setText((int)bpMonitor.getSBP() + "/" + (int)bpMonitor.getDBP());
    }

    public void updateBPMIndicator() {
        bpmTextField.setText(String.valueOf((int) bpMonitor.getBPM()));
    }

    public void updateCO2Indicator() {
        co2TextField.setText(String.valueOf(bpMonitor.getEndTitle()));
    }

    public void updateAvgDepthIndicator() {
        avgDepthTextField.setText(String.valueOf((int) bpMonitor.getAvgDepth()));
    }

    public void updateAvgLeaningDepthIndicator() {
        avgLeaningDepthTextField.setText(String.valueOf((int) bpMonitor.getAvgLeaningDepth()));
    }

    public void updateDampingIndicator(int damping) {
        dampingTextField.setText(String.valueOf(damping));
    }
    /**
     * clear all data and refresh axes and pressure
     */
    private void refreshChart() {

        bpMonitor.refresh();

        entriesDepth.clear();
        entriesVents.clear();
        entriesBP.clear();
        entriesEndTitle.clear();

        bpmTextField.setText(String.valueOf(0));
        avgDepthTextField.setText(String.valueOf(0));
        avgLeaningDepthTextField.setText(String.valueOf(0));
        dampingTextField.setText(String.valueOf(0));

        //create starting points
        entriesDepth.add(new BarEntry(0, 0));
        entriesVents.add(new BarEntry(0, 0));
        entriesBP.add(new Entry(0, 0));
        entriesEndTitle.add(new Entry(0, 0));

        maxYBP = 0;

        reformatAxis(0,DEFAULT_MAXIMUM_Y_BP); //reset axis

        initialized = true; //start collecting new data points

        //slider.setProgress(DEFAULT_VISIBLE_TIME_RANGE - MINIMUM_VISIBLE_TIME_RANGE); //uncomment to reset x scale & slider

    }

    /**
     * parse a bluetooth message string to extract time, depth, and pressure data
     * @param msg the bluetooth message string of the form #TIME:DEPTH,PRESSURE
     */
    private void processBTMessage(String msg) {
        //extract time and depth from string bluetooth message
        int timeEnd = msg.indexOf(":");
        int depthEnd = msg.indexOf(",");

        String timeStr = msg.substring(1, timeEnd);
        String depthStr = msg.substring(timeEnd + 1, depthEnd);
        String ventStr = msg.substring(depthEnd+1);

        long elapsedTime = Long.valueOf(timeStr);
        //this.netTime = elapsedTime;
        int depth = Integer.valueOf(depthStr);
        int vents = Integer.valueOf(ventStr);
        //this.damping = Integer.valueOf(dampingStr);

        if(depth * -1 > negativeDepthOffset) {
            negativeDepthOffset = depth * -1;
        }

        depth += negativeDepthOffset;
        depth = (int)(depth * DEPTH_SCALE_FACTOR);
        depth -= 5;


        //Log.i(TAG, msg);//time + ": " + depth + ", " + damping);

        bpMonitor.update(elapsedTime, depth, vents);

    }

    /**
     * discard data points that can no longer be displayed because they happened greater than MAXIMUM_VISIBLE_TIME_RANGE
     * seconds ago
     */
    private void deleteOldDataPoints() {
        while(entriesBP.get(0).getX() < (time - MAXIMUM_VISIBLE_TIME_RANGE)) {
            entriesBP.remove(0);
            entriesEndTitle.remove(0);
        }
    }

    /**
     * adjust the x axis to fit the data. If elapsed time is greater than visibleTimeRange, slide
     * view to fit the last (visibleTimeRange - TIME_PADDING) seconds worth of data. For example, if
     * visibleTimeRange = 10seconds and TIME_PADDING = 2seconds. The x axis range will be from time-8
     * to time+2.
     * @param time last logged time
     * @param value the depth or pressure value to fit inside the graph
     */
    private void reformatAxis(float time, float value) {
        if(time > visibleTimeRange - TIME_PADDING) {
            xAxisBP.setAxisMaximum(time + TIME_PADDING);
            xAxisBP.setAxisMinimum(time + TIME_PADDING - visibleTimeRange);
            xAxisEndTitle.setAxisMaximum(time + TIME_PADDING);
            xAxisEndTitle.setAxisMinimum(time + TIME_PADDING - visibleTimeRange);
        } else {
            xAxisBP.setAxisMaximum(visibleTimeRange);
            xAxisBP.setAxisMinimum(0);
            xAxisEndTitle.setAxisMaximum(visibleTimeRange);
            xAxisEndTitle.setAxisMinimum(0);
        }

        if((int)(value * 1.1) > maxYBP) {
            maxYBP = (int)(value * 1.1);
            yAxisBP.setAxisMaximum(maxYBP);
        }

        chartBP.fitScreen(); //set visible boundaries to max
        chartEndTitle.fitScreen(); //set visible boundaries to max
        chartDepth.fitScreen();
        chartVents.fitScreen();
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //Send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        //mConnectedThread.write("x");

        refreshChart(); //refresh chartBP (set x axis to 0)


    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
            initialized = false; //stop collecting data points
            bpMonitor.release();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }


    /**
     * Checks that the Android device Bluetooth is available and prompts to be turned on if off
     */
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);        	//read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    //Log.i("MainActivity", readMessage);
                    // Send the obtained bytes to the UI Activity via handler
                    if(initialized) { //wait for refresh to be called to initial x axis before collecting data points
                        bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        //write method
        public void write(int input) {
            //byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(input);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
}
