package medinf.medinfsignals;

        import java.io.IOException;
        import java.io.InputStream;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.Set;

        import medinf.medinfsignals.Bluetooth;
        import medinf.medinfsignals.OrientedSensor;

        import android.app.Activity;
        import android.app.AlertDialog;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.os.Bundle;
        import android.view.Menu;
        import android.view.MenuInflater;
        import android.view.MenuItem;
        import android.view.View;
        import android.view.WindowManager;
        import android.widget.AdapterView;
        import android.widget.AdapterView.OnItemClickListener;
        import android.widget.Button;
        import android.widget.ListView;
        import android.widget.ProgressBar;
        import android.widget.SimpleAdapter;
        import android.widget.TextView;
        import android.widget.Toast;


/**
 * Hauptactivity
 * Diese Activity dient dazu, andere Bluetooth-Geraete zu finden und diese aufzulisten. Bei Auswahl eines Geraetes
 * wird eine neue Activity gestartet.
 */
public class MainActivity extends Activity {

    // Intent Request Code fuer Bluetooth
    private static final int REQUEST_ENABLE_BT = 1337;
    //GUI Elemente
    private ProgressBar progressSearch = null;
    private Button searchButton = null;
    // Liste mit allen gefundenen Bluetooth-Geraeten
    public static ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
    // Liste mit Namen und MACs
    private List<Map<String, String>> data = new ArrayList<Map<String, String>>();
    // Adapter fuer die ListView
    private SimpleAdapter deviceListAdapter = null;
    public InputStream is;
    // Adapter für bluetooth
    private BluetoothAdapter mBluetoothAdapter;

    /**
     * BroadcastReceiver zum Empfang von Broadcast Nachrichten.
     */
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        /**
         * Behandlung der Nachrichten ACTION_FOUND, ACTION_DISCOVERY_STARTED,
         * ACTION_DISCOVERY_FINISHED
         */
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            // neues Geraet wurde gefunden
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                // BluetoothDevice Objekt von Intent holen
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Name und Adresse zu ListView hinzufuegen
                Map<String, String> datum = new HashMap<String, String>(2);
                datum.put("name", device.getName());
                datum.put("mac", device.getAddress());
                data.add(datum);
                devices.add(device);

