package jarrett.tim.rg;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Our main activity class
 * 
 * @author tjarrett
 * 
 */
public class ActivityMain extends Activity
{
    private static final String SERVER = "Group-A1-Server";

	private static final String CLIENT = "Group-A1-Client";

	private static final String QR_SCANNER = "Group-A1-QR-Scanner";

	/**
     * The list of ThingViews mapped by the values in the spinner
     */
    private Map<String, ThingView> thingView = new HashMap<String, ThingView>();

    /**
     * Our FrameLayout
     */
    private FrameLayout frame;

    /**
     * The currently select ThingView
     */
    private ThingView currentThing;

    /**
     * The TextView that shows the current state
     */
    private TextView currentState;

    // the currently-selected target server device
    private BluetoothDevice selectedDevice;
    
    private BluetoothServer bts;
    
    private String currentPosition = null;
    
    
    private final Handler handler = new Handler() 
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch ( msg.what ) {
                case BluetoothServer.BLUETOOTH_MESSAGE:
                    byte[] buffer = (byte[])msg.obj;
                    String content = new String(buffer, 0, msg.arg1);
                    Log.d(SERVER, "Received " + content);
                    
                    //Message is a server response...
                    if ( content.indexOf("[") != -1 ) {
                        //I can ignore this as the client
                        Log.d(CLIENT, "Server responded with: " + content);
                        
                    } else {
                        //I'm the server so I need to keep going
                        Log.d(SERVER, "Got " + content + " from client");
                        applyEventToCurrentThing(content);
                        
                    }
                    
                    break;
                    
            }
            
        }//end handleMessage
        
        
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //Wire up bluetooth
        bts = BluetoothServer.factory(this, handler);
        bts.initBluetooth();

        // Populate our maps based on the gadgets_array
        // -- to add more gadget create an entry in the gadgets_array and create
        // a class that descends from
        // ThingView with the same name but append "View" (e.g. Monkey =>
        // MonkeyView)
        String[] things = getResources().getStringArray(R.array.gadgets_array);
        for ( String thingName : things ) {
            String className = this.getClass().getPackage().getName() + "." + thingName.replace(" ", "") + "View";

            try {
                ThingView view = (ThingView)Class.forName(className).getConstructor(new Class[] { Context.class }).newInstance(this);
                view.addStateChangeListener(new StateChangeListener()
                {
                    /**
                     * 
                     */
                    @Override
                    public void onStateChanged(ThingView view)
                    {
                        if ( view == currentThing ) {
                            updateCurrentStateMessage();

                            String message = generateMessage(view);
                            if ( message != null ) {
                                Log.d(SERVER, "Thing fired off this: " + message);
                                bts.send(message);
                                
                            }


                        }

                    }

                });

                thingView.put(thingName, view);


            } catch ( Exception e ) {
                // Replace this with a complaint about the class not being found
                throw new RuntimeException(e);
            }

        }// end for

        // Go get our button holder
        LinearLayout buttonHolder = (LinearLayout)findViewById(R.id.button_holder);

        // Put a horizontal linear layout inside of it
        LinearLayout horizButtonHolder = new LinearLayout(getBaseContext());

        // Copy the settings from the parent button holder
        horizButtonHolder.setLayoutParams(buttonHolder.getLayoutParams());

        // Make it horizontal
        horizButtonHolder.setOrientation(LinearLayout.HORIZONTAL);

        // Add it to the parent
        buttonHolder.addView(horizButtonHolder);

        // Point to it as the current holder
        LinearLayout currentButtonHolder = horizButtonHolder;

        // Keep track of how many buttons we've put out
        int counter = 0;

        // Sort the buttons alphabetically
        List<String> eventButtons = new ArrayList<String>(Event.values().length);
        for ( Event event : Event.values() ) {
            eventButtons.add(event.toString());

        }

        // Collections.sort(eventButtons);

        // Now build out the buttons
        for ( String event : eventButtons ) {
            // If this is the 5th button in this row
            if ( counter >= 5 ) {
                // Create a new linearlayout
                horizButtonHolder = new LinearLayout(getBaseContext());

                // Copy the layout params
                horizButtonHolder.setLayoutParams(buttonHolder.getLayoutParams());

                // Make it horizontal
                horizButtonHolder.setOrientation(LinearLayout.HORIZONTAL);

                // Add it
                buttonHolder.addView(horizButtonHolder);

                // Set it as the current one
                currentButtonHolder = horizButtonHolder;

                // Reset the counter
                counter = 0;

            }

            // Inflate our button
            Button btn = (Button)LayoutInflater.from(getBaseContext()).inflate(R.layout.button, null);

            // Set the string on the button
            btn.setText(event);

            /**
             * Set an onclicklistener for each button as it goes by
             */
            btn.setOnClickListener(new OnClickListener()
            {

                @Override
                public void onClick(View btnClicked)
                {
                    String text = ((Button)(btnClicked)).getText().toString();
                    
                    String event = "0,1|" + text;
                    

                    String direction = "ALL";

                    if ( "Heat".equals(text) ) {
                        direction = "UP";

                    } else if ( "Water".equals(text) ) {
                        direction = "DOWN";

                    } else if ( "Reset".equals(text) || "Register".equals(text) ) {
                        direction = null;
                        
                    }

                    //Append direction if it makes sense
                    if ( direction != null ) {
                        event += "|" + direction;
                    }
                   
                    bts.send(event);

                }// end onClick

            });

            // Add it to this button holder
            currentButtonHolder.addView(btn);

            // Bump up our count
            counter++;

        }// end for

        // Fill out our gadget spinner...
        // Derived from spinner example at:
        // http://developer.android.com/resources/tutorials/views/hello-spinner.html
        Spinner gadgetSpinner = (Spinner)findViewById(R.id.gadget_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.gadgets_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gadgetSpinner.setAdapter(adapter);

        currentState = (TextView)findViewById(R.id.current_state);

        // Listen for spinner changes...
        gadgetSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
        {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
            {
                ThingView tmpThingView = thingView.get(parent.getItemAtPosition(pos).toString());
                setDisplayedImage(tmpThingView);

            }

            @Override
            public void onNothingSelected(AdapterView parent)
            {
                // Just swallow it
            }

        });

        // Get the frame view
        frame = (FrameLayout)findViewById(R.id.frame);

    }// end onCreate

    /**
     * Update the image that is being displayed...
     * 
     * @param thingView
     */
    private void setDisplayedImage(ThingView thingView)
    {
        // Remove the current thing view, if any
        if ( currentThing != null ) {
            frame.removeAllViews();

        }

        currentThing = thingView;
        frame.addView(thingView);

        // Display the current state
        updateCurrentStateMessage();

    }// end setDisplayedImage

    /**
     * Try to apply the given Event to whatever the currently selected thing is
     * 
     * @param e
     */
    private void applyEventToCurrentThing(String e)
    {
        // If we don't have a thing, bail out -- this shouldn't be possible
        if ( currentThing == null ) {
            return;

        }

        // Pass the event to the thing
        currentThing.receiveEvent(e);

        // Update the displayed message
        updateCurrentStateMessage();

    }// end applyEventToCurrentThing

    /**
     * Checks with the currently selected thing and displays it's tate
     */
    public void updateCurrentStateMessage()
    {
        currentState.setText("Current State: " + currentThing.getState().toString());

    }// end updateCurrentStateMessage

    private String generateMessage(ThingView view)
    {
        String msg = null;

        if ( view.getEmits() != null ) {
            msg = "0,1|" + view.getEmits();

        }

        return msg;

    }



    /** called when the framework needs to create a dialog */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args)
    {
        switch ( id ) {
            case BluetoothServer.DIALOG_NO_BLUETOOTH:
                return create("No bluetooth... sadness...", true);

            case BluetoothServer.DIALOG_WE_HAVE_BLUETOOTH:
                return create("We have bluetooth! Yippie!", false);

            case BluetoothServer.DIALOG_BLUETOOTH_ENABLED:
                return create("Bluetooth was enabled", false);

            case BluetoothServer.DIALOG_BLUETOOTH_ALREADY_ENABLED:
                return create("Bluetooth was already enabled", false);

            case BluetoothServer.DIALOG_USER_IS_EVIL:
                return create("You MUST enable bluetooth for this application to work!", true);

        }// end switch

        return super.onCreateDialog(id, args);

    }// end onCreateDialog

    /**
     * our dialog creation helper method. quitAfter=true will cause the dialog
     * to finish() this activity
     */
    private Dialog create(String message, final boolean quitAfter)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        return builder.setMessage(message).setPositiveButton("Ok", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
                if ( quitAfter ) {
                    finish();

                }
            }

        }).create();
    }

    /** set up our menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main_menu, menu);
        return true;

    }

    /** handle menu selection to turn the server on and off */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
        switch ( item.getItemId() ) {
            case R.id.bluetooth_settings:
                startActivityForResult(new Intent(this, ActivityBluetooth.class), BluetoothServer.SELECTING_DEVICE);
                return true;
            case R.id.qr_scanning:
            	Log.d(QR_SCANNER, "Initiating QR Scan");
            	IntentIntegrator integrator = new IntentIntegrator(this);
            	integrator.initiateScan();

            	Log.d(QR_SCANNER, " --------------- Got thing:  " + integrator.getMessage());
            	
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /** handle results from startActivityForResult() calls */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
		switch (requestCode) {
		case BluetoothServer.BLUETOOTH_ENABLED:
			if (resultCode == RESULT_OK) {
				// onBluetoothEnabled(); // CONTINUE INITIALIZATION
				// showDialog(DIALOG_BLUETOOTH_ENABLED); // ONLY USED FOR
				// DEMONSTRATION IN CLASS - DO NOT REALLY DO THIS
			} else {
				showDialog(BluetoothServer.DIALOG_USER_IS_EVIL);
			}
			break;
		case BluetoothServer.SELECTING_DEVICE:
			if (resultCode == RESULT_OK) {
				selectedDevice = data.getParcelableExtra("device");
				Log.d("Tim", "Got the device...");
				// bts.sendMessageToServer("Go go gadget arms");
				// ((TextView)findViewById(R.id.selected_device)).setText(selectedDevice.getName());
			} else {
				// ((TextView)findViewById(R.id.selected_device)).setText("NO DEVICE SELECTED");
			}
			break;
		case IntentIntegrator.REQUEST_CODE:
			IntentResult scanResult = IntentIntegrator.parseActivityResult(
					requestCode, resultCode, data);
			if (scanResult != null) {
				String ntf_title = "RG Location Set";
				String ntf_text = "Scan Position Returned: " + scanResult.getContents();
				
				createNotification(ntf_title, ntf_text);
				
				Log.d(QR_SCANNER, "Scan Position Returned: " + scanResult.getContents());
				currentPosition = scanResult.getContents();
			}
			break;
		default:
			Log.e("Tim", "unknown activity request code: " + requestCode);
		}
        super.onActivityResult(requestCode, resultCode, data);
    }

	private void createNotification(String ntf_title, String ntf_text) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		
		Notification notification = new Notification(android.R.drawable.ic_menu_mylocation, ntf_text, System.currentTimeMillis());

		Intent notificationIntent = new Intent(this, ActivityMain.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.setLatestEventInfo(this, ntf_title, ntf_text, contentIntent);
		mNotificationManager.notify(1, notification);
	}




}// end ActivityMain
