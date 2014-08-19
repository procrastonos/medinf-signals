package medinf.medinfsignals;

import android.util.Log;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.ArrayList;
import java.util.Arrays;

public class FreqAnalysis
{
    // constants
    private int HISTORY_SIZE = 0;
    private int FFT_SIZE = 0;
    private int IFT_SIZE = 0;
    private int WINDOW_SIZE = 50;
    private int lower = 0;
    private int higher = 0;
    private int num_of_maxima = 10;
    private static final int LEFT = -1;
    private static final int RIGHT = 1;

    // flags
    private byte direction = 0;
    private int certainty = 0;

    // data
    private ArrayList<Float> data;                          // raw data
    private ArrayList<Float> maxima;                        // list of measured maxima
    private float[] fft_array;                              // array of forward fourier transformed data
    private float[] ift_array;                              // array of inverse fourier transformed data
    private int threshold = 10;                              // threshold for movement detection
    private float average = 500;                            // current average of raw data
    private float high_average = 0;                         // average of ift values greater than the average
    private float low_average = 0;                          // average of ift values smaller than the average

    // local objects
    private FloatFFT_1D fft;                                // forward fast fourier transform object
    private FloatFFT_1D ift;                                // reverse fast fourier transform object

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

    private float getMax(float [] array)
    {
        float max = 0;

        for (int i=array.length - WINDOW_SIZE; i<array.length; i++)
        {
            if (array[i] - average > max)
                max = array[i];
        }

        return max;
    }

    private float getMin(float [] array)
    {
        float min = 0;

        for (int i=array.length - WINDOW_SIZE; i<array.length; i++)
        {
            if (array[i] - average < min)
                min = array[i];
        }

        return min;
    }

    // update data model
    public void update(int value)
    {
        //-add-data---------------------------------------------------------------------------------
        // add sample to data
        data.add((float) value);

        // update average
        average = average + ((value - average)/HISTORY_SIZE);

        // remove oldest sample
        if (data.size() > FFT_SIZE)
            data.remove(0);

        //-calculatet-fft---------------------------------------------------------------------------
        // fill float array from ArrayList
        int i = 0;
        for (Float f : data)
        {
            fft_array[i++] = (f != null ? f : 0);
        }
        // perform forward fft
        fft.realForward(fft_array);

        //-calculate-ift----------------------------------------------------------------------------
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

        //-calculate-thresholds
        high_average = high_average + ((getMax(fft_array) - high_average)/HISTORY_SIZE);

        //-detect-eye-movement----------------------------------------------------------------------
        if (Math.abs(ift_array[ift_array.length-1] - average) > threshold)
        {
            /*// add detected maximum to list
            maxima.add(new Float(value));

            // remove oldest maximum
            if (maxima.size() > num_of_maxima)
                maxima.remove(0);

            */
            if (value - average > 0)
                direction = 1; // eye movement to the right?
            else
                direction = -1; // eye movement to the left

        }
    }

    // calculate and return the forward fft
    public ArrayList<Float> calcFFT()
    {
        // move floats back into ArrayList. Yes, this is completely pointless.
        ArrayList<Float> fft_list = new ArrayList<Float>();

        for (int i=0; i < fft_array.length; i++)
            fft_list.add(fft_array[i]);

        return fft_list;
    }

    public ArrayList<Float> calcIFT()
    {
        // move floats back into ArrayList
        ArrayList<Float> ift_list = new ArrayList<Float>();

        // shift by offset to right border
        int offset = HISTORY_SIZE - FFT_SIZE;
        float shift = 50;

        for (int i=0; i < offset; i++)
            ift_list.add(new Float(0));

        for (int i=0; i < ift_array.length; i++)
            ift_list.add(ift_array[i] - shift);

        return ift_list;
    }

    public float getThreshold()
    {
        return high_average;
    }

    public float getAverage()
    {
        return average;
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