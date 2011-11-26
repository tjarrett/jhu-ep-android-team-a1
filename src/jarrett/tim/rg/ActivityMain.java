package jarrett.tim.rg;

import java.util.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.TextView;

/**
 * Our main activity class
 * 
 * @author tjarrett
 * 
 */
public class ActivityMain extends Activity
{	
	public static final String DEBUG = "A1-DEBUG";
	
	/**
	 * If this is set to true, will try to send button press events via bluetooth otherwise 
	 * keeps the events local -- mostly for testing purposes in an emulator only. Leave this 
	 * as true as it will get flipped to false if no bluetooth is detected (and a message will 
	 * be displayed)
	 */
	private boolean bluetoothMode = true;

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
    
    /**
     * Reference to our bluetooth server
     */
    private BluetoothServer bts;
    
    /**
     * The currentPosition within the grid. Defaults to unknown (maybe we should default to 0,0?)
     */
    private String currentPosition = "unknown";
    
    /**
     * This handler responds to incoming messages via bluetooth
     * 
     * @todo Figure out a better way to differentiate between incoming commands and something that has been 
     *       "emitted" from the remote devices
     */
    private final Handler handler = new Handler() 
    {
        /**
         * Handles incoming messages
         */
        @Override
        public void handleMessage(Message msg)
        {
            switch ( msg.what ) {
                case BluetoothServer.BLUETOOTH_MESSAGE:
                    //Get the buffer out of the message
                    byte[] buffer = (byte[])msg.obj;
                    
                    //Turn the buffer into a string
                    String content = new String(buffer, 0, msg.arg1);
                    Log.d(RgTools.SERVER, "Received " + content);
                    
                    //Swallow acknowledgements (caused by responding with (int)1 which we aren't doing
                    if ( content.trim().equals("") ) {
                        return;
                    }
                    
                    //Message is a server response...
                    if ( content.indexOf("[") != -1 ) {
                        //I can ignore this as the client
                        RgTools.createNotification(getApplicationContext(), "Receiving Response", content, android.R.drawable.ic_input_add); //This doesn't work when multiple responses coming in succession
                        Log.d(RgTools.CLIENT, content);
                        
                    } else {
                        //I'm the server so I need to keep going
                    	RgTools.createNotification(getApplicationContext(), "Receiving ", content, android.R.drawable.ic_input_add);
                        applyEventToCurrentThing(content);
                        
                    }
                    
                    break;
                    
            }//end switch
            
        }//end handleMessage
        
    };//end Handler

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        //Wire up bluetooth
        if ( bluetoothMode ) {
            bts = BluetoothServer.factory(this, handler);
            bts.initBluetooth();
            
        }

        // Populate our maps based on the gadgets_array
        // -- to add more gadget create an entry in the gadgets_array and create
        // a class that descends from
        // ThingView with the same name but append "View" (e.g. Monkey =>
        // MonkeyView)
        String[] things = getResources().getStringArray(R.array.gadgets_array);
        for ( String thingName : things ) {
            //Figure out the fully qualified class name for this thing
            String className = this.getClass().getPackage().getName() + "." + thingName.replace(" ", "") + "View";

            try {
                //Create a new instance of this particular thing
                ThingView view = (ThingView)Class.forName(className).getConstructor(new Class[] { Context.class }).newInstance(this);
                
                //Add a state change listener that will be called when the state changes
                view.addStateChangeListener(new StateChangeListener()
                {
                    /**
                     * This will be called when state changes
                     */
                    @Override
                    public void onStateChanged(ThingView view)
                    {
                        //Since state changes can technically happen on a thing that is not currently displayed, make sure 
                        //that the currentThing and the view given match
                        if ( view == currentThing ) {
                            //Update the state message
                            updateCurrentStateMessage();

                            //Figure out if this state change caused more events to be fired... 
                            if ( view.getEmits() != null ) {
                                //It's not null, but that doesn't mean anything, could be an empty list...
                                List<String> emits = view.getEmits();
                                
                                if ( emits.size() > 0 ) {
                                    //List wasn't empty so loop through sending each message to the remote server
                                    for ( String msg : emits ) {
                                        //Build the message
                                        String finalMsg = currentPosition + "|" + msg;
                                        
                                        //Mangle this message so the other server knows it's a response and not a command...
                                        //This Rube Goldberg Protocol needs some work...
                                        
                                        //todo: remove the next line...
                                        finalMsg = finalMsg + "[";
                                        
                                        Log.d(RgTools.SERVER, "Thing fired off this: " + finalMsg);
                                        
                                        if ( bluetoothMode ) {
                                            //Send the message
                                            sendEvent(finalMsg);
                                            
                                        }
                                        
                                    }//end for
                                    
                                }

                            }

                        }

                    }//end onStateChanged

                });

                //Add this thing view to the list
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
                /**
                 * Handle a button being clicked
                 */
                @Override
                public void onClick(View btnClicked)
                {
                    //Get the text of the button
                    String text = ((Button)(btnClicked)).getText().toString();
                    
                    //Start building our event
                    String event = currentPosition + "|" + text;
                    
                    //Default direction is ALL which is what the homework says to do... but 
                    //I think this is going to be a problem since we are now supposed to send 
                    //Right, Left, Up, Down, etc
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
                    
                    //If the button pressed is "Start", keep it local...
                    if ( "Start".equals(text) ) {
                        //Start always stays local
                        applyEventToCurrentThing(event);
                        
                    } else {
                        //Send the event (potentially out over bluetooth)
                        sendEvent(event);
                        
                    }

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
            /**
             * Called when an item is selected
             */
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
            {
                ThingView tmpThingView = thingView.get(parent.getItemAtPosition(pos).toString());
                setDisplayedImage(tmpThingView);

            }//end onItemSelected

            /**
             * Called when nothing is selected
             */
            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // Just swallow it
            }//end onNothingSelected

        });