                // Adapter aktualisieren, da ein neues Geraet hinzugekommen ist
                deviceListAdapter.notifyDataSetChanged();
                // Ausgabe einer kurzen Meldung, dass ein Geraet gefunden wurde
                Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.find_new_device) + device.getName(), Toast.LENGTH_SHORT);
                toast.show();

            // Suche wurde gestartet
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                // ProgressBar anzeigen
                progressSearch.setVisibility(View.VISIBLE);
                // Ausgabe einer kurzen Meldung, dass die Geraetesuche gestartet wurde
                Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.search_started), Toast.LENGTH_SHORT);
                toast.show();

                // Suche wurde beendet
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                //ProgressBar verstecken
                progressSearch.setVisibility(View.GONE);
                // Ausgabe einer kurzen Meldung, dass die Geraetesuche beendet wurde
                Toast toast = Toast.makeText(getApplicationContext(), getResources().getString(R.string.search_ended), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    };

    /**
     * Initialisierung der Fortschrittsanzeige für die Suche (progressSearch) des broadcastReceivers, der
     * Geraeteliste (deviceList) sowie des searchButtons.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout setzen
        setContentView(R.layout.activity_main);

        //Display aktiv lassen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // GUI-Element progressSearch holen
        progressSearch = (ProgressBar)findViewById(R.id.progressSearch);
        progressSearch.setVisibility(View.GONE); // zuerst verstecken

        // BroadcastReceiver registrieren. (Muss in onDestroy deregistriert werden!!)
        registerReceiver(broadcastReceiver,new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(broadcastReceiver,new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(broadcastReceiver,new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        // Adapter für ListVIew initialisieren
        deviceListAdapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2,
                new String[] {"name", "mac"},
                new int[] {android.R.id.text1, android.R.id.text2});

        // GUI-Element deviceList holen
        final ListView deviceList =  (ListView)findViewById(R.id.deviceList);
        // GUI-Element header aufblähen
        TextView header = (TextView)getLayoutInflater().inflate(R.layout.listheader, null);
        header.setText(R.string.discovered_devices);	// Text setzen

        // Deaktivierung des Header (verbietet anklicken)
        header.setEnabled(false);
        // Header für deviceList setzen
        deviceList.addHeaderView(header);
        // Adapter für deviceList setzen
        deviceList.setAdapter(deviceListAdapter);
        // Listener für click events definieren
        deviceList.setOnItemClickListener(new OnItemClickListener() {

            // ----2----
            //Bei Klick auf ein Bluetooth-Geraet weitere Suche abbrechen und Bluetooth-Geraet ueber die
            //id setzen. Neuen Intent erstellen und neue Activity starten.
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //nur auf Elemente in der Liste reagieren
                if (view.isEnabled()){
                    //TODO: Implementierung der Methode mit oben beschriebener Funktionalitaet

                    // suche abbrechen
                    mBluetoothAdapter.cancelDiscovery();

                    // Bluetooth device anhand von id setzen
                    Bluetooth.setBluetoothDevice(devices.get((int)id));

                    try {
                        // Zu Bluetooth device verbinden
                        Bluetooth.connect();
                    } catch (IOException e) {
                        // Verbindung fehlgeschlagen
                    }

                    // Neue Activity starten
                    Intent intent = new Intent(MainActivity.this, OrientedSensor.class);

                    startActivity(intent);

                }
            }
        });

        //Button zum Starten der Bluetooth-Geraete Suche holen
        searchButton = (Button)findViewById(R.id.searchButton);
        // Listener für click events definieren;
        searchButton.setOnClickListener(new View.OnClickListener() {

            // ----1----
            //Holen des BluetoothAdapter;
            //ueberpruefung, ob Bluetooth unterstuetzt wird und eingeschaltet ist;
            //Ist Bluetooth nicht eingeschaltet, soll mit Hilfe eines Intent eine Activity fuer eine Anfrage zum Aktivieren
            //von Bluetooth gestartet werden (Tipp: BluetoothAdapter.ACTION_REQUEST_ENABLE; startActivityForResult).
            //Ist Bluetooth aktiviert, wird die Liste der gefundenen Geraete zurueckgesetzt und eine neue Suche gestartet
            @Override
            public void onClick(View v) {
                //TODO: Implementierung der Methode mit oben beschriebener Funktionalitaet
                // get bluetooth adapter
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                // check if bluetooth adapter exists
                if (mBluetoothAdapter == null) {
                    // no bluetooth
                }

                // check if bluetooth is enabled
                if (!mBluetoothAdapter.isEnabled()) {
                    // ask user to enable bluetooth
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else startBluetooth();
            }
        });
    }

    // Bluetooth discovery starten
    private void startBluetooth() {
        // Device-Liste zurücksetzen
        devices.clear();
        data.clear();

        // start discovery
        mBluetoothAdapter.startDiscovery();
    }

    // activity result callback
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT)
            if (resultCode == RESULT_OK) // either RESULT_OK or RESULT_CANCELED
                startBluetooth();
    }

    /**
     * Activity neu laden, nachdem die letzte beendet wurde
     */
    protected void refreshActivity () {
        //Activity beenden
        finish();
        //Neue Activity erzeugen
        Intent myIntent = new Intent(MainActivity.this, MainActivity.class);
        startActivity(myIntent);
    }

    /**
     * Die letzte Ausfuehrung, bevor die Aktivity beendet wird. Dies kann passieren, wenn die
     * Aktivity durch finish() beendet wurde oder das System die Aktivity zerstoert, um temporaeren
     * Speicherplatz freizugeben.
     */
    protected void onDestroy() {
        try {
            Bluetooth.disconnect();
        } catch (IOException e) {}
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);	//BroadcastReceiver deregistrieren
    }

    /**
     * Erstellt ein Options-Menue
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu, menu);
        return result;
    }

    /**
     * Erzeuge eine Auswahl fuer das Options-Menue
     */
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.exit:
                new AlertDialog.Builder(this).setTitle(R.string.exitConfirmTitle)
                        .setMessage(R.string.exitConfirmText)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                                System.exit(0);
                            }
                        })
                        .setNegativeButton("Abbrechen", new
                                DialogInterface.OnClickListener() {

                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                        .show();
        }
        return true;
    }
}