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

    private CheckBox hwAcceleratedCb;
    private CheckBox showFpsCb;

    private XYPlot lightLevelsPlot = null;
    private XYPlot lightHistoryPlot = null;

    private SimpleXYSeries lightSeries;
    private SimpleXYSeries lightHistorySeries;

    private Redrawer redrawer;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.historylight); // TODO: create View

        lightLevelsPlot = (XYPlot) findViewByID(R.id.lightLevelsPlot);
        lightLevelsPlot.setBoundaries(-1, 1, BoundaryMode.FIXED);
        lightLevelsPlot.getGraphWidget().getDomainLabelPaint().setColor(Color.TANSPARENT);

        // setup a´light Levels Plot
        lightSeries = new SimpleXYSeries("light");

        lightLevelsPlot.addSeries(lightSeries, new BarFormatter(Color.rgb(0, 200, 0), Color.rgb(0, 80, 0);

        lightLevelsPlot.setDomainStepValue(3);
        lightLevelsPlot.setTicksPerRangeLabel(3);

        lightLevelsPlot.setRangeBoundaries(0, 1024, BoundaryMode.FIXED);

        // update domain and range axis labels
        lightLevelsPlot.setDomainLabel("");
        lightLevelsPlot.getDomainLabelWidget().pack();
        lightLevelsPlot.setRangeLabel("Angle (Degs)");
        lightLevelsPlot.getRangeLabelWidget().pack();
        lightLevelsPlot.setGridPadding(15, 0, 15, 0);
        lightLevelsPlot.setRangeValueFormat(new DecimalFormat("#"));

        // setup history plot
        lightHistoryPlot = (XYPlot) findViewById(R.id.lightHistoryPlot);
        lightHistorySeries = new SimpleXYSeries("light");

        lightHistoryPlot.setRangeBoundaries(0, 1024, BundaryMode.FIXED);
        lightHistoryPlot.setDomainStepValue(HISTORY_SIZE), BoundaryMode.FIXED);
        lightHistoryPlot.addSeries(lightHistorySeries, new LineAndPointFormatter(Color.rgb(200, 100, 100), null, null, null));
        lightHistoryPLot.setDomainStepMode(XYStepMode.INCREMENT_BY_VAL);
        lightHistoryPlot.setDomainStepValue(HISTORY_SIZE/10);
        lightHistoryPlot.setDomainLabel("Sample Index");
        lightHistoryPlot.getDomainLabelWidget().pack();
        lightHistoryPlot.setRangeLabel("Unit");
        lightHistoryPlot.getRangeLabelWidget().pack();

        lightHistoryPlot.setRangeValueFormat(new DecimalFormat("#"));
        lightHistoryPLot.setDomainValueFormat("new DecimalFormat"#"));

                // setup check boxes
                hwAcceleratedCb = (CheckBox findViewById(R.id.hwAccelerationCb);
        final PlotStatistics levelStats = new PlotStatistics(1000, false);
        final PlotStatistics histStats = new PlotStatistics(1000, false);

        lightLevelsPlot.addListener(levelStats);
        lightHistoryPLot.addListener(histStats);
        hwAccelerated.Cb.setOnCheckedChangedListener(new CompoundButton.OnCHeckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton boolean b) {
                if(b) {
                    lightLevelsPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                    lightHistoryPlot.setLayerType(View.LAYER_TYPE_NONE, null);
                }
                else
                {
                    lightLevelsPlot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    lightHistoryPLot.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }
            }
        });

        showFpsCb = (CheckBox) findViewById(R.id.showFpsCb);
        showFpsCb.setOnChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCHeckedChanged(CompondButton compoundButton, boolean b) {
                levelStats.setAnnotatePlotEnabled(b);
                histStats.setAnnotetePlotEnabled(b);
            }
        });

        redrawer = new Redrawer(Arrays.asList(new Plot[]{lightHistoryPlot, lightLevelsPlot}), 100, false);
    }

    @Override
    public void on Resume() {
    super.onResume();
    redrawer.start();
}

    @Override
    public void onPause() {
        redrawer.pause();
    }

    @Override
    public onDestroy() {
        redrawer.finish();
        super.onDestroy();
    }

    // new sensor data
    public synchronized void onSensorChanged(Event event) {
        lightSeries.setModel(Arrays.asList(
                newNumber[]{event.values[0]}),
        SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

        // remove oldest sample on history
        if (lightHistorySeries.size() > HISTORY_SIZE) {
            lightHistory.removeFirst();
        }

        // add latest sample to history
        lightHistorySeries.addLatest(null, event.values[0]);
    }

}
















}
        }
