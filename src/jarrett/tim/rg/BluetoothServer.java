package jarrett.tim.rg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * Class for handling the Bluetooth communication. Used by both ActivityMain and
 * ActivityBluetooth.
 * 
 * @author tjarrett
 * 
 */
public class BluetoothServer
{
    
	// dialog constants for reporting bluetooth status
    public static final int DIALOG_NO_BLUETOOTH = 11;
    public static final int DIALOG_WE_HAVE_BLUETOOTH = 12;
    public static final int DIALOG_BLUETOOTH_ENABLED = 13;
    public static final int DIALOG_BLUETOOTH_ALREADY_ENABLED = 14;
    public static final int DIALOG_USER_IS_EVIL = 15;

    // constants used for startActivityForResult calls, so we know which call is
    // responding
    public static final int BLUETOOTH_ENABLED = 21;
    public static final int SELECTING_DEVICE = 42;

    // constant to represent a message being passed from the server thread back
    // to our GUI thread
    public static final int BLUETOOTH_MESSAGE = 98;

    // a UUID that represents this service
    // THIS MUST BE UNIQUE FOR THE SERVICE AND BOTH THE CLIENT AND SERVER NEED
    // TO KNOW ITS VALUE
    public static final UUID uuid = UUID.fromString("bb37d560-09b3-11e1-be50-0800200c9a66");

    /**
     * The actual BluetoothServer instance -- there can be only one
     */
    private static BluetoothServer bts;

    /**
     * The activity that this server is attached to -- we need this so that I
     * can pop-up messages and the such
     */
    private Activity activity;

    /**
     * Whether or not the server is running currently
     */
    private boolean isRunning = false;

    /**
     * Our bluetoothAdapter
     */
    private BluetoothAdapter bluetoothAdapter;

    /**
     * Handler for communicating with the GUI threads
     */
    private Handler handler;

    /**
     * List of BluetoothDevices that we are either bonded to our discovering
     */
    private List<BluetoothDeviceWrapper> wrappers = new ArrayList<BluetoothDeviceWrapper>();

    /**
     * The list adapter for displaying BluetoothDevices we want to connect
     */
    private BluetoothDeviceListAdapter bluetoothDeviceListAdapter = new BluetoothDeviceListAdapter();

    /**
     * The device we are connecting to
     */
    private BluetoothDevice selectedBluetoothServer;

    /**
     * The AcceptThread -- the ServerThread (basically) derived from Google's
     * bluetooth chat example
     * http://developer.android.com/resources/samples/BluetoothChat/index.html
     */
    private AcceptThread serverThread;

    /**
     * The client thread -- derived from Google's blueooth chat example
     * http://developer.android.com/resources/samples/BluetoothChat/index.html
     */
    private ConnectedThread connectedThread;

