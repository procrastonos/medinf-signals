package medinf.medinfsignals;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

import com.androidplot.Plot;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import medinf.medinfsignals.ConnectedThread;

public class SensorPlot extends Activity
{
    // constants
    public static int MESSAGE_READ = 1234;
    private static final int LIGHT = 0;
    private static final int EMG = 1;
    private static final int FREQ_SIZE = 200;
    private static final int HISTORY_SIZE = 600;
    private static final int VALUE_SIZE = 1024;
    private static final int VALUE_OFFSET = 0;

    // GUI elements
    private XYPlot historyPlot = null;
    private XYPlot frequencyPlot = null;

    // plot objects
    private SimpleXYSeries lightHistorySeries;
    private SimpleXYSeries emgHistorySeries;
    private SimpleXYSeries freqSeries;
    private SimpleXYSeries iftSeries;
    private Redrawer histRedrawer;
    private Redrawer freqRedrawer;

    // local objects
    private ConnectedThread connectedThread;        // thread handling reading from BT socket
    private Handler messageHandler;                 // message handler to pass data from thread to activity
    FreqAnalysis freqAnalysis;                      // frequency analysis object

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // create frequency analysis object (upper bound for window, upper bound for range, window)
        freqAnalysis = new FreqAnalysis(FREQ_SIZE, 5, 40);

        // create the message handler
        messageHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                // select correct message
                if (msg.what == MESSAGE_READ)
                {
                    // receive message value and function code
                    int val = msg.arg1;
                    int code = msg.arg2;

                    // draw light data
                    if (code == LIGHT)
                        drawLight(val);

                    // if value was an EMG(EOG) reading
                    if (code == EMG)
                    {
                        // draw the plain EMG plot
                        drawEMG(val);

                        // update EMG data model
                        freqAnalysis.update(val);

                        //draw forward and reverse fft plots
                        drawFFT(freqAnalysis.calcFFT());        // history window of 100
                        drawIFT(freqAnalysis.calcIFT());   // frequency range of 2..40
                    }
                }
            }
        };

        //Display bleibt aktiv
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // set view
        setContentView(R.layout.sensor_plot_layout);

        // set up series
        lightHistorySeries = new SimpleXYSeries("Brightness");
        lightHistorySeries.useImplicitXVals();
        emgHistorySeries = new SimpleXYSeries("EMG");
        emgHistorySeries.useImplicitXVals();
        freqSeries = new SimpleXYSeries("Frequency");
        freqSeries.useImplicitXVals();
        iftSeries = new SimpleXYSeries("LowiFrequencies");
        iftSeries.useImplicitXVals();

        // set up history plot
        historyPlot = (XYPlot) findViewById(R.id.historyPlot);
        historyPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        historyPlot.setRangeBoundaries(VALUE_OFFSET, VALUE_SIZE, BoundaryMode.FIXED);
        historyPlot.setDomainStepValue(HISTORY_SIZE/10);
        historyPlot.addSeries(lightHistorySeries, new LineAndPointFormatter(Color.rgb(255, 0, 0), null, null, null));
        historyPlot.addSeries(emgHistorySeries, new LineAndPointFormatter(Color.rgb(0, 0, 255), null, null, null));
        historyPlot.addSeries(iftSeries, new LineAndPointFormatter(Color.rgb(0, 255, 0), null, null, null));
        historyPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        historyPlot.setDomainLabel("Time");
        historyPlot.getDomainLabelWidget().pack();
        historyPlot.setRangeLabel("Brightness/EMG Val");
        historyPlot.getRangeLabelWidget().pack();
        historyPlot.setRangeValueFormat(new DecimalFormat("#"));
        historyPlot.setDomainValueFormat(new DecimalFormat("#"));

        // set up frequency plot
        frequencyPlot = (XYPlot) findViewById(R.id.frequencyPlot);
        frequencyPlot.setDomainBoundaries(0, FREQ_SIZE, BoundaryMode.FIXED);
        frequencyPlot.setRangeBoundaries(0, VALUE_SIZE, BoundaryMode.FIXED);
        frequencyPlot.setDomainStepValue(FREQ_SIZE/10);
        frequencyPlot.addSeries(freqSeries, new LineAndPointFormatter(Color.rgb(0, 255, 0), null, null, null));
        frequencyPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        frequencyPlot.setDomainLabel("Frequency");
        frequencyPlot.getDomainLabelWidget().pack();
        frequencyPlot.setRangeLabel("Apmlitude");
        frequencyPlot.getRangeLabelWidget().pack();
        frequencyPlot.setRangeValueFormat(new DecimalFormat("#"));
        frequencyPlot.setDomainValueFormat(new DecimalFormat("#"));

        // set up re-drawer threads
        histRedrawer = new Redrawer(Arrays.asList(new Plot[]{historyPlot}), 100, false);
        freqRedrawer = new Redrawer(Arrays.asList(new Plot[]{frequencyPlot}), 10, false);
        histRedrawer.start();
        freqRedrawer.start();

        // set up and start connection handling thread
        connectedThread = new ConnectedThread(App.socket, messageHandler);
        connectedThread.start();
    }

    // draw light plot
    private synchronized void drawLight(int value)
    {
        // remove oldest sample on history
        if (lightHistorySeries.size() > HISTORY_SIZE)
        {
            lightHistorySeries.removeFirst();
        }

        // add latest sample to history
        lightHistorySeries.addLast(null, value);
    }

    // draw EMG(EOG) plot
    private synchronized void drawEMG(int value)
    {
        // remove oldest sample on history
        if (emgHistorySeries.size() > HISTORY_SIZE)
        {
            emgHistorySeries.removeFirst();
        }

        // add latest sample to history
        emgHistorySeries.addLast(null, value);

    }

    // draw forward FFT plot
    private synchronized void drawFFT(ArrayList<Float> fft_data) {
        freqSeries.setModel(fft_data, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
    }

    // draw reverse FFT plot
    private synchronized void drawIFT(ArrayList<Float> ift_data)
    {
        iftSeries.setModel(ift_data, SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
    }

    private synchronized void drawEyeDetect()
    {

    }

    @Override
    public void onResume()
    {
        super.onResume();
        // re-draw plots
        histRedrawer.start();
        freqRedrawer.start();
    }

    @Override
    public void onPause()
    {
        histRedrawer.finish();
        freqRedrawer.finish();

        super.onPause();
    }

    @Override
    public void onDestroy()
    {
        histRedrawer.finish();
        freqRedrawer.finish();
        connectedThread.cancel();
        //Beendet aktuelle Aktivity
        finish();

        super.onDestroy();
    }

    public void onFinish()
    {
        histRedrawer.finish();
        freqRedrawer.finish();
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
}
