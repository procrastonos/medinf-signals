package medinf.medinfsignals;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ConnectedThread extends Thread
{
    public static int MESSAGE_READ = 1234;

    private Handler mHandler;
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    public ConnectedThread(BluetoothSocket socket, Handler handler)
    {
        mHandler = handler;
        mmSocket = socket;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;

        // Get the input and output streams, using temp objects because
        // member streams are final
        try
        {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e)
        {
            Log.e("socket stream error", "Failed to get I/O Streams for BT socket!");
        }

        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    public void run()
    {
        //Log.v("run", "Trying to read...");
        byte[] buffer = new byte[2];    // buffer store for the stream
        int bytes;                      // # of bytes returned from read()
        int value;                      // unpacked buffer

        // Keep listening to the InputStream until an exception occurs
        while (true)
        {
            try
            {
                // check if stream has bytes available
                bytes = mmInStream.available();

                // if so..
                if (bytes > 0)
                {
                    // read two bytes
                    mmInStream.read(buffer);

                    // message format:
                    // [0..1]        [00..11]             [00000..111111]
                    // high/low bit  2 bit function code  5 bit data

                    // split into high and low bytes
                    int h = (int) buffer[0] & 0x000000FF;
                    int l = (int) buffer[1] & 0x000000FF;

                    // get function code
                    int c_h = (h & 0x60) >> 5;
                    int c_l = (l & 0x60) >> 5;

                    // accept only buffer in right order and same function code
                    if ((h & 128) == 128 && (l & 128) == 0 && c_h == c_l)
                    {
                        // combine data to one int
                        value = ((h & 0x1f) << 5) + (l & 0x1f);

                        // Send the obtained bytes to the UI activity
                        mHandler.obtainMessage(MESSAGE_READ, value, c_h).sendToTarget();
                    }
                }
            } catch (IOException e)
            {
                Log.e("io", "Failed to read from socket!");
                break;
            }
        }
    }

    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes)
    {
        try
        {
            mmOutStream.write(bytes);
        } catch (IOException e) { }
    }

    /* Call this from the main activity to shutdown the connection */
    public void cancel()
    {
        try
        {
            mmSocket.close();
        } catch (IOException e) { }
    }
}