package medinf.medinfsignals;

import android.util.Log;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.ArrayList;
import java.util.Arrays;

public class FreqAnalysis
{
    // constants
    private int HISTORY_SIZE = 0;           // size of raw data array
    private int FFT_SIZE = 0;               // size of forward fft array
    private int IFT_SIZE = 0;               // size of inverse fft array
    private int WINDOW_SIZE = 10;           // size of window in which to search for maxima
    private static final int LEFT = -1;     // define for left eye movement
    private static final int RIGHT = 1;     // define for right eye movement
    private int lower = 0;                  // lower bound of frequency window for inverse fft
    private int higher = 0;                 // upper bound for frequency window for inverse fft

    // flags
    private byte direction = 0;             // direction in which eye moveing
    private int certainty = 0;              // certainty value for REM detection

    // data
    private ArrayList<Float> data;          // raw data
    private ArrayList<Float> maxima;        // list of measured maxima
    private float[] fft_array;              // array of forward fourier transformed data
    private float[] ift_array;              // array of inverse fourier transformed data
    private float high_threshold = 120;     // upper threshold for movement detection
    private float low_threshold = 120;      // lower threshold for movement detection
    private float average = 500;            // current average of raw data
    private float high_average = 0;         // average of ift values greater than the average
    private float low_average = 0;          // average of ift values smaller than the average
    private int counter = 0;                // counter of detected movements
    private float frequency = 0;            // frequency of eye movement

    // local objects
    private FloatFFT_1D fft;                // forward fast fourier transform object
    private FloatFFT_1D ift;                // reverse fast fourier transform object

    public FreqAnalysis(int hist_size, int fft_size, int low, int high)
    {
        // set data sizes
        HISTORY_SIZE = hist_size;
        FFT_SIZE = fft_size;
        IFT_SIZE = fft_size;
        // prevent out of range values for lower and higher
        lower = Math.max(0, Math.min(low, high));
        higher = Math.min(FFT_SIZE, Math.max(low, high));

        // initialize data
        data = new ArrayList<Float>();
        maxima = new ArrayList<Float>();

        // create new empty array and fill with 0s
        fft_array = new float[FFT_SIZE];
        Arrays.fill(fft_array, 0);
        ift_array = new float[IFT_SIZE];
        Arrays.fill(ift_array, 0);

        // initialize fft objects
        fft = new FloatFFT_1D(FFT_SIZE);
        ift = new FloatFFT_1D(IFT_SIZE);
    }

    private float getMax(float [] array, int window_size)
    {
        float max = 0;

        // find maximum (absolute) value of array in the given window
        for (int i=array.length - window_size; i<array.length; i++)
        {
            if (Math.abs(array[i] - average) > max)
                max = array[i];
        }

        return max;
    }

    private float getMin(float [] array, int window_size)
    {
        float min = 0;

        for (int i=array.length - window_size; i<array.length; i++)
        {
            if (array[i] - average < min)
                min = array[i];
        }

        return min;
    }

    private void addData(int value)
    {
        // add sample to data
        data.add((float) value);

        // update average
        average = average + ((value - average)/HISTORY_SIZE);

        // remove oldest sample
        if (data.size() > FFT_SIZE)
            data.remove(0);
    }

    private void calcFFT()
    {
        // fill float array from ArrayList
        int i = 0;
        for (Float f : data)
        {
            // fill array with 0s if list isn't filled yet
            fft_array[i++] = (f != null ? f : 0);
        }
        // perform forward fft
        fft.realForward(fft_array);
    }

    private void calcIFT()
    {
        int i = 0;
        // reset ift array to 0s
        Arrays.fill(ift_array, 0);
        // get the frequency values from the given window
        for (i=lower; i<higher; i++)
        {
            ift_array[i] = fft_array[i];
        }

        // set base frequency
        if (lower > 0)
            ift_array[0] = 68496;

        // perform inverse fft
        ift.realInverse(ift_array, true);

        for (i=0; i<ift_array.length; i++)
        {
            // scale ift output
            ift_array[i] = average + (float)Math.pow((ift_array[i] - average) * 0.5, 3);
        }

    }

    // update data model
    public void update(int value)
    {
        addData(value);

        calcFFT();
        calcIFT();

        // detect eye movement
        if (getMax(ift_array, WINDOW_SIZE) - average > high_threshold)
        {
            // count rising signal edges
            if (certainty == 0)
                counter++;
            certainty = 1;
        }
        else
        {
            certainty = 0;
            direction = 0;
        }
    }

    // calculate frequency. is called every 10ms
    public int getFrequency()
    {
        // update frequency by weighted current movements per minute
        frequency += ((counter * 6000) - frequency) / 100;
        // reset counter
        counter = 0;
        return (int)frequency;
    }

    // calculate and return the forward fft
    public ArrayList<Float> getFFT()
    {
        // move floats back into ArrayList. Yes, this is completely pointless.
        ArrayList<Float> fft_list = new ArrayList<Float>();

        for (int i=0; i < fft_array.length; i++)
            fft_list.add(fft_array[i]);

        return fft_list;
    }

    public ArrayList<Float> getIFT()
    {
        // move floats back into ArrayList
        ArrayList<Float> ift_list = new ArrayList<Float>();

        // shift by offset to right border
        int offset = HISTORY_SIZE - FFT_SIZE;
        float shift = 0;

        for (int i=0; i < offset; i++)
            ift_list.add(new Float(0));

        for (int i=0; i < ift_array.length; i++)
            ift_list.add(ift_array[i] - shift);

        return ift_list;
    }

    public float getAverage()
    {
        return average;
    }

    public float getThreshold(boolean high)
    {
        return high? high_threshold : low_threshold;
    }

    public int getREMCertainty()
    {
        return certainty;
    }

    public byte getEyeDirection()
    {
        return direction;
    }
}
