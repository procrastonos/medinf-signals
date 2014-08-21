package medinf.medinfsignals;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

public class App extends Application {
    // global socket to share between activities
    public static BluetoothSocket socket = null;
}
