package jarrett.tim.rg;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.javadude.rube.protocol.Direction;
import com.javadude.rube.protocol.Event;
import com.javadude.rube.protocol.EventCarrier;
import com.javadude.rube.protocol.Reporter;
import com.javadude.rube.protocol.SocketHandler;
import com.javadude.rube.protocol.SocketHandlerHolder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Our main activity class
 * 
 * @author tjarrett
 * 
 */
public class ActivityMain extends Activity implements Reporter,
		SocketHandlerHolder {

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
	 * The currentPosition within the grid. Defaults to unknown (maybe we should
	 * default to 0,0?)
	 */
	private String currentPosition = "0,1";

	/**
	 * The socketHander for network connectivity
	 */
	private SocketHandler socketHandler;

	/**
	 * Handler to get message back onto the UI thread
	 * Based on code here:
	 * http://stackoverflow.com/questions/5097267/error-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-view
	 */
	private Handler messageHandler = new Handler() {
		/**
		 * Handle a message
		 */
		@Override
		public void handleMessage(Message msg) {
			String event = (String) msg.obj;
			ActivityMain.this.applyEventToCurrentThing(event);
		}

	};

	/**
	 * Out IP address field
	 */
	private EditText ipAddress;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// As per Prof. Stanchfield...
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// The IP Address field, so we don't have to recompile in class
		ipAddress = (EditText) findViewById(R.id.ip_address);

		// Make a connect button to connect to the given IP address
		Button btnConnect = (Button) findViewById(R.id.connect);
		btnConnect.setOnClickListener(new OnClickListener() {
			/**
			 * Handle the connect button being clicked...
			 */
			@Override
			public void onClick(View arg0) {
				// Connect to socket server via ip address defined in ip address
				// textview
				Socket socket = null;
				try {
					socket = new Socket(ipAddress.getText().toString(), 4242);
					socketHandler = new GadgetSocketHandler(ActivityMain.this,
							socket);
					socketHandler.start();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

		});

		// Populate our maps based on the gadgets_array
		// -- to add more gadget create an entry in the gadgets_array and create
		// a class that descends from
		// ThingView with the same name but append "View" (e.g. Monkey =>
		// MonkeyView)
		String[] things = getResources().getStringArray(R.array.gadgets_array);
		for (String thingName : things) {
			// Figure out the fully qualified class name for this thing
			String className = this.getClass().getPackage().getName() + "."
					+ thingName.replace(" ", "") + "View";

			try {
				// Create a new instance of this particular thing
				ThingView view = (ThingView) Class.forName(className)
						.getConstructor(new Class[] { Context.class })
						.newInstance(this);

				// Add a state change listener that will be called when the
				// state changes
				view.addStateChangeListener(new StateChangeListener() {
					/**
					 * This will be called when state changes
					 */
					@Override
					public void onStateChanged(ThingView view) {
						// Since state changes can technically happen on a thing
						// that is not currently displayed, make sure
						// that the currentThing and the view given match
						if (view == currentThing) {
							// Update the state message
							updateCurrentStateMessage();

							// Figure out if this state change caused more
							// events to be fired...
							if (view.getEmits() != null) {
								// It's not null, but that doesn't mean
								// anything, could be an empty list...
								List<String> emits = view.getEmits();

								if (emits.size() > 0) {
									// List wasn't empty so loop through sending
									// each message to the remote server
									for (String msg : emits) {
										// Build the message
										String finalMsg = currentPosition + "|"
												+ msg;

										// Mangle this message so the other
										// server knows it's a response and not
										// a command...
										// This Rube Goldberg Protocol needs
										// some work...

										// todo: remove the next line...
										// finalMsg = finalMsg + "[";

										Log.d(RgTools.SERVER,
												"Thing fired off this: "
														+ finalMsg);

										sendEvent(finalMsg);

									}// end for

								}

							}

						}

					}// end onStateChanged

				});

				// Add this thing view to the list
				thingView.put(thingName, view);

			} catch (Exception e) {
				// Replace this with a complaint about the class not being found
				throw new RuntimeException(e);

			}

		}// end for

		// Fill out our event and gadget spinners...
		// Derived from spinner example at:
		// http://developer.android.com/resources/tutorials/views/hello-spinner.html
		Spinner eventSpinner = (Spinner) findViewById(R.id.event_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.events_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		eventSpinner.setAdapter(adapter);

		// Listen for spinner changes...
		eventSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			/**
			 * Called when an item is selected
			 */
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				if (pos != 0) {
					String text = parent.getItemAtPosition(pos).toString();

					// Start building our event
					String event = currentPosition + "|" + text;

					// Default direction is ALL which is what the homework says
					// to do... but
					// I think this is going to be a problem since we are now
					// supposed to send
					// Right, Left, Up, Down, etc
					String direction = "ALL";
					Direction directionObject = Direction.ALL;

					if ("Heat".equals(text)) {
						direction = "UP";
						directionObject = Direction.UP;

					} else if ("Water".equals(text)) {
						direction = "DOWN";
						directionObject = Direction.DOWN;

					} else if ("Reset".equals(text) || "Register".equals(text)) {
						direction = null;
					}

					// Append direction if it makes sense
					if (direction != null) {
						event += "|" + direction;
					}

					// If the button pressed is "Start", keep it local...
					if ("Start".equals(text)) {
						// Start always stays local
						applyEventToCurrentThing(event);

					} else {
						// Send out the event -- maybe even over bluetooth

						// If it's ElectricOn, send both Up and Right
						if ("ElectricOn".equals(text)) {
							sendEvent(currentPosition, text, Direction.UP);
							sendEvent(currentPosition, text, Direction.RIGHT);

							// If it's ElectricOff, send Down and left
						} else if ("ElectricOff".equals(text)) {
							sendEvent(currentPosition, text, Direction.DOWN);
							sendEvent(currentPosition, text, Direction.LEFT);
							sendEvent(currentPosition, text, Direction.RIGHT);

						} else if ("Pull".equals(text)) {
							sendEvent(currentPosition, text, Direction.UP);
							sendEvent(currentPosition, text, Direction.RIGHT);
							sendEvent(currentPosition, text, Direction.LEFT);

						} else if ("Release".equals(text)) {
							sendEvent(currentPosition, text, Direction.DOWN);
							sendEvent(currentPosition, text, Direction.LEFT);

						} else {
							// Everything else send the event we constructed
							sendEvent(currentPosition, text, directionObject);
						}

					}
				} else {
					Log.d(RgTools.DEBUG,
							"Selected the 0th position in spinner.");
				}

				// reset to 'Select Event'
				parent.setSelection(0);

			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Do nothing
			}// end onClick

		});

		Spinner gadgetSpinner = (Spinner) findViewById(R.id.gadget_spinner);
		adapter = ArrayAdapter.createFromResource(this, R.array.gadgets_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		gadgetSpinner.setAdapter(adapter);

		// Listen for spinner changes...
		gadgetSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			/**
			 * Called when an item is selected
			 */
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int pos, long id) {
				ThingView tmpThingView = thingView.get(parent
						.getItemAtPosition(pos).toString());
				setDisplayedImage(tmpThingView);

			}// end onItemSelected

			/**
			 * Called when nothing is selected
			 */
			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Just swallow it
			}// end onNothingSelected

		});

		updateCurrentPositionView();

		currentState = (TextView) findViewById(R.id.current_state);

		// Get the frame view
		frame = (FrameLayout) findViewById(R.id.frame);

	}// end onCreate

	/**
	 * Called just before the activity is destroyed
	 */
	@Override
	protected void onDestroy() {
		// unregister any of our broadcast receivers
		socketHandler.close();
		super.onDestroy();

	}// end onDestroy

	/**
	 * Update the image that is being displayed...
	 * 
	 * @param thingView
	 */
	private void setDisplayedImage(ThingView thingView) {
		// Remove the current thing view, if any
		if (currentThing != null) {
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
	private void applyEventToCurrentThing(String e) {
		// If we don't have a thing, bail out -- this shouldn't be possible
		if (currentThing == null) {
			Log.e(RgTools.SERVER,
					"Current Thing Not Selected. Something is very wrong.");
			return;

		}

		// Pass the event to the thing
		currentThing.receiveEvent(e);

		// Update the displayed message
		updateCurrentStateMessage();

	}// end applyEventToCurrentThing

	/**
	 * Checks the current position thing and displays it
	 */
	private void updateCurrentPositionView() {
		((TextView) findViewById(R.id.current_position))
				.setText("Current Grid Position: " + currentPosition);
	}

	/**
	 * Checks with the currently selected thing and displays it's state
	 */
	public void updateCurrentStateMessage() {
		currentState.setText("Current State: "
				+ currentThing.getState().toString());

	}// end updateCurrentStateMessage

	/**
	 * Parse out a string formatted like 0,1|Heat|Up and pass it on to the new
	 * sendEvent method
	 * 
	 * @param eventMessage
	 */
	private void sendEvent(String eventMessage) {
		// Split it apart
		String[] bits = eventMessage.split("\\|");

		if (bits.length == 2) {
			sendEvent(bits[0], bits[1], null);

		} else if (bits.length == 3) {
			Direction direction = Direction.valueOf(bits[2]);
			sendEvent(bits[0], bits[1], direction);

		} else {
			// Put error message here
		}

	}// end sendEvent

	/**
	 * Send the given event to the widget (either over network or locally)
	 * 
	 * @param location
	 * @param eventString
	 * @param direction
	 */
	private void sendEvent(String location, String eventString,
			Direction direction) {
		RgTools.createNotification(getApplicationContext(), "Sending Event",
				eventString, android.R.drawable.ic_menu_share);

		if (!location.equals("unknown")) {
			String[] locationSplit = location.split(",");
			int x = Integer.parseInt(locationSplit[0]);
			int y = Integer.parseInt(locationSplit[1]);

			EventCarrier eventCarrier = new EventCarrier(
					Event.valueOf(eventString), x, y, 0, direction);
			if (RgTools.wifiMode && socketHandler != null) {
				socketHandler.send(eventCarrier);
			} else {
				applyEventToCurrentThing(x + "," + y + "|" + eventString + "|"
						+ Direction.NULL);

			}
		} else {
			applyEventToCurrentThing(location + "|" + eventString + "|"
					+ Direction.NULL);
		}

	}// end sendEvent

	/** set up our menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = new MenuInflater(this);
		inflater.inflate(R.menu.main_menu, menu);
		return true;

	}// end onCreateOptionsMenu

	/** handle menu selection to turn the server on and off */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.qr_scanning:
			Log.d(RgTools.QR_SCANNER, "Initiating QR Scan");
			IntentIntegrator integrator = new IntentIntegrator(this);
			integrator.initiateScan();
			Log.d(RgTools.QR_SCANNER, " --------------- Got thing:  "
					+ integrator.getMessage());
			break;

		}// end switch

		return super.onMenuItemSelected(featureId, item);

	}// end onMenuItemSelected

	/** handle results from startActivityForResult() calls */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		/* Handle QR code stuff */
		case IntentIntegrator.REQUEST_CODE:
			IntentResult scanResult = IntentIntegrator.parseActivityResult(
					requestCode, resultCode, data);
			if (scanResult != null) {
				String ntf_title = "RG Location Set";
				String ntf_text = "Scan Position Returned: "
						+ scanResult.getContents();

				RgTools.createNotification(getApplicationContext(), ntf_title,
						ntf_text, android.R.drawable.ic_menu_mylocation);

				Log.d(RgTools.QR_SCANNER, "Scan Position Returned: "
						+ scanResult.getContents());
				currentPosition = scanResult.getContents();
				updateCurrentPositionView();
			}
			break;

		}// end switch

		super.onActivityResult(requestCode, resultCode, data);

	}// end onActivityResult

	/**
	 * Take the contents of the EventCarrier and perform the event against your
	 * state machine
	 * 
	 * @param eventCarrier
	 */
	public void sendToHandler(EventCarrier eventCarrier) {
		// We programmer ThingView to take string formatted as:
		// 0,1|Heat|UP (for example)
		// Might refactor in future, but for now just make the message the way
		// we like

		String msg = eventCarrier.getX() + "," + eventCarrier.getY() + "|"
				+ eventCarrier.getEvent().toString();

		if (eventCarrier.getDirection() != null) {
			msg += "|" + eventCarrier.getDirection().toString();

		} else {
			msg += "|NULL";
		}

		//We are on the wrong thead... see:
		// http://stackoverflow.com/questions/5097267/error-only-the-original-thread-that-created-a-view-hierarchy-can-touch-its-view
		Message uiMessage = new Message();
		uiMessage.obj = msg;
		messageHandler.sendMessage(uiMessage);

	}

	/**
	 * 
	 * @param socketHandler
	 */
	@Override
	public void addSocketHandler(SocketHandler socketHandler) {
		this.socketHandler = socketHandler;
	}

	/**
	 * 
	 * @param socketHandler
	 */
	@Override
	public void removeSocketHandler(SocketHandler socketHandler) {
		this.socketHandler = null;
	}

	/**
	 * 
	 * @param message
	 * @param t
	 */
	@Override
	public void report(String message, Throwable t) {
		Log.d("Rube1", message, t);
	}

	/**
	 * 
	 * @param line
	 */
	@Override
	public void report(String line) {
		Log.d("Rube2", line);
	}

}// end ActivityMain
