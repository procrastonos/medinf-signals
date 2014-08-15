package medinf.medinfsignals;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;

import com.androidplot.Plot;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;


public class SensorPlot extends Activity
{
    private ConnectedThread connectedThread;
    private Handler messageHandler;
    // bluetooth thread
    Button b;

    public static int MESSAGE_READ = 1234;
    private static final int LIGHT = 0;
    private static final int EMG = 1;
    private static final int HISTORY_SIZE = 600;
    private static final int VALUE_SIZE = 1024;
    private static final int VALUE_OFFSET = 0;

    private int BT_MSG = 2342;
    private XYPlot historyPlot = null;
    private XYPlot frequencyPlot = null;
    private SimpleXYSeries lightHistorySeries;
    private SimpleXYSeries emgHistorySeries;
    private SimpleXYSeries freqSeries;
    private Redrawer histRedrawer;
    private Redrawer freqRedrawer;


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("socket stream error", "Failed to get I/O Streams for BT socket!");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.v("run", "Trying to read...");
            byte[] buffer = new byte[2];  // buffer store for the stream
	        //byte[] finalBuffer = new byte[2];
            int low = 0;

            int bytes; // bytes returned from read()
	        int value; // Upacked buffer

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    bytes = mmInStream.available();

                    if (bytes > 0) {
                        mmInStream.read(buffer);

                        int h = (int)buffer[0] & 0x000000FF;
                        int l = (int)buffer[1] & 0x000000FF;

                        int c_h = (h & 0x60) >> 5;
                        int c_l = (l & 0x60) >> 5;

                        if ((h & 128) == 128 && (l & 128) == 0 && c_h == c_l) {
                            value = ((h & 0x1f) << 5) + (l & 0x1f);

                            // Send the obtained bytes to the UI activity
                            messageHandler.obtainMessage(MESSAGE_READ, value, c_h).sendToTarget();
                        }
                    }
                } catch (IOException e) {
                    Log.v("io", "Failed to read from socket!");
                    break;
                }

                try {
                    sleep(1, 0);
                }
                catch (InterruptedException e) {
                }

            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        messageHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_READ)
                {
                    int val = (int)(msg.arg1);
                    int code = (int)(msg.arg2);
                    drawData(val, code);
                }
            }
        };

        //Display bleibt aktiv
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.sensor_plot_layout);

        // set up series
        lightHistorySeries = new SimpleXYSeries("Brightness");
        lightHistorySeries.useImplicitXVals();
        emgHistorySeries = new SimpleXYSeries("EMG");
        emgHistorySeries.useImplicitXVals();

        // set up history plot
        historyPlot = (XYPlot) findViewById(R.id.historyPlot);
        historyPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        historyPlot.setRangeBoundaries(VALUE_OFFSET, VALUE_SIZE, BoundaryMode.FIXED);
        historyPlot.setDomainStepValue(HISTORY_SIZE/10);
        historyPlot.addSeries(lightHistorySeries, new LineAndPointFormatter(Color.rgb(255, 0, 0), null, null, null));
        historyPlot.addSeries(emgHistorySeries, new LineAndPointFormatter(Color.rgb(0, 0, 255), null, null, null));
        historyPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        historyPlot.setDomainLabel("Time");
        historyPlot.getDomainLabelWidget().pack();
        historyPlot.setRangeLabel("Brightness/EMG Val");
        historyPlot.getRangeLabelWidget().pack();
        historyPlot.setRangeValueFormat(new DecimalFormat("#"));
        historyPlot.setDomainValueFormat(new DecimalFormat("#"));

        // set up frequency plot
        frequencyPlot = (XYPlot) findViewById(R.id.frequencyPlot);
        frequencyPlot.setDomainBoundaries(0, 100, BoundaryMode.FIXED);
        frequencyPlot.setRangeBoundaries(0, VALUE_SIZE, BoundaryMode.FIXED);
        frequencyPlot.setDomainStepValue(HISTORY_SIZE/10);
        frequencyPlot.addSeries(emgHistorySeries, new LineAndPointFormatter(Color.rgb(0, 255, 0), null, null, null));
        frequencyPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        frequencyPlot.setDomainLabel("Frequency");
        frequencyPlot.getDomainLabelWidget().pack();
        frequencyPlot.setRangeLabel("Apmlitude");
        frequencyPlot.getRangeLabelWidget().pack();
        frequencyPlot.setRangeValueFormat(new DecimalFormat("#"));
        frequencyPlot.setDomainValueFormat(new DecimalFormat("#"));


        histRedrawer = new Redrawer(Arrays.asList(new Plot[]{historyPlot}), 100, false);
        freqRedrawer = new Redrawer(Arrays.asList(new Plot[]{frequencyPlot}), 100, false);
        histRedrawer.start();
        freqRedrawer.start();

        connectedThread = new ConnectedThread(App.socket);
        connectedThread.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        histRedrawer.start();
        freqRedrawer.start();
    }

    @Override
    public void onPause()
    {
        histRedrawer.start();
        freqRedrawer.start();
    }

    @Override
    public void onDestroy() {
        histRedrawer.start();
        freqRedrawer.start();
        //Beendet aktuelle Aktivity
        finish();

        super.onDestroy();
    }

    public void onFinish(){
        //Beendet aktuelle Aktivity
        finish();
        super.onDestroy();
    }

    public void onBackPressed()
    {
        //Beendet die Aktivity und gibt Speicher frei
        System.exit(0);
        super.onBackPressed();
    }

    // new sensor data
    public synchronized void drawData(int value, int code) {

        if (code == LIGHT) {
            // remove oldest sample on history
            if (lightHistorySeries.size() > HISTORY_SIZE) {
                lightHistorySeries.removeFirst();
            }

            // add latest sample to history
            lightHistorySeries.addLast(null, value);
        }
        if (code == EMG) {
            // remove oldest sample on history
            if (emgHistorySeries.size() > HISTORY_SIZE) {
                emgHistorySeries.removeFirst();
            }

            // add latest sample to history
            emgHistorySeries.addLast(null, value);
        }
    }
}
