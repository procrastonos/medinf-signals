package medinf.medinfsignals;

import android.util.Log;

import org.jtransforms.fft.FloatFFT_1D;

import java.util.ArrayList;
import java.util.Arrays;

public class FreqAnalysis
{
    // constants
    private static int FFT_SIZE = 0;
    private static int IFT_SIZE = 0;
    private static int lower = 0;
    private static int higher = 0;

    // data
    ArrayList<Float> data;
    float[] fft_array;
    float[] ift_array;

    // local objects
    FloatFFT_1D fft;                                // forward fast fourier transform object
    FloatFFT_1D ift;                                // reverse fast fourier transform object

    public FreqAnalysis(int fft_size, int lower, int higher)
    {
        // set data sizes
        FFT_SIZE = fft_size;
        // prevent out of range values for lower and higher
        lower = Math.max(0, Math.min(lower, higher));
        higher = Math.min(FFT_SIZE, Math.max(lower, higher));
        // IFT_SIZE = higher - lower;
        IFT_SIZE = FFT_SIZE;

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
    }

    // calculate and return the forward fft
    public ArrayList<Float> calcFFT()
    {
        // fill float array from ArrayList
        int i = 0;
        for (Float f : data)
        {
            fft_array[i++] = (f != null ? f : 0);
        }

        // perform forward fft
        fft.realForward(fft_array);

        // move floats back into ArrayList. Yes, this is completely pointless.
        ArrayList<Float> fft_list = new ArrayList<Float>();

        for (i=0; i < fft_array.length; i++)
            fft_list.add(fft_array[i]);

        return fft_list;
    }

    public ArrayList<Float> calcIFT()
    {
        // reset ift array to 0s
        Arrays.fill(ift_array, 0);

        // get the frequency values from the given window
        for (int i=lower, j=0; i<higher; i++, j++)
        {
            ift_array[j] = fft_array[i];
        }

        ift.realInverse(ift_array, true);

        /*// shift values to right border
        //float [] shifted = new float[FFT_SIZE];
        Arrays.fill(ift_array, 0);
        int offset = FFT_SIZE - (higher - lower);
        for (int i = offset, j =0; i<FFT_SIZE; i++, j++)
            shifted[i] = ift_array[j];
        */

        // move floats back into ArrayList
        ArrayList<Float> ift_list = new ArrayList<Float>();

        for (int i=0; i < ift_array.length; i++)
            ift_list.add(ift_array[i]);

        return ift_list;
    }
}
