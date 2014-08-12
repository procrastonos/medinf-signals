package medinf.medinfsignals;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

import java.io.IOException;

import medinf.medinfsignals.Bluetooth;

/**
 * Zweitaktivity
 * Diese Activity dient dazu, die Werte des Bluetooth-Geraetes zu Visualisieren
 */
public class OrientedSensor extends Activity
{	
	private BluetoothThread readBThread;

	/**
	 * Ausfuehrungsparallelitaet innerhalb des Programms, um die Bluetooth-Werte auszulesen und 
	 * weiterzugeben
	 */
    private class BluetoothThread extends Thread {
    	volatile boolean fPause = false;
    	 
    	   public void run() {
		   short value = 0;
    		   while (true) {
 	    		  		//Werte von Bluetooth.read() auslesen und dem Handler übergeben
    	    		  	//TODO:
				// Bluetooth.read() auslesen
				//value = Bluetooth.read();
				// Listener auslösen und value übergeben
				/*for (ValueRecievedListener VRL : listener)
					vrl.ValueRecieved(value);
*/
	   	        }
	       }
    	   
    	    /**
    		* Stoppt den ausgefuehrten Thread
    		*/
    	    public void stopT() {
    	         fPause = true;
    	    }
    	   
    	   
	    	 /**
	   		 * Startet den gestoppten Thread
	   		 */
			public void startT() {
	    	      fPause = false;
	    	}
    }
    
    
    /** 
     * Wird aufgerufen, wenn die Aktivity gestartet wird. Visualisierung der eingelesenen
     * Bluetooth-Werte 
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Auswahl des erstellten Layouts
        //setContentView(R.layout.XXX);

        //Display bleibt aktiv
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        //Wenn Thread nicht gestartet, versuche ihn zu starten
        if (readBThread == null) {
        	
        	readBThread = new BluetoothThread();
        	readBThread.start();
        }
    }     
        
    /**
     * Wird nach Interaktion mit dem Benutzer aufgerufen. Folgt immer nach onPause()
     */
    public void onResume() {
        super.onResume();
    }

    /**
     *  Pausiert die aktuelle Aktivity. Wird aufgerufen, wenn das System eine vorherige Aktivity fortsetzt.
     *  Wird genutzt, um nicht gespeicherte Daten im Speicher zu sichern. Gefolgt von onResume() wird diese
     *  Aktivity anschliessend wieder ausgefuehrt
     */
    public void onPause() {
        super.onPause();
    }

    /**
     * Die letzte Ausfuehrung, bevor die Aktivity beendet wird. Dies kann passieren, wenn die
     * Aktivity durch finish() beendet wurde oder das System die Aktivity zerstoert, um temporaeren
     * Speicherplatz freizugeben.  
     */
    public void onDestroy() {
    	try {
    		//Bluetooth-Verbindung schließen
			Bluetooth.disconnect();
		} catch (IOException e) {
		}
    	//Beendet aktuelle Aktivity
    	finish();
        super.onDestroy();
    }
   
    /**
     * Die letzte Ausfuehrung, bevor die Aktivity beendet wird.
     */
    public void onFinish(){
    	try {
    		//Bluetooth-Verbindung schließen
			Bluetooth.disconnect();
		} catch (IOException e) {
		}
    	//Beendet aktuelle Aktivitx
    	finish();
        super.onDestroy();
    }
    
    /**
     * Wird nach Interaktion mit dem Benutzer aufgerufen, wenn dieser die Zueruck-Taste drueckt
     */
    public void onBackPressed()
    {
    	try {
    		//Bluetooth-Verbindung schließen
			Bluetooth.disconnect();
		} catch (IOException e) {
		}
    	//Beendet die Aktivity und gibt Speicher frei
    	System.exit(0);
    	super.onBackPressed();
    }
    
 
}