    /**
     * Listen for changes comming from the Bluetooth device
     * 
     * @todo Handle Discoverability Mode changes
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {

        /**
         * Receive update from BluetoothDevice
         */
        @Override
        public void onReceive(Context context, Intent intent)
        {

            if ( BluetoothDevice.ACTION_FOUND.equals(intent.getAction()) ) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if ( device != null ) {
                    bluetoothDeviceListAdapter.add(new BluetoothDeviceWrapper(device));

                }

            } else if ( BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction()) ) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                if ( selectedBluetoothServer == null ) {
                    setSelectedBluetoothDevice(device);
                    
                }
                
                Log.d(RgTools.BLUETOOTH_SERVER, "Connected to: " + device.getName());
                
            }

        }// end onReceive

    };

    /**
     * Can only be constructed from the factory() method
     * 
     * @param a
     */
    private BluetoothServer(Activity a, Handler handler)
    {
        activity = a;

        this.handler = handler;

    }// end BluetoothServer

    /**
     * Check that the bluetooth adapter is available and that it is active (if
     * not active, tries to activate it)
     */
    public void initBluetooth()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( bluetoothAdapter == null ) {
            activity.showDialog(DIALOG_NO_BLUETOOTH);
            return;
            
        }

        // Make sure bluetooth is enabled
        if ( !bluetoothAdapter.isEnabled() ) {
            try {
                activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH_ENABLED);
                
            } catch ( NullPointerException npe ) {
                activity.showDialog(DIALOG_NO_BLUETOOTH);
                return;
                
            }            

        }
        
        //Register the activities that we want to listen to on our newly enabled adapter
        activity.registerReceiver(bts.getBroadcastReceiver(), new IntentFilter(BluetoothDevice.ACTION_FOUND));
        activity.registerReceiver(bts.getBroadcastReceiver(), new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
        activity.registerReceiver(bts.getBroadcastReceiver(), new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

    }// end initBlueTooth

    /**
     * Get access to the adapter directly
     * 
     * @return
     */
    public BluetoothAdapter getBluetoothAdapter()
    {
        return bluetoothAdapter;

    }// end getBluetoothAdapter

    /**
     * Get the bluetooth device at position position in the list of bonded and discoverable devices
     * @param position
     */
    public void setSelectedBluetoothDevice(int position)
    {
        setSelectedBluetoothDevice(wrappers.get(position).getBluetoothDevice());

    }//end setSelectedBluetoothDevice
    
    /**
     * Set the selected remote device 
     * @param device
     */
    public void setSelectedBluetoothDevice(BluetoothDevice device)
    {
        for ( BluetoothDeviceWrapper wrapper : wrappers ) {
            if ( wrapper.getBluetoothDevice().getAddress().equals(device.getAddress()) ) {
                wrapper.setSelected(true);
                
            } else {
                wrapper.setSelected(false);
                
            }
            
        }
        
        //Announce changes
        bluetoothDeviceListAdapter.fireChange();
        
        //Keep off our client thread
        // - derived from http://developer.android.com/resources/samples/BluetoothChat/index.html
        Log.d(RgTools.CLIENT, "Wiring up thread to connected device: " + device.getName());
        selectedBluetoothServer = device;
        Thread connectThread = new ConnectThread(getSelectedBluetoothDevice());
        connectThread.start();
        
    }//end setSelectedBluetoothDevice

    /**
     * Return the currently selected bluetooth device
     * @return
     */
    public BluetoothDevice getSelectedBluetoothDevice()
    {
        return selectedBluetoothServer;

    }//end getSelectedBluetoothDevice

    /**
     * Returns true if the server is running, false otherwise
     * 
     * @return
     */
    public boolean isServerRunning()
    {
        return isRunning;

    }// end isServerRunning

    /**
     * Start the server (if it's not already running)
     */
    public void startServer()
    {
        if ( serverThread == null ) {
            serverThread = new AcceptThread();
            serverThread.start();
            isRunning = true;
        }

    }// end startServer

    /**
     * Stop the server
     */
    public void stopServer()
    {
        if ( serverThread != null ) {
            serverThread.cancel();
            serverThread = null;
            isRunning = false;

        }

        if ( connectedThread != null ) {
            connectedThread.cancel();
            connectedThread = null;

        }

    }// end stopServer

    /**
     * Kicks off discoverability mode for the default 120 seconds -- this is
     * only needed if we are trying to act as a server so if the server isn't
     * running it does nothing
     */
    public void startDiscoverabilityMode()
    {
        // Only if the server is running
        if ( isServerRunning() ) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            activity.startActivity(intent);

        }

    }// end startDiscoverabilityMode

    /**
     * Start device discovery
     */
    public void startDeviceDiscovery()
    {
        // Clear any devices already in the list
        wrappers.clear();

        // Reset the selected device
        selectedBluetoothServer = null;

        // Add in any devices already bonded
        for ( BluetoothDevice device : bluetoothAdapter.getBondedDevices() ) {
            bluetoothDeviceListAdapter.add(new BluetoothDeviceWrapper(device));

        }

        // Actually start the discovery
        bluetoothAdapter.startDiscovery();

    }// end startDeviceDiscovery

    /**
     * Stops device discovery
     */
    public void stopDeviceDiscovery()
    {
        bluetoothAdapter.cancelDiscovery();

    }// end stopDeviceDiscovery

    /**
     * Returns the list adapter for discovery devices
     * 
     * @return
     */
    public BluetoothDeviceListAdapter getBluetoothDeviceListAdapter()
    {
        return bluetoothDeviceListAdapter;

    }// end getBluetoothDeviceListAdapter

    /**
     * Return our pre-defined broadcast receiver
     * 
     * @return
     */
    public BroadcastReceiver getBroadcastReceiver()
    {
        return broadcastReceiver;

    }// end getBroadcastReceiver

    /**
     * Sends the given message to the device we are connected to
     * @param msg
     */
    public void send(String msg)
    {
        //Send the message to the thread
        if ( connectedThread != null ) {
            connectedThread.write(msg.getBytes());
        }
        
    }//end send

    /**
     * Take a socket, and manage it on another thread -- after this happens we don't 
     * have to worry about server/client. The socket will use the handler to receive messages 
     * and will use this send() to send messages
     * 
     * Derived from http://developer.android.com/resources/samples/BluetoothChat/index.html
     */
    private void manageConnectedSocket(BluetoothSocket socket)
    {
        Log.d(RgTools.BLUETOOTH_SERVER, "Managing connected socket");
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

    }//end manageConnectedSocket

    /**
     * Build us one and only one of these BluetoothServer objects
     * 
     * @param a
     * @return
     */
    static public BluetoothServer factory(Activity a, Handler handler)
    {
        if ( !isConfigured() ) {
            bts = new BluetoothServer(a, handler);
            return bts;

        } else {
            return getInstance();

        }

    }// end BluetoothServer

    /**
     * Gets the BluetoothServer object that has already been created. Returns
     * null if it hasn't been created yet
     * 
     * @return
     */
    static public BluetoothServer getInstance()
    {
        return bts;

    }// end getInstance

    /**
     * Returns true if we already have a BluetoothServer, false otherwise
     * 
     * @return
     */
    static public boolean isConfigured()
    {
        if ( bts == null ) {
            return false;

        } else {
            return true;

        }

    }// end isConfigured

    /**
     * A wrapper class that holds a device and its selection status for us to
     * display in a list
     * 
     * Derived from code presented in class
     */
    public static class BluetoothDeviceWrapper
    {
        // generate a unique id for each device (makes the listview happy)
        private static int nextId = 0;
        
        //Increment the id
        private int id = nextId++;
        
        //Track the bluetooth device
        private BluetoothDevice bluetoothDevice;
        
        //Whether or not this is selected
        private boolean selected;

        /**
         * Constructor
         * @param bluetoothDevice
         */
        public BluetoothDeviceWrapper(BluetoothDevice bluetoothDevice)
        {
            this.bluetoothDevice = bluetoothDevice;
            
        }//end BluetoothDeviceWrapper

        /**
         * Return the bluetooth device
         * @return
         */
        public BluetoothDevice getBluetoothDevice()
        {
            return bluetoothDevice;
            
        }//end getBluetoothDevice

        /**
         * Return the id
         * @return
         */
        public int getId()
        {
            return id;
            
        }//end getId

        /**
         * Whether or not this item is selected
         * @return
         */
        public boolean isSelected()
        {
            return selected;
        }

        /**
         * Set this item as selected 
         * @param selected
         */
        public void setSelected(boolean selected)
        {
            this.selected = selected;
            
        }//end setSelected
        
    }//end BluetoothDeviceWrapper

    /**
     * List Adapter for displaying bonded and discoverable devices 
     * @author tjarrett
     *
     */
    private class BluetoothDeviceListAdapter implements ListAdapter
    {
        // list of data set observers to notify when the list data changes
        private List<DataSetObserver> observers = new ArrayList<DataSetObserver>();

        /**
         * Return the number of devices in the lis
         */
        @Override
        public int getCount()
        {
            return wrappers.size();
            
        }//end getCount

        /**
         * Return the item at the given position
         */
        @Override
        public Object getItem(int position)
        {
            return wrappers.get(position);
            
        }//end getItem

        /** return the id of the wrapped bluetooth device for this position */
        @Override
        public long getItemId(int position)
        {
            return wrappers.get(position).getId();
            
        }//end getItemId

        /**
         * return which view we want to use to display the device - all list
         * entries use the same id
         */
        @Override
        public int getItemViewType(int position)
        {
            return 0;
            
        }//end getItemViewType

        /**
         * get (or fill in an existing) view to display the wrapper at the
         * specified position
         */
        @Override
        public View getView(int position, View view, ViewGroup group)
        {
            if ( view == null )
                view = LayoutInflater.from(activity).inflate(R.layout.device_item, null);
            BluetoothDeviceWrapper wrapper = wrappers.get(position);
            TextView name = (TextView)view.findViewById(R.id.name);
            name.setText(wrapper.getBluetoothDevice().getName());
            TextView mac = (TextView)view.findViewById(R.id.mac);
            mac.setText(wrapper.getBluetoothDevice().getAddress());
            TextView status = (TextView)view.findViewById(R.id.status);
            if ( wrapper.isSelected() ) {
                name.setBackgroundColor(Color.YELLOW);
                mac.setBackgroundColor(Color.YELLOW);
                status.setBackgroundColor(Color.YELLOW);
            } else {
                name.setBackgroundColor(Color.WHITE);
                mac.setBackgroundColor(Color.WHITE);
                status.setBackgroundColor(Color.WHITE);
            }
            switch ( wrapper.getBluetoothDevice().getBondState() ) {
                case BluetoothDevice.BOND_BONDED:
                    status.setText("BONDED");
                    break;
                case BluetoothDevice.BOND_BONDING:
                    status.setText("BONDING");
                    break;
                case BluetoothDevice.BOND_NONE:
                    status.setText("NOT BONDED");
                    break;
            }
            return view;
            
        }//end 

        /** we use the same view type for all entries in the list */
        @Override
        public int getViewTypeCount()
        {
            return 1;
        }

        /** all entries have consistent ids */
        @Override
        public boolean hasStableIds()
        {
            return true;
        }

        /** tell if the list is empty */
        @Override
        public boolean isEmpty()
        {
            return wrappers.isEmpty();
        }

        /** all items are always enabled */
        @Override
        public boolean areAllItemsEnabled()
        {
            return true;
        }

        /** all items are always enabled */
        @Override
        public boolean isEnabled(int position)
        {
            return true;
        }

        /** report that something in the list has changed so it will be redrawn */
        private void fireChange()
        {
            for ( DataSetObserver observer : observers ) {
                observer.onChanged();
            }
        }

        /** add an observer */
        @Override
        public void registerDataSetObserver(DataSetObserver observer)
        {
            observers.add(observer);
        }

        /** remove an observer */
        @Override
        public void unregisterDataSetObserver(DataSetObserver observer)
        {
            observers.remove(observer);
        }

        /** add a new wrapper to the list */
        public void add(BluetoothDeviceWrapper wrapper)
        {
            wrappers.add(wrapper);
            fireChange();
        }
    }// end BluetoothDeviceListAdapter

    /**
     * The server thread
     * Derived from http://developer.android.com/resources/samples/BluetoothChat/index.html
     * @author tjarrett
     *
     */
    private class AcceptThread extends Thread
    {
        /**
         * The socket
         */
        private final BluetoothServerSocket serverSocket;

        /**
         * Constructor
         */
        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Rube", BluetoothServer.uuid);

            } catch ( IOException ioe ) {
                ioe.printStackTrace();
                
            }

            serverSocket = tmp;

        }//end constructor

        /**
         * Kick off the thread
         */
        public void run()
        {
            BluetoothSocket socket = null;

            // Keep listening until exception occurs or a socket is returned
            while ( true ) {
                try {
                    socket = serverSocket.accept();

                } catch ( IOException ioe ) {
                    break;

                }

                // If a connection was accepted...
                if ( socket != null ) {
                    // Do work to manage the connection elsewhere
                    manageConnectedSocket(socket);
                    try {
                        serverSocket.close();

                    } catch ( IOException ioe ) {
                        break;

                    }

                    break;

                }

            }// end while

        }// end run

        /**
         * Cancel this thread
         */
        public void cancel()
        {
            try {
                serverSocket.close();

            } catch ( IOException ioe ) {
                ioe.printStackTrace();
                
            }

        }// end cancel

    }// end AcceptThread

    /**
     * Thread for managing AcceptThread and ConnectThread
     * 
     * Derived from http://developer.android.com/resources/samples/BluetoothChat/index.html
     * @author tjarrett
     *
     */
    private class ConnectedThread extends Thread
    {
        /**
         * The bluetooth socket
         */
        private final BluetoothSocket bluetoothSocket;
        
        /**
         * The input stream
         */
        private final InputStream inStream;
        
        /**
         * The output stream
         */
        private final OutputStream outStream;

        /**
         * Constructor
         * @param socket
         */
        public ConnectedThread(BluetoothSocket socket)
        {
            bluetoothSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                
            } catch ( IOException e ) {
                e.printStackTrace();
                
            }

            inStream = tmpIn;
            outStream = tmpOut;
            
        }//end ConnectedThread

        /**
         * Kick off the thread
         */
        public void run()
        {
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while ( true ) {
                try {
                    // Read from the InputStream
                    bytes = inStream.read(buffer);
                    
                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(BLUETOOTH_MESSAGE, bytes, -1, buffer).sendToTarget();
                    
                } catch ( IOException e ) {
                    break;
                    
                }
                
            }
            
        }//end run

        /**
         * Call this to send data to the remote device
         * @param bytes
         */
        public void write(byte[] bytes)
        {
            try {
                outStream.write(bytes);
                
            } catch ( IOException e ) {
                e.printStackTrace();
                
            }
            
        }//end write

        /**
         * Call this to shut down the connetion
         */
        public void cancel()
        {
            try {
                bluetoothSocket.close();
                
            } catch ( IOException e ) {
                e.printStackTrace();
                
            }
            
        }//end cancel
        
    }//end ConnectedThread

    /**
     * For connecting to a remote device
     * 
     * Derived from http://developer.android.com/resources/samples/BluetoothChat/index.html
     * @author tjarrett
     *
     */
    private class ConnectThread extends Thread
    {
        /**
         * The bluetooth socket
         */
        private final BluetoothSocket bluetoothSocket;
        
        /**
         * The bluetooth device
         */
        private final BluetoothDevice bluetoothDevice;

        /**
         * Constructor
         * @param device
         */
        public ConnectThread(BluetoothDevice device)
        {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            bluetoothDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server
                // code
                tmp = device.createRfcommSocketToServiceRecord(BluetoothServer.uuid);
                
            } catch ( IOException e ) {
                Log.d(RgTools.CLIENT, "Could not open RF comm socket");
                e.printStackTrace();

            }
            
            bluetoothSocket = tmp;
            
        }//end ConnectThread

        /**
         * Kick off the thread
         */
        public void run()
        {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            Log.d(RgTools.CLIENT, "Running client thread...");

            try {
                // Do work to manage the connection (in a separate thread)
                Log.d(RgTools.CLIENT, "Managing socket with device: " + bluetoothDevice.getName());
                
                RgTools.createNotification(activity.getApplicationContext(), "Device Connected", bluetoothDevice.getName(), android.R.drawable.ic_menu_info_details);
                
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                bluetoothSocket.connect();

            } catch ( IOException connectException ) {
                // Unable to connect; close the socket and get out
                try {
                    bluetoothSocket.close();
                } catch ( IOException closeException ) {
                    Log.d(RgTools.CLIENT, "Connection closed");

                }

                Log.d(RgTools.CLIENT, "Returning from ConnectThread");
                return;
            }

            manageConnectedSocket(bluetoothSocket);

        }//end run

        /**
         * Cancel this connection
         */
        public void cancel()
        {
            try {
                bluetoothSocket.close();
                
            } catch ( IOException e ) {
                e.printStackTrace();
                
            }
            
        }//end cancel
        
    }//end ConnectThread

}// end BluetoothServer
