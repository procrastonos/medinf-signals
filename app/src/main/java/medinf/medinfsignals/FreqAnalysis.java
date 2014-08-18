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
    private int lower = 0;
    private int higher = 0;

    // data
    ArrayList<Float> data;
    float[] fft_array;
    float[] ift_array;

    // local objects
    FloatFFT_1D fft;                                // forward fast fourier transform object
    FloatFFT_1D ift;                                // reverse fast fourier transform object

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
        // create new empty array and fill with 0s
        fft_array = new float[FFT_SIZE];
        Arrays.fill(fft_array, 0);
        ift_array = new float[IFT_SIZE];
        Arrays.fill(ift_array, 0);

        // initialize fft objects
        fft = new FloatFFT_1D(FFT_SIZE);
        ift = new FloatFFT_1D(IFT_SIZE);
    }

    // update data model
    public void update(int value)
    {
        // add sample to data
        data.add((float) value);

        // remove oldest sample
        if (data.size() > FFT_SIZE)
            data.remove(0);

        // calculate fft
        // fill float array from ArrayList
        int i = 0;
        for (Float f : data)
        {
            fft_array[i++] = (f != null ? f : 0);
        }
        // perform forward fft
        fft.realForward(fft_array);

        // reset ift array to 0s
        Arrays.fill(ift_array, 0);

        // calculate rft
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

    public int getREMCertainty()
    {
        return 0;
    }
}
