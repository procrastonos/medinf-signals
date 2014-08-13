package medinf.medinfsignals;

import android.app.Activity;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.lang.Math.*;
import medinf.medinfsignals.Bluetooth;

public class SensorPlot extends Activity
{
    // bluetooth thread
    private BluetoothThread readBThread;
    Button b;

    private static final int HISTORY_SIZE = 1024;

    private int BT_MSG = 2342;
    private XYPlot lightHistoryPlot = null;
    private SimpleXYSeries lightHistorySeries;
    private Redrawer redrawer;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

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

    private Handler messageHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == BT_MSG)
            {
                Log.d("brightness value", ""+msg.arg1);
                drawData(msg.arg1);
            }
        }
    };

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