        // Get the frame view
        frame = (FrameLayout)findViewById(R.id.frame);

    }// end onCreate

    /**
     * Called just before the activity is destroyed
     */
    @Override protected void onDestroy() 
    {
        //unregister any of our broadcast receivers
        unregisterReceiver(bts.getBroadcastReceiver());
        super.onDestroy();
        
    }//end onDestroy

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
    
    /**
     * Send the given event to the widget (either over bluetooth or locally)
     * @param event
     */
    private void sendEvent(String event)
    {
    	RgTools.createNotification(getApplicationContext(), "Sending Event", event, android.R.drawable.ic_menu_share);
        if ( bluetoothMode ) {
            bts.send(event);
            
        } else {
            applyEventToCurrentThing(event);
            
        }
        
    }//end sendEvent
    
    /** called when the framework needs to create a dialog */
    @Override
    protected Dialog onCreateDialog(int id, Bundle args)
    {
        switch ( id ) {
            /**
             * This device does not support bluetooth... fall back to local mode for testing purposes...
             * This is really just for the emulator...
             */
            case BluetoothServer.DIALOG_NO_BLUETOOTH:
                bluetoothMode = false;
                return create("Your device does not appear to support bluetooth. Falling back to local only mode.", false);

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

    }//end onCreateOptionsMenu

    /** handle menu selection to turn the server on and off */
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item)
    {
        switch ( item.getItemId() ) {
            case R.id.bluetooth_settings:
                startActivityForResult(new Intent(this, ActivityBluetooth.class), BluetoothServer.SELECTING_DEVICE);
                return true;
                
            case R.id.qr_scanning:
            	Log.d(RgTools.QR_SCANNER, "Initiating QR Scan");
            	IntentIntegrator integrator = new IntentIntegrator(this);
            	integrator.initiateScan();
            	Log.d(RgTools.QR_SCANNER, " --------------- Got thing:  " + integrator.getMessage());
            	break;
            	
        }//end switch
        
        return super.onMenuItemSelected(featureId, item);
        
    }//end onMenuItemSelected

    /** handle results from startActivityForResult() calls */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
		switch ( requestCode ) {
		    /* handle the case where we tried to enable bluetooth */
    		case BluetoothServer.BLUETOOTH_ENABLED:
    		    //Handle the case where it wasn't enabled
    			if ( resultCode != RESULT_OK ) {
    				showDialog(BluetoothServer.DIALOG_USER_IS_EVIL);
    				try {
                        finalize(); //bluetooth not enabled but it is required so quit
                        
                    } catch ( Throwable e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        
                    }
    			
    		    //Otherwise it was enabled so start the BluetoothServer
    			} else {
    	            if ( bluetoothMode && !bts.isServerRunning() ) {
    	                bts.startServer();
    	                
    	            }
    	            
    			}
    			break;
    	
    	    /* Handle QR code stuff */
    		case IntentIntegrator.REQUEST_CODE:
    			IntentResult scanResult = IntentIntegrator.parseActivityResult(
    					requestCode, resultCode, data);
    			if ( scanResult != null ) {
    				String ntf_title = "RG Location Set";
    				String ntf_text = "Scan Position Returned: " + scanResult.getContents();
    				
    				RgTools.createNotification(getApplicationContext(), ntf_title, ntf_text, android.R.drawable.ic_menu_mylocation);
    				
    				Log.d(RgTools.QR_SCANNER, "Scan Position Returned: " + scanResult.getContents());
    				currentPosition = scanResult.getContents();
    				
    			}
    			break;
    			
		}//end switch
		
        super.onActivityResult(requestCode, resultCode, data);
        
    }//end onActivityResult

}// end ActivityMain
