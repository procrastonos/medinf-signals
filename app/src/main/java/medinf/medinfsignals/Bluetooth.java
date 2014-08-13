package medinf.medinfsignals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;




/**
 * Bluetooth-Verbindung
 *
 */
public class Bluetooth {
    // Lesebuffer
    static byte[] buff = new byte[1];

    // Bluetooth device
    public static BluetoothDevice device = null;
    // Bluetooth socket
    public static BluetoothSocket socket = null;
    public static InputStream input = null;
    public static OutputStream output = null;
    //   public static DataInputStream datainput = null;
    public static int adc_filter = 0;


    public static void setBluetoothDevice (BluetoothDevice device) {
        Bluetooth.device = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            socket = Bluetooth.device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
            input = socket.getInputStream();
            output = socket.getOutputStream();
        } catch (IOException e) {
            //e.printStackTrace();
            Log.v("Socket creation", "Failed to create bluetooth socket");
        }
    }


    public static BluetoothSocket getBTSocket() {
        return socket;
    }
    /**
     * Stellt die Verbindung her
     * @throws IOException
     */
    public static void connect () throws IOException {
        // Cancel discovery because it will slow down the connection
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            socket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            Log.v("Bluetooth connection", "Failed to connect to bluetooth device");
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.v("Bluetooth closing", "Failed to close bluetooth socket");
            }
        }
    }

    /**
     * Empfängt Daten vom entferntem Device
     * @param packet empfangener Byte-Paket
     * @return gelesenen ADC-Wert
     * @throws IOException
     */
    public synchronized static short read() throws IOException {
        // ----3----
        //Die Daten sollen von der Bluetooth-Schnittstelle des Mikrocontrollers eingelesen werden
        //und anschließend übergeben werden. Dafür benötigen Sie noch einen Lesebuffer (siehe oben)
        //Alternative: Überlegen Sie sich eine Implementierung für das Empfangen von mehreren Channeln.

        //TODO: Implementierung der Methode mit oben beschriebener Funktionalität

        input.read(buff);

        return buff[0];

        //return (short)(buff[0]<<8 + buff[1]);
    }


    /**
     * Sendet Byte-Paket an entferntes Device
     * @param packet
     * @throws IOException
     */
    public synchronized static void send (int packet) throws IOException {
        output.write(packet);
        output.flush();
    }

    /**
     * Schliesst die Verbindung
     * @throws IOException
     */
    public static void disconnect() throws IOException {
        if (socket != null){
            socket.close();
            socket = null;
        }
    }

    /**
     * Schliesst den Stream
     */
    public static void stopStream () throws IOException{
        if (input != null || output != null){
            input = null;
            output = null;
        }
    }

    /**
     * öffnet den Stream
     * @throws IOException
     */
    public static void startStream () throws IOException {
        if (input == null || output == null){
            input = socket.getInputStream();
            output = socket.getOutputStream();
        }
    }
}
