package medinf.medinfsignals;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.androidplot.Plot;
import com.androidplot.util.PlotStatistics;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.*;
import java.text.DecimalFormat;
import java.util.Arrays;

public class SensorPlot extends Activity
{
    private static final int HISTORY_SIZE = 300;

    private XYPlot lightHistoryPlot = null;

    private SimpleXYSeries lightHistorySeries;

    private Redrawer redrawer;
//remove
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_plot_layout); // TODO: create View

        // setup history plot
        lightHistoryPlot = (XYPlot) findViewById(R.id.lightHistoryPlot);
        lightHistoryPlot.setDomainBoundaries(0, 300, BoundaryMode.FIXED);
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
        redrawer.finish();
        super.onDestroy();
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
