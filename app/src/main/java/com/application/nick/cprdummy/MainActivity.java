package com.application.nick.cprdummy;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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

    private static final int DEFAULT_VISIBLE_TIME_RANGE = 10;
    private static final int MINIMUM_VISIBLE_TIME_RANGE = 5;
    private static final int TIME_PADDING = 2;
    private static final int DEFAULT_Y = 5;

    // String for MAC address
    private static String address;

    LineChart chart;
    XAxis xAxis;
    YAxis yAxis;
    List<Entry> depthEntries, pressureEntries;

    SeekBar slider;

    private float maxY = 0, time = 0;
    private int visibleTimeRange;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        visibleTimeRange = DEFAULT_VISIBLE_TIME_RANGE;

        //init chart
        chart = (LineChart) findViewById(R.id.chart);

        //create data lists
        depthEntries = new ArrayList<>();
        pressureEntries = new ArrayList<>();

        //create starting point
        depthEntries.add(new Entry(0, 0));
        pressureEntries.add(new Entry(0, 0));

        //create datasets
        LineDataSet depthDataSet = new LineDataSet(depthEntries, getString(R.string.depth_label));
        LineDataSet pressureDataSet = new LineDataSet(pressureEntries, getString(R.string.pressure_label));

        //format datasets
        depthDataSet.setAxisDependency(YAxis.AxisDependency.LEFT); //use left axis
        depthDataSet.setDrawValues(false); //no text labels for data points
        depthDataSet.setColor(Color.GREEN); //line is green
        depthDataSet.setLineWidth(5);
        depthDataSet.setDrawCircles(false); //don't show data points (just lines)
        pressureDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        pressureDataSet.setDrawValues(false);
        pressureDataSet.setColor(Color.RED);
        pressureDataSet.setLineWidth(5);
        pressureDataSet.setDrawCircles(false);

        // use the interface ILineDataSet for list of datasets
        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(depthDataSet);
        dataSets.add(pressureDataSet);

        //attach datasets to chart
        LineData data = new LineData(dataSets);
        chart.setData(data);

        //format axes
        xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); //put x axis on the bottom of the chart
        xAxis.setTextColor(Color.WHITE);
        yAxis = chart.getAxisLeft(); //y axis is on the left
        yAxis.setTextColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false); //no right side y axis

        //format chart description and legend
        Description chartDescrition = new Description();
        chartDescrition.setText(getString(R.string.chart_description));
        chartDescrition.setTextColor(Color.WHITE);
        chart.setDescription(chartDescrition);
        chart.getLegend().setTextColor(Color.WHITE);

        //default axis boundaries
        xAxis.setAxisMinimum(0);
        xAxis.setAxisMaximum(visibleTimeRange);
        yAxis.setAxisMinimum(0);

        //set y axis bounds with default
        reformatAxis(DEFAULT_Y,0);

        //don't let user pinch to scale chart. This would get weird with real time data
        chart.setScaleEnabled(false);

        /*
        //button and text input for test points
        final Button addButton = (Button)findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText editText = (EditText)findViewById(R.id.edit_text);
                String text = editText.getText().toString();
                processBTMessage(text);
            }
        });
        */

        //button to refresh chart and clear all data
        final Button refreshButton = (Button)findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                refreshChart();
            }
        });

        //slider to control visible x range
        slider = (SeekBar)findViewById(R.id.slider);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean changeFromUser) {
                value += MINIMUM_VISIBLE_TIME_RANGE; //slider values go from 0 to [(max time range) - (min time range)]
                visibleTimeRange = value;
                reformatAxis(time);
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
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) { //if message is what we want
                    String readMessage = (String) msg.obj; // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage); //keep appending to string until "~"

                    int endOfLineIndex = recDataString.indexOf("~"); // determine the end-of-line
                    if (endOfLineIndex > 0) { // make sure there data before ~
                        String dataInPrint = recDataString.substring(0, endOfLineIndex); // extract string
                        Log.i("MainActivity", dataInPrint);
                        //int dataLength = dataInPrint.length(); //get length of data received

                        if (recDataString.charAt(0) == '#')	//if it starts with # we know it is what we are looking for
                        {
                            processBTMessage(recDataString.toString());
                        }
                        recDataString.delete(0, recDataString.length()); //clear all string data

                        //dataInPrint = " ";
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        //add test data
        String[] testData=readTestData().split("~");
        for(String dataPoint : testData) {
            processBTMessage(dataPoint);
        }

    }

    /**
     * read text from file res/raw/testdata.txt
     * @return String of all text from file
     */
    private String readTestData(){
        InputStream stream = getResources().openRawResource(R.raw.testdata);

        Writer writer = new StringWriter();
        char[] buffer = new char[10240];
        try {
            Reader reader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return writer.toString();
    }

    /**
     * clear all data and refresh Y axis. x axis stays the same because time is still counting up
     */
    private void refreshChart() {
        depthEntries.clear();
        pressureEntries.clear();
        maxY = 0;

        //create starting point
        depthEntries.add(new Entry(0, 0));
        pressureEntries.add(new Entry(0, 0));

        //visibleTimeRange = DEFAULT_VISIBLE_TIME_RANGE;
        //xAxis.setAxisMinimum(0);
        //xAxis.setAxisMaximum(visibleTimeRange);
        reformatAxis(DEFAULT_Y,time);

        //slider.setProgress(DEFAULT_VISIBLE_TIME_RANGE - MINIMUM_VISIBLE_TIME_RANGE);
    }

    /**
     * parse a bluetooth message string to extract time, depth, and pressure data
     * @param msg the bluetooth message string of the form #TIME:DEPTH,PRESSURE
     */
    private void processBTMessage(String msg) {
        int timeEnd = msg.indexOf(":");
        int depthEnd = msg.indexOf(",");
        String timeStr = msg.substring(1, timeEnd);
        String depthStr = msg.substring(timeEnd + 1, depthEnd);
        String pressureStr = msg.substring(depthEnd + 1);

        float time = Long.valueOf(timeStr) / 1000f;
        int depth = Integer.valueOf(depthStr);
        float pressure = Float.valueOf(pressureStr);

        depthEntries.add(new Entry(time, depth));
        pressureEntries.add(new Entry(time, pressure));

        this.time = time; //update global time variable

        if(depth > maxY || pressure > maxY) { //if there is a new max y value
            maxY = depth > pressure ? depth : pressure; //make the greater of pressure and depth maxY
            reformatAxis(maxY, time); //reformat x and y axis
        } else {
            reformatAxis(time); //just reformat x axis
        }
    }

    /**
     * adjust the x axis to fit the data. If elapsed time is greater than visibleTimeRange, slide
     * view to fit the last (visibleTimeRange - TIME_PADDING) seconds worth of data. For example, if
     * visibleTimeRange = 10seconds and TIME_PADDING = 2seconds. The x axis range will be from time-8
     * to time+2.
     * @param time last logged time
     */
    private void reformatAxis(float time) {
        if(time > visibleTimeRange - TIME_PADDING) {
            xAxis.setAxisMaximum(time + TIME_PADDING);
            xAxis.setAxisMinimum(time + TIME_PADDING - visibleTimeRange);
        } else {
            xAxis.setAxisMaximum(visibleTimeRange);
            xAxis.setAxisMinimum(0);
        }

        chart.fitScreen();
    }

    /**
     * Same as reformatAxis(time) but also adjusts to fit the max Y value. Called if there is a new
     * max Y value. Upper boundary of chart is 1.1 * maxY. Lower boundary is 0.
     * @param maxY maximum y value (pressure or depth)
     * @param time last logged time
     */
    private void reformatAxis(float maxY, float time) {
        float padding = maxY * 0.1f;
        yAxis.setAxisMaximum(maxY + padding);

        reformatAxis(time);
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        /*//Get MAC address from DeviceListActivity via intent
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

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        mConnectedThread.write("x");*/
    }

    @Override
    public void onPause()
    {
        super.onPause();
        /*try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }*/
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
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
                    Log.i("MainActivity", readMessage);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    //e.printStackTrace();
                    //break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
}
