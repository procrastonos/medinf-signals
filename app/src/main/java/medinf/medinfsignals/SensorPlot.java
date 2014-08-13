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
import com.androidplot.util.PlotStatistics;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.lang.Math.*;
import medinf.medinfsignals.Bluetooth;

public class SensorPlot extends Activity
{
    private ConnectedThread connectedThread;
    private Handler messageHandler;
    // bluetooth thread
    private BluetoothThread readBThread;
    Button b;

    public static int MESSAGE_READ = 1234;
    private static final int HISTORY_SIZE = 1024;

    private int BT_MSG = 2342;
    private XYPlot lightHistoryPlot = null;
    private SimpleXYSeries lightHistorySeries;
    private Redrawer redrawer;


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
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                    Log.v("bytes", ""+bytes);
                    messageHandler.obtainMessage(MESSAGE_READ, bytes, 0)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.v("io", "Failed to read from socket!");
                    break;
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
                    //byte[] buff = (byte[])msg.obj;
                    int val = (int)(msg.arg1);

                    Log.d("brightness value", "" + val);
                    drawData(val);
                }
            }
        };

        connectedThread = new ConnectedThread(App.socket);
        connectedThread.run();

        b = (Button)findViewById(R.id.button);

        //Display bleibt aktiv
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Wenn Thread nicht gestartet, versuche ihn zu starten
        if (readBThread == null) {

            readBThread = new BluetoothThread();
            readBThread.start();
        }

        setContentView(R.layout.sensor_plot_layout);

        // setup history plot
        lightHistoryPlot = (XYPlot) findViewById(R.id.lightHistoryPlot);
        lightHistoryPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        lightHistorySeries = new SimpleXYSeries("Brightness");
        lightHistorySeries.useImplicitXVals();

        lightHistoryPlot.setRangeBoundaries(0, 260, BoundaryMode.FIXED);
        lightHistoryPlot.setDomainStepValue(HISTORY_SIZE/10);
        lightHistoryPlot.addSeries(lightHistorySeries, new LineAndPointFormatter(Color.rgb(255, 0, 0), null, null, null));
        lightHistoryPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        lightHistoryPlot.setDomainLabel("Time");
        lightHistoryPlot.getDomainLabelWidget().pack();
        lightHistoryPlot.setRangeLabel("Brightness");
        lightHistoryPlot.getRangeLabelWidget().pack();

        lightHistoryPlot.setRangeValueFormat(new DecimalFormat("#"));
        lightHistoryPlot.setDomainValueFormat(new DecimalFormat("#"));

        redrawer = new Redrawer(Arrays.asList(new Plot[]{lightHistoryPlot}), 100, false);
    }

    class BluetoothThread extends Thread {
        volatile boolean fPause = false;

        int i=0;

        public void run() {
            int value = 0;
            while (true) {
                //Werte von Bluetooth.read() auslesen und dem Handler übergeben
                //TODO:

                try {
                    value = (int)Bluetooth.read();
                //value = 200+(int)(100*(1.0+Math.sin((double)(i))));
                } catch (IOException e) {
                    Log.v("Bluetooth read", "Failed to read from bt device");
                }

                //Message msg = Message.obtain(messageHandler, BT_MSG, value, 0);
                messageHandler.obtainMessage(BT_MSG, value, 0).sendToTarget();
                //messageHandler.sendMessage(msg);
            }
        }


        /**
         * Stoppt den ausgefuehrten Thread
         */
        public void stopT() {
            fPause = true;
        }


        /**
         * Startet den gestoppten Thread
         */
        public void startT() {
            fPause = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        redrawer.start();
    }

    @Override
    public void onPause()
    {
        redrawer.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        try {
            //Bluetooth-Verbindung schließen
            Bluetooth.disconnect();
        } catch (IOException e) {
            Log.v("Plot destroy", "Failed to disconnect from bt");
        }

        redrawer.finish();
        //Beendet aktuelle Aktivity
        finish();

        super.onDestroy();
    }

    public void onFinish(){
        try {
            //Bluetooth-Verbindung schließen
            Bluetooth.disconnect();
        } catch (IOException e) {
            Log.v("Plot destroy", "Failed to disconnect from bt");
        }
        //Beendet aktuelle Aktivity
        finish();
        super.onDestroy();
    }

    public void onBackPressed()
    {
        try {
            //Bluetooth-Verbindung schließen
            Bluetooth.disconnect();
        } catch (IOException e) {
            Log.v("Plot destroy", "Failed to disconnect from bt");
        }
        //Beendet die Aktivity und gibt Speicher frei
        System.exit(0);
        super.onBackPressed();
    }

    // new sensor data
    public synchronized void drawData(int value) {
        // remove oldest sample on history
        if (lightHistorySeries.size() > HISTORY_SIZE) {
            lightHistorySeries.removeFirst();
        }
        //b.setText(value + "");
        // add latest sample to history
        lightHistorySeries.addLast(null, value);
    }
}
