package medinf.medinfsignals;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;

import com.androidplot.Plot;
import com.androidplot.util.PlotStatistics;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.*;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import medinf.medinfsignals.Bluetooth;

// ???
interface ValueReceivedListener{
    public short ValueReceived(short value);
}

// ???
class ValueReceivedClass implements ValueReceivedListener {
    @Override
    public short ValueReceived(short value) {
        return value;
    }
}

class BluetoothThread extends Thread {
    List<ValueReceivedListener> listeners = new ArrayList<ValueReceivedListener>();

    volatile boolean fPause = false;
    public void addListener(ValueReceivedListener toAdd){
        listeners.add(toAdd);
    }

    public void run() {
        short value = 0;
        while (true) {
            //Werte von Bluetooth.read() auslesen und dem Handler übergeben
            //TODO:
            // Bluetooth.read() auslesen

            try {
                value = Bluetooth.read();
            } catch (IOException e) {
                // panic!
            }

            // Listener auslösen und value übergeben
            if (value > 0) {
                for (ValueReceivedListener vrl : listeners)
                    vrl.ValueReceived(value);
            }
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

public class SensorPlot extends Activity
{
    // bluetooth thread
    private BluetoothThread readBThread;

    private static final int HISTORY_SIZE = 300;
    private XYPlot lightHistoryPlot = null;
    private SimpleXYSeries lightHistorySeries;
    private Redrawer redrawer;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

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
        lightHistorySeries = new SimpleXYSeries("light");

        lightHistoryPlot.setRangeBoundaries(0, 1024, BoundaryMode.FIXED);
        lightHistoryPlot.setDomainStepValue(HISTORY_SIZE/10);
        lightHistoryPlot.addSeries(lightHistorySeries, new LineAndPointFormatter(Color.rgb(200, 100, 100), null, null, null));
        lightHistoryPlot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        lightHistoryPlot.setDomainStepValue(HISTORY_SIZE/10);
        lightHistoryPlot.setDomainLabel("Sample Index");
        lightHistoryPlot.getDomainLabelWidget().pack();
        lightHistoryPlot.setRangeLabel("Unit");
        lightHistoryPlot.getRangeLabelWidget().pack();

        lightHistoryPlot.setRangeValueFormat(new DecimalFormat("#"));
        lightHistoryPlot.setDomainValueFormat(new DecimalFormat("#"));

        final PlotStatistics histStats = new PlotStatistics(1000, false);

        lightHistoryPlot.addListener(histStats);

        redrawer = new Redrawer(Arrays.asList(new Plot[]{lightHistoryPlot}), 100, false);
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
            //
        }
        //Beendet die Aktivity und gibt Speicher frei
        System.exit(0);
        super.onBackPressed();
    }


    // new sensor data
    /*
    public synchronized void newDataReceived(Event event) {
        lightSeries.setModel(Arrays.asList(
                new Number[]{event.values[0]}),
        SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

        // remove oldest sample on history
        if (lightHistorySeries.size() > HISTORY_SIZE) {
            lightHistorySeries.removeFirst();
        }

        // add latest sample to history
        lightHistorySeries.addLast(null, event.values[0]);
    }
*/
}
